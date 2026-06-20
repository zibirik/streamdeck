using System.Runtime.InteropServices;

namespace StreamPanel.Server.Windows;

public interface IAudioOutputSwitcher
{
    void SwitchTo(string target);
}

public sealed class AudioOutputSwitcher : IAudioOutputSwitcher
{
    public void SwitchTo(string target)
    {
        var normalized = target.Trim().ToLowerInvariant();
        var deviceId = FindDeviceId(normalized)
            ?? throw new InvalidOperationException($"No audio output device matched '{target}'.");

        var policy = (IPolicyConfig)new PolicyConfigClient();
        policy.SetDefaultEndpoint(deviceId, ERole.eMultimedia);
        policy.SetDefaultEndpoint(deviceId, ERole.eConsole);
        Marshal.ReleaseComObject(policy);
    }

    private static string? FindDeviceId(string target)
    {
        var enumerator = (IMMDeviceEnumerator)new MMDeviceEnumerator();
        enumerator.EnumAudioEndpoints(EDataFlow.eRender, 0x1, out var collection);
        collection.GetCount(out var count);

        string? fallback = null;
        for (uint i = 0; i < count; i++)
        {
            collection.Item(i, out var device);
            device.GetId(out var id);
            device.OpenPropertyStore(0, out var store);
            var friendlyNameKey = PKEY_Device_FriendlyName;
            store.GetValue(ref friendlyNameKey, out var value);
            var name = value.GetValue()?.ToString() ?? string.Empty;
            Marshal.ReleaseComObject(store);
            Marshal.ReleaseComObject(device);

            var lower = name.ToLowerInvariant();
            var matchesHeadphones = lower.Contains("headphone") || lower.Contains("headset") || lower.Contains("earphone")
                || lower.Contains("наушник");
            var matchesSpeakers = lower.Contains("speaker") || lower.Contains("динамик") || lower.Contains("realtek")
                || lower.Contains("display") || lower.Contains("monitor");

            if (target is "headphones" or "headphone" or "headset")
            {
                if (matchesHeadphones) return id;
            }
            else if (target is "speakers" or "speaker")
            {
                if (matchesSpeakers && !matchesHeadphones) return id;
                if (matchesSpeakers) fallback = id;
            }
        }

        Marshal.ReleaseComObject(collection);
        Marshal.ReleaseComObject(enumerator);
        return fallback;
    }

    private static readonly PropertyKey PKEY_Device_FriendlyName = new(
        new Guid(0xa45c254e, 0xdf1c, 0x4efd, 0x80, 0x20, 0x67, 0xd1, 0x46, 0xa8, 0x50, 0xe0), 14);

    private enum EDataFlow { eRender, eCapture, eAll }
    private enum ERole { eConsole, eMultimedia, eCommunications }

    [ComImport, Guid("BCDE0395-E52F-467C-8E3D-C4579291692E")]
    private class MMDeviceEnumerator;

    [ComImport, Guid("A95664D2-9614-4F35-A746-DE8DB63617E6"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface IMMDeviceEnumerator
    {
        int NotImpl1();
        [PreserveSig] int GetDefaultAudioEndpoint(EDataFlow dataFlow, ERole role, out IMMDevice ppDevice);
        [PreserveSig] int EnumAudioEndpoints(EDataFlow dataFlow, int dwStateMask, out IMMDeviceCollection ppDevices);
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

    [ComImport, Guid("f8679f50-850a-41cf-9c7f-5e7c5c8bbd0e"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface IPolicyConfig
    {
        [PreserveSig] int NotImpl0();
        [PreserveSig] int NotImpl1();
        [PreserveSig] int NotImpl2();
        [PreserveSig] int NotImpl3();
        [PreserveSig] int NotImpl4();
        [PreserveSig] int NotImpl5();
        [PreserveSig] int NotImpl6();
        [PreserveSig] int NotImpl7();
        [PreserveSig] int NotImpl8();
        [PreserveSig] int NotImpl9();
        [PreserveSig] int NotImpl10();
        [PreserveSig] int NotImpl11();
        [PreserveSig] int NotImpl12();
        [PreserveSig] int SetDefaultEndpoint([MarshalAs(UnmanagedType.LPWStr)] string deviceId, ERole role);
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

    [StructLayout(LayoutKind.Explicit)]
    private struct PropVariant
    {
        [FieldOffset(0)] private ushort vt;
        [FieldOffset(8)] private IntPtr pointer;

        public object? GetValue() => vt == 31 ? Marshal.PtrToStringUni(pointer) : null;
    }
}
