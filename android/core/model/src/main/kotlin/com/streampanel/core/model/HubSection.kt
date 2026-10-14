package com.streampanel.core.model

/**
 * The tool panels are grouped into a few topics, so each topic gets its own screen
 * instead of one endless column of everything. The order of [panels] is only the default;
 * the user's own order in [DashboardLayoutSettings.panelSlots] always wins.
 */
enum class HubSection(val panels: List<DashboardPanelId>) {
    /** Daily remote control: sound, shortcuts, windows, launching things. */
    Control(
        listOf(
            DashboardPanelId.Media,
            DashboardPanelId.QuickActions,
            DashboardPanelId.SystemTools,
            DashboardPanelId.QuickLaunch,
            DashboardPanelId.NavigationShortcuts,
            DashboardPanelId.Clipboard,
        ),
    ),

    /** Everything about going live. */
    Stream(
        listOf(
            DashboardPanelId.StreamTools,
            DashboardPanelId.StreamChat,
            DashboardPanelId.Discord,
            DashboardPanelId.GameStatus,
        ),
    ),

    /** How the computer is doing. */
    Pc(
        listOf(
            DashboardPanelId.HardwareMonitor,
            DashboardPanelId.ProcessMonitor,
            DashboardPanelId.PcConfigurator,
        ),
    ),

    /** Focus, time and developer helpers. */
    Work(
        listOf(
            DashboardPanelId.Pomodoro,
            DashboardPanelId.TimeTracker,
            DashboardPanelId.StudyMode,
            DashboardPanelId.MeetingMode,
            DashboardPanelId.DevTools,
        ),
    ),
    ;

    companion object {
        /** The section a panel lives in (every panel belongs to exactly one). */
        fun of(panel: DashboardPanelId): HubSection = entries.first { panel in it.panels }
    }
}

/** The panels of [section] that the user left visible, in the user's order. */
fun DashboardLayoutSettings.visiblePanels(section: HubSection): List<DashboardPanelId> =
    orderedVisiblePanels.filter { it in section.panels }
