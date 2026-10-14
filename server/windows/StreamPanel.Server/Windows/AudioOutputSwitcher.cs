using System.Runtime.InteropServices;

namespace StreamPanel.Server.Windows;

public sealed record AudioOutputDevice(string Id, string Name, bool IsDefault);

public interface IAudioOutputSwitcher
{
    /// <summary>The sound outputs that are plugged in and enabled right now.</summary>
    IReadOnlyList<AudioOutputDevice> ListOutputs();

    /// <summary>Makes <paramref name="target"/> the default output: "speakers", "headphones" or part of a device's name.</summary>
    void SwitchTo(string target);
}

public sealed class AudioOutputSwitcher : IAudioOutputSwitcher
{
    private static readonly string[] HeadphoneWords = ["headphone", "headset", "earphone", "earbud", "airpods", "наушник", "гарнитур"];
    private static readonly string[] SpeakerWords = ["speaker", "динамик", "колонк"];

    public IReadOnlyList<AudioOutputDevice> ListOutputs()
    {
        var devices = new List<AudioOutputDevice>();
        var enumerator = (IMMDeviceEnumerator)new MMDeviceEnumerator();
        try
        {
            var defaultId = DefaultOutputId(enumerator);
            Marshal.ThrowExceptionForHR(enumerator.EnumAudioEndpoints(EDataFlow.eRender, DeviceStateActive, out var collection));
            try
            {
                Marshal.ThrowExceptionForHR(collection.GetCount(out var count));
                for (uint i = 0; i < count; i++)
                {
                    Marshal.ThrowExceptionForHR(collection.Item(i, out var device));
                    try
                    {
                        Marshal.ThrowExceptionForHR(device.GetId(out var id));
                        devices.Add(new AudioOutputDevice(id, FriendlyName(device), string.Equals(id, defaultId, StringComparison.Ordinal)));
                    }
                    finally
                    {
                        Marshal.ReleaseComObject(device);
                    }
                }
            }
            finally
            {
                Marshal.ReleaseComObject(collection);
            }
        }
        finally
        {
            Marshal.ReleaseComObject(enumerator);
        }

        return devices;
    }

    public void SwitchTo(string target)
    {
        var devices = ListOutputs();
        var chosen = Choose(devices, target)
            ?? throw new InvalidOperationException(
                devices.Count == 0
                    ? "No sound output devices are available."
                    : $"No sound output matched '{target}'. Available: {string.Join(", ", devices.Select(d => d.Name))}.");

        var policy = (IPolicyConfig)new PolicyConfigClient();
        try
        {
            // Windows keeps a separate default for music/games, for system sounds and for calls: set them all.
            foreach (var role in new[] { ERole.eConsole, ERole.eMultimedia, ERole.eCommunications })
            {
                Marshal.ThrowExceptionForHR(policy.SetDefaultEndpoint(chosen.Id, role));
            }
        }
        finally
        {
            Marshal.ReleaseComObject(policy);
        }
    }

    /// <summary>Picks the device for a word like "headphones" or a piece of a device name. Kept free of COM so it can be tested.</summary>
    public static AudioOutputDevice? Choose(IReadOnlyList<AudioOutputDevice> devices, string target)
    {
        var wanted = target.Trim().ToLowerInvariant();
        if (wanted.Length == 0)
        {
            return null;
        }

        if (wanted is "headphones" or "headphone" or "headset" or "наушники")
        {
            return devices.FirstOrDefault(d => Matches(d.Name, HeadphoneWords));
        }

        if (wanted is "speakers" or "speaker" or "колонки" or "динамики")
        {
            return devices.FirstOrDefault(d => Matches(d.Name, SpeakerWords) && !Matches(d.Name, HeadphoneWords))
                ?? devices.FirstOrDefault(d => !Matches(d.Name, HeadphoneWords));
        }

        return devices.FirstOrDefault(d => d.Name.Contains(target.Trim(), StringComparison.OrdinalIgnoreCase));
    }

    private static bool Matches(string name, string[] words) =>
        words.Any(word => name.Contains(word, StringComparison.OrdinalIgnoreCase));

    private static string? DefaultOutputId(IMMDeviceEnumerator enumerator)
    {
        if (enumerator.GetDefaultAudioEndpoint(EDataFlow.eRender, ERole.eMultimedia, out var device) != 0)
        {
            return null;
        }

        try
        {
            return device.GetId(out var id) == 0 ? id : null;
        }
        finally
        {
            Marshal.ReleaseComObject(device);
        }
    }

    private static string FriendlyName(IMMDevice device)
    {
        Marshal.ThrowExceptionForHR(device.OpenPropertyStore(StgmRead, out var store));
        try
        {
            var key = PkeyDeviceFriendlyName;
            Marshal.ThrowExceptionForHR(store.GetValue(ref key, out var value));
            try
            {
                return value.AsString() ?? string.Empty;
            }
            finally
            {
                PropVariantClear(ref value);
            }
        }
        finally
        {
            Marshal.ReleaseComObject(store);
        }
    }

    private const int DeviceStateActive = 0x1;
    private const int StgmRead = 0;

