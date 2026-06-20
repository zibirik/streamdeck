package com.streampanel.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class DashboardZone {
    Sidebar,
    Deck,
    ToolsColumn,
}

@Serializable
enum class DashboardPanelId {
    HardwareMonitor,
    ProcessMonitor,
    DevTools,
    StreamTools,
    StreamChat,
    GameStatus,
    PcConfigurator,
    Discord,
    StudyMode,
    MeetingMode,
    Pomodoro,
    TimeTracker,
    Clipboard,
    QuickActions,
    Media,
    SystemTools,
    NavigationShortcuts,
    QuickLaunch,
}

@Serializable
enum class ToolsColumnWidth {
    Narrow,
    Medium,
    Wide,
}

@Serializable
data class DashboardPanelSlot(
    val id: DashboardPanelId,
    val visible: Boolean = true,
)

@Serializable
data class DashboardLayoutSettings(
    val zoneOrder: List<DashboardZone> = defaultZoneOrder(),
    val panelSlots: List<DashboardPanelSlot> = defaultPanelSlots(),
    val sidebarVisible: Boolean = true,
    val toolsColumnWidth: ToolsColumnWidth = ToolsColumnWidth.Medium,
    val compactTabOrder: List<DashboardZone> = listOf(DashboardZone.Deck, DashboardZone.ToolsColumn, DashboardZone.Sidebar),
) {
    val orderedVisiblePanels: List<DashboardPanelId>
        get() = panelSlots.filter { it.visible }.map { it.id }

    fun withPanelMoved(id: DashboardPanelId, direction: Int): DashboardLayoutSettings {
        val slots = panelSlots.toMutableList()
        val index = slots.indexOfFirst { it.id == id }
        if (index < 0) return this
        val target = (index + direction).coerceIn(0, slots.lastIndex)
        if (target == index) return this
        val item = slots.removeAt(index)
        slots.add(target, item)
        return copy(panelSlots = slots)
    }

    fun withPanelMovedTo(id: DashboardPanelId, targetIndex: Int): DashboardLayoutSettings {
        val slots = panelSlots.toMutableList()
        val index = slots.indexOfFirst { it.id == id }
        if (index < 0) return this
        val item = slots.removeAt(index)
        slots.add(targetIndex.coerceIn(0, slots.size), item)
        return copy(panelSlots = slots)
    }

    fun withPanelVisibility(id: DashboardPanelId, visible: Boolean): DashboardLayoutSettings {
        return copy(
            panelSlots = panelSlots.map { slot ->
                if (slot.id == id) slot.copy(visible = visible) else slot
            },
        )
    }

    fun withAllPanelsVisible(visible: Boolean): DashboardLayoutSettings {
        return copy(panelSlots = panelSlots.map { it.copy(visible = visible) })
    }

    fun withCompactTabMoved(zone: DashboardZone, direction: Int): DashboardLayoutSettings {
        val tabs = compactTabOrder.toMutableList()
        val index = tabs.indexOf(zone)
        if (index < 0) return this
        val target = (index + direction).coerceIn(0, tabs.lastIndex)
        if (target == index) return this
        val item = tabs.removeAt(index)
        tabs.add(target, item)
        return copy(compactTabOrder = tabs)
    }

    fun withZoneMoved(zone: DashboardZone, direction: Int): DashboardLayoutSettings {
        val zones = zoneOrder.toMutableList()
        val index = zones.indexOf(zone)
        if (index < 0) return this
        val target = (index + direction).coerceIn(0, zones.lastIndex)
        if (target == index) return this
        val item = zones.removeAt(index)
        zones.add(target, item)
        return copy(zoneOrder = zones)
    }

    companion object {
        fun defaultZoneOrder(): List<DashboardZone> = listOf(
            DashboardZone.Sidebar,
            DashboardZone.Deck,
            DashboardZone.ToolsColumn,
        )

        fun defaultPanelSlots(): List<DashboardPanelSlot> = listOf(
            DashboardPanelSlot(DashboardPanelId.HardwareMonitor),
            DashboardPanelSlot(DashboardPanelId.ProcessMonitor),
            DashboardPanelSlot(DashboardPanelId.DevTools),
            DashboardPanelSlot(DashboardPanelId.StreamTools),
            DashboardPanelSlot(DashboardPanelId.StreamChat),
            DashboardPanelSlot(DashboardPanelId.GameStatus),
            DashboardPanelSlot(DashboardPanelId.PcConfigurator),
            DashboardPanelSlot(DashboardPanelId.Discord),
            DashboardPanelSlot(DashboardPanelId.StudyMode),
            DashboardPanelSlot(DashboardPanelId.MeetingMode),
            DashboardPanelSlot(DashboardPanelId.Pomodoro),
            DashboardPanelSlot(DashboardPanelId.TimeTracker),
            DashboardPanelSlot(DashboardPanelId.Clipboard),
            DashboardPanelSlot(DashboardPanelId.QuickActions),
            DashboardPanelSlot(DashboardPanelId.Media),
            DashboardPanelSlot(DashboardPanelId.SystemTools),
            DashboardPanelSlot(DashboardPanelId.NavigationShortcuts),
            DashboardPanelSlot(DashboardPanelId.QuickLaunch),
        )

        fun mergeWithDefaults(raw: DashboardLayoutSettings?): DashboardLayoutSettings {
            if (raw == null) return DashboardLayoutSettings()
            val knownIds = DashboardPanelId.entries.toSet()
            val mergedPanels = mutableListOf<DashboardPanelSlot>()
            val seen = mutableSetOf<DashboardPanelId>()
            raw.panelSlots.filter { it.id in knownIds }.forEach { slot ->
                mergedPanels += slot
                seen += slot.id
            }
            defaultPanelSlots().forEach { slot ->
                if (slot.id !in seen) mergedPanels += slot
            }
            val mergedZones = raw.zoneOrder.filter { it in DashboardZone.entries }.distinct()
            val zoneOrder = if (mergedZones.size == DashboardZone.entries.size) mergedZones else defaultZoneOrder()
            val mergedTabs = raw.compactTabOrder.filter { it in DashboardZone.entries }.distinct()
            val compactTabOrder = if (mergedTabs.size == DashboardZone.entries.size) mergedTabs else listOf(
                DashboardZone.Deck,
                DashboardZone.ToolsColumn,
                DashboardZone.Sidebar,
            )
            return raw.copy(
                zoneOrder = zoneOrder,
                panelSlots = mergedPanels,
                compactTabOrder = compactTabOrder,
            )
        }
    }
}
