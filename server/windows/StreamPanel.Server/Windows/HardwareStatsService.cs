using System.Diagnostics;
using System.Runtime.InteropServices;

namespace StreamPanel.Server.Windows;

public interface IHardwareStatsService
{
    double? CpuPercent { get; }
    double? RamPercent { get; }
    void Refresh();
}

public sealed class HardwareStatsService : IHardwareStatsService, IDisposable
{
    private readonly PerformanceCounter? _cpuCounter;
    private double? _cpuPercent;
    private double? _ramPercent;

    public HardwareStatsService()
    {
        try
        {
            _cpuCounter = new PerformanceCounter("Processor", "% Processor Time", "_Total");
            _cpuCounter.NextValue();
        }
        catch
        {
            _cpuCounter = null;
        }
    }

    public double? CpuPercent => _cpuPercent;
    public double? RamPercent => _ramPercent;

    public void Refresh()
    {
        try
        {
            if (_cpuCounter != null)
            {
                _cpuPercent = Math.Round(_cpuCounter.NextValue(), 1);
            }
        }
        catch
        {
            _cpuPercent = null;
        }

        try
        {
            var memStatus = new MEMORYSTATUSEX();
            if (GlobalMemoryStatusEx(memStatus))
            {
                _ramPercent = Math.Round((double)memStatus.dwMemoryLoad, 1);
            }
        }
        catch
        {
            _ramPercent = null;
        }
    }

    public void Dispose() => _cpuCounter?.Dispose();

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Auto)]
    private sealed class MEMORYSTATUSEX
    {
        public uint dwLength = (uint)Marshal.SizeOf<MEMORYSTATUSEX>();
        public uint dwMemoryLoad;
        public ulong ullTotalPhys;
        public ulong ullAvailPhys;
        public ulong ullTotalPageFile;
        public ulong ullAvailPageFile;
        public ulong ullTotalVirtual;
        public ulong ullAvailVirtual;
        public ulong ullAvailExtendedVirtual;
    }

    [DllImport("kernel32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool GlobalMemoryStatusEx([In, Out] MEMORYSTATUSEX lpBuffer);
}
