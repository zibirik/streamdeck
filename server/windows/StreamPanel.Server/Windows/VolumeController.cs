using System.Runtime.InteropServices;

namespace StreamPanel.Server.Windows;

public interface IVolumeController
{
    int GetVolumePercent();
    bool GetMuted();
    bool GetMicMuted();
    void SetVolumePercent(int percent);
    void VolumeUp(int steps = 2);
    void VolumeDown(int steps = 2);
    void Mute();
    void ToggleMicMute();
}

public sealed class VolumeController : IVolumeController
{
    public int GetVolumePercent()
    {
        var endpoint = GetDefaultEndpoint();
        endpoint.GetMasterVolumeLevelScalar(out var level);
        Marshal.ReleaseComObject(endpoint);
        return (int)Math.Round(Math.Clamp(level, 0f, 1f) * 100);
    }

    public bool GetMuted()
    {
        var endpoint = GetDefaultEndpoint();
        endpoint.GetMute(out var muted);
        Marshal.ReleaseComObject(endpoint);
        return muted;
    }

    public bool GetMicMuted()
    {
        var endpoint = GetDefaultCaptureEndpoint();
        endpoint.GetMute(out var muted);
        Marshal.ReleaseComObject(endpoint);
        return muted;
    }

    public void SetVolumePercent(int percent)
    {
        var volume = Math.Clamp(percent, 0, 100) / 100f;
        var endpoint = GetDefaultEndpoint();
        endpoint.SetMasterVolumeLevelScalar(volume, Guid.Empty);
        Marshal.ReleaseComObject(endpoint);
    }

    public void VolumeUp(int steps = 2)
    {
        for (var i = 0; i < steps; i++)
        {
            SendVirtualKey(0xAF, keyUp: false);
            SendVirtualKey(0xAF, keyUp: true);
        }
    }

    public void VolumeDown(int steps = 2)
    {
        for (var i = 0; i < steps; i++)
        {
            SendVirtualKey(0xAE, keyUp: false);
            SendVirtualKey(0xAE, keyUp: true);
        }
    }

    public void Mute()
    {
        SendVirtualKey(0xAD, keyUp: false);
        SendVirtualKey(0xAD, keyUp: true);
    }

    public void ToggleMicMute()
    {
        var endpoint = GetDefaultCaptureEndpoint();
        endpoint.GetMute(out var muted);
        endpoint.SetMute(!muted, Guid.Empty);
        Marshal.ReleaseComObject(endpoint);
    }

    private static IAudioEndpointVolume GetDefaultEndpoint()
    {
        var deviceEnumerator = (IMMDeviceEnumerator)new MMDeviceEnumerator();
        deviceEnumerator.GetDefaultAudioEndpoint(EDataFlow.eRender, ERole.eMultimedia, out var device);
        device.Activate(typeof(IAudioEndpointVolume).GUID, 0x23, IntPtr.Zero, out var obj);
        return (IAudioEndpointVolume)obj;
    }

    private static IAudioEndpointVolume GetDefaultCaptureEndpoint()
    {
        var deviceEnumerator = (IMMDeviceEnumerator)new MMDeviceEnumerator();
        deviceEnumerator.GetDefaultAudioEndpoint(EDataFlow.eCapture, ERole.eCommunications, out var device);
        device.Activate(typeof(IAudioEndpointVolume).GUID, 0x23, IntPtr.Zero, out var obj);
        return (IAudioEndpointVolume)obj;
    }

    private static void SendVirtualKey(ushort virtualKey, bool keyUp)
    {
        var input = new INPUT
        {
            type = 1,
            U = new InputUnion
            {
                ki = new KEYBDINPUT
                {
                    wVk = virtualKey,
                    dwFlags = keyUp ? 0x0002u : 0u
                }
            }
        };
        SendInput(1, [input], Marshal.SizeOf<INPUT>());
    }

    [DllImport("user32.dll")]
    private static extern uint SendInput(uint nInputs, INPUT[] pInputs, int cbSize);

    [StructLayout(LayoutKind.Sequential)]
    private struct INPUT
    {
        public int type;
        public InputUnion U;
    }

    [StructLayout(LayoutKind.Explicit)]
    private struct InputUnion
    {
        [FieldOffset(0)] public KEYBDINPUT ki;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct KEYBDINPUT
    {
        public ushort wVk;
        public ushort wScan;
        public uint dwFlags;
        public uint time;
        public IntPtr dwExtraInfo;
    }

    private enum EDataFlow
    {
        eRender,
        eCapture,
        eAll
    }

    private enum ERole
    {
        eConsole,
        eMultimedia,
        eCommunications
    }

    [ComImport, Guid("BCDE0395-E52F-467C-8E3D-C4579291692E")]
    private class MMDeviceEnumerator;

    [ComImport, Guid("A95664D2-9614-4F35-A746-DE8DB63617E6"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface IMMDeviceEnumerator
    {
        int NotImpl1();
        [PreserveSig] int GetDefaultAudioEndpoint(EDataFlow dataFlow, ERole role, out IMMDevice ppDevice);
    }

    [ComImport, Guid("D666063F-1587-4E43-81F1-B948E807363F"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface IMMDevice
    {
        int Activate([MarshalAs(UnmanagedType.LPStruct)] Guid iid, int dwClsCtx, IntPtr pActivationParams, [MarshalAs(UnmanagedType.IUnknown)] out object ppInterface);
    }

    [ComImport, Guid("5CDF2C82-841E-4546-9722-0CF74078229A"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface IAudioEndpointVolume
    {
        int RegisterControlChangeNotify(IntPtr pNotify);
        int UnregisterControlChangeNotify(IntPtr pNotify);
        int GetChannelCount(out uint pnChannelCount);
        int SetMasterVolumeLevel(float fLevelDB, Guid pguidEventContext);
        int SetMasterVolumeLevelScalar(float fLevel, Guid pguidEventContext);
        int GetMasterVolumeLevel(out float pfLevelDB);
        int GetMasterVolumeLevelScalar(out float pfLevel);
        int SetChannelVolumeLevel(uint nChannel, float fLevelDB, Guid pguidEventContext);
        int SetChannelVolumeLevelScalar(uint nChannel, float fLevel, Guid pguidEventContext);
        int GetChannelVolumeLevel(uint nChannel, out float pfLevelDB);
        int GetChannelVolumeLevelScalar(uint nChannel, out float pfLevel);
        int SetMute([MarshalAs(UnmanagedType.Bool)] bool bMute, Guid pguidEventContext);
        int GetMute(out bool pbMute);
    }
}