    private static readonly PropertyKey PkeyDeviceFriendlyName = new(
        new Guid(0xa45c254e, 0xdf1c, 0x4efd, 0x80, 0x20, 0x67, 0xd1, 0x46, 0xa8, 0x50, 0xe0), 14);

    [DllImport("ole32.dll")]
    private static extern int PropVariantClear(ref PropVariant pvar);

    private enum EDataFlow { eRender, eCapture, eAll }

    private enum ERole { eConsole, eMultimedia, eCommunications }

    [ComImport, Guid("BCDE0395-E52F-467C-8E3D-C4579291692E")]
    private class MMDeviceEnumerator;

    // The order of the methods IS the contract with Windows (it calls them by position), so it must match exactly.
    [ComImport, Guid("A95664D2-9614-4F35-A746-DE8DB63617E6"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface IMMDeviceEnumerator
    {
        [PreserveSig] int EnumAudioEndpoints(EDataFlow dataFlow, int dwStateMask, out IMMDeviceCollection ppDevices);
        [PreserveSig] int GetDefaultAudioEndpoint(EDataFlow dataFlow, ERole role, out IMMDevice ppEndpoint);
    }

    [ComImport, Guid("0BD7A1BE-7A1A-44DB-8397-CC5392387B5E"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface IMMDeviceCollection
    {
        [PreserveSig] int GetCount(out uint pcDevices);
        [PreserveSig] int Item(uint nDevice, out IMMDevice ppDevice);
    }

    [ComImport, Guid("D666063F-1587-4E43-81F1-B948E807363F"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface IMMDevice
    {
        [PreserveSig] int Activate([MarshalAs(UnmanagedType.LPStruct)] Guid iid, int dwClsCtx, IntPtr pActivationParams, [MarshalAs(UnmanagedType.IUnknown)] out object ppInterface);
        [PreserveSig] int OpenPropertyStore(int stgmAccess, out IPropertyStore ppProperties);
        [PreserveSig] int GetId([MarshalAs(UnmanagedType.LPWStr)] out string ppstrId);
    }

    [ComImport, Guid("886D8EEB-8CF2-4446-8D02-CDBA1DBDCF99"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface IPropertyStore
    {
        [PreserveSig] int GetCount(out uint cProps);
        [PreserveSig] int GetAt(uint iProp, out PropertyKey pkey);
        [PreserveSig] int GetValue(ref PropertyKey key, out PropVariant pv);
        [PreserveSig] int SetValue(ref PropertyKey key, ref PropVariant propvar);
        [PreserveSig] int Commit();
    }

    [ComImport, Guid("870af99c-171d-4f9e-af0d-e63df40c2bc9")]
    private class PolicyConfigClient;

    /// <summary>Undocumented but stable since Windows 7; the same interface every audio switcher uses.</summary>
    [ComImport, Guid("F8679F50-850A-41CF-9C72-430F290290C8"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface IPolicyConfig
    {
        // The ten methods before SetDefaultEndpoint are not needed, but they take up their places.
        [PreserveSig] int GetMixFormat(IntPtr deviceName, IntPtr format);
        [PreserveSig] int GetDeviceFormat(IntPtr deviceName, int isDefault, IntPtr format);
        [PreserveSig] int ResetDeviceFormat(IntPtr deviceName);
        [PreserveSig] int SetDeviceFormat(IntPtr deviceName, IntPtr endpointFormat, IntPtr mixFormat);
        [PreserveSig] int GetProcessingPeriod(IntPtr deviceName, int isDefault, IntPtr defaultPeriod, IntPtr minimumPeriod);
        [PreserveSig] int SetProcessingPeriod(IntPtr deviceName, IntPtr period);
        [PreserveSig] int GetShareMode(IntPtr deviceName, IntPtr mode);
        [PreserveSig] int SetShareMode(IntPtr deviceName, IntPtr mode);
        [PreserveSig] int GetPropertyValue(IntPtr deviceName, int fx, IntPtr key, IntPtr value);
        [PreserveSig] int SetPropertyValue(IntPtr deviceName, int fx, IntPtr key, IntPtr value);
        [PreserveSig] int SetDefaultEndpoint([MarshalAs(UnmanagedType.LPWStr)] string deviceId, ERole role);
        [PreserveSig] int SetEndpointVisibility([MarshalAs(UnmanagedType.LPWStr)] string deviceId, int visible);
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct PropertyKey
    {
        public Guid fmtid;
        public uint pid;

        public PropertyKey(Guid fmtid, uint pid)
        {
            this.fmtid = fmtid;
            this.pid = pid;
        }
    }

    // A PROPVARIANT is 24 bytes on 64-bit Windows; Windows writes all of them, so the struct must have room for them.
    [StructLayout(LayoutKind.Explicit, Size = 24)]
    private struct PropVariant
    {
        [FieldOffset(0)] private ushort vt;
        [FieldOffset(8)] private IntPtr pointer;

        /// <summary>VT_LPWSTR (31) is a plain text pointer; the friendly name comes in that form.</summary>
        public readonly string? AsString() => vt == 31 ? Marshal.PtrToStringUni(pointer) : null;
    }
}
