package com.streampanel.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streampanel.core.datastore.PreferencesDataSource
import com.streampanel.core.model.DashboardLayoutSettings
import com.streampanel.core.model.DashboardPanelId
import com.streampanel.core.model.DashboardZone
import com.streampanel.core.model.ToolsColumnWidth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LayoutCustomizerViewModel @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource,
) : ViewModel() {
    val layout = preferencesDataSource.dashboardLayout.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DashboardLayoutSettings(),
    )

    private fun update(transform: (DashboardLayoutSettings) -> DashboardLayoutSettings) {
        viewModelScope.launch {
            preferencesDataSource.setDashboardLayout(transform(layout.value))
        }
    }

    fun moveCompactTab(zone: DashboardZone, direction: Int) = update { it.withCompactTabMoved(zone, direction) }

    fun moveZone(zone: DashboardZone, direction: Int) = update { it.withZoneMoved(zone, direction) }

    fun movePanel(id: DashboardPanelId, direction: Int) = update { it.withPanelMoved(id, direction) }

    fun movePanelToTop(id: DashboardPanelId) = update { it.withPanelMovedTo(id, 0) }

    fun movePanelToBottom(id: DashboardPanelId) = update { it.withPanelMovedTo(id, it.panelSlots.lastIndex) }

    fun setPanelVisible(id: DashboardPanelId, visible: Boolean) = update { it.withPanelVisibility(id, visible) }

    fun setAllPanelsVisible(visible: Boolean) = update { it.withAllPanelsVisible(visible) }

    fun setSidebarVisible(visible: Boolean) = update { it.copy(sidebarVisible = visible) }

    fun setToolsColumnWidth(width: ToolsColumnWidth) = update { it.copy(toolsColumnWidth = width) }

    fun applyPreset(preset: LayoutPreset) = viewModelScope.launch {
        val visibleIds = when (preset) {
            LayoutPreset.Stream -> listOf(
                DashboardPanelId.StreamTools,
                DashboardPanelId.HardwareMonitor,
                DashboardPanelId.Discord,
                DashboardPanelId.Media,
                DashboardPanelId.SystemTools,
                DashboardPanelId.QuickActions,
                DashboardPanelId.QuickLaunch,
                DashboardPanelId.NavigationShortcuts,
            )
            LayoutPreset.Work -> listOf(
                DashboardPanelId.DevTools,
                DashboardPanelId.MeetingMode,
                DashboardPanelId.ProcessMonitor,
                DashboardPanelId.TimeTracker,
                DashboardPanelId.Clipboard,
                DashboardPanelId.NavigationShortcuts,
                DashboardPanelId.QuickLaunch,
                DashboardPanelId.HardwareMonitor,
            )
            LayoutPreset.Gaming -> listOf(
                DashboardPanelId.StreamTools,
                DashboardPanelId.Media,
                DashboardPanelId.Discord,
                DashboardPanelId.QuickLaunch,
                DashboardPanelId.SystemTools,
                DashboardPanelId.HardwareMonitor,
                DashboardPanelId.QuickActions,
            )
            LayoutPreset.Minimal -> listOf(
                DashboardPanelId.QuickActions,
                DashboardPanelId.Media,
                DashboardPanelId.QuickLaunch,
                DashboardPanelId.SystemTools,
            )
        }
        val ordered = visibleIds + DashboardPanelId.entries.filter { it !in visibleIds }
        preferencesDataSource.setDashboardLayout(
            layout.value.copy(
                panelSlots = ordered.map { id ->
                    com.streampanel.core.model.DashboardPanelSlot(
                        id = id,
                        visible = id in visibleIds,
                    )
                },
            ),
        )
    }

    fun restoreDefaults() = viewModelScope.launch {
        preferencesDataSource.setDashboardLayout(DashboardLayoutSettings())
    }
}

enum class LayoutPreset {
    Stream,
    Work,
    Gaming,
    Minimal,
}
