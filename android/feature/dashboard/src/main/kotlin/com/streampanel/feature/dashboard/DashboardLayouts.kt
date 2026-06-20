package com.streampanel.feature.dashboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.streampanel.core.designsystem.AppStrings
import com.streampanel.core.designsystem.ClipboardSharePanel
import com.streampanel.core.designsystem.ConnectionPill
import com.streampanel.core.designsystem.ControlButtonCard
import com.streampanel.core.designsystem.DevToolsPanel
import com.streampanel.core.designsystem.DiscordPanel
import com.streampanel.core.designsystem.GlassSurface
import com.streampanel.core.designsystem.GameStatusPanel
import com.streampanel.core.designsystem.GradientBrandText
import com.streampanel.core.designsystem.HardwareMonitorPanel
import com.streampanel.core.designsystem.MediaControlPanel
import com.streampanel.core.designsystem.MeetingModePanel
import com.streampanel.core.designsystem.NavigationShortcutsPanel
import com.streampanel.core.designsystem.PcConfiguratorPanel
import com.streampanel.core.designsystem.PomodoroPanel
import com.streampanel.core.designsystem.ProcessMonitorPanel
import com.streampanel.core.designsystem.QuickActionsStrip
import com.streampanel.core.designsystem.QuickLaunchGrid
import com.streampanel.core.designsystem.SidebarIconActions
import com.streampanel.core.designsystem.SidebarNavItem
import com.streampanel.core.designsystem.StreamToolsPanel
import com.streampanel.core.designsystem.StreamChatPanel
import com.streampanel.core.designsystem.StudyModePanel
import com.streampanel.core.designsystem.SystemToolsPanel
import com.streampanel.core.designsystem.TimeTrackerPanel
import com.streampanel.core.designsystem.WindowWidthSizeClass
import com.streampanel.core.designsystem.buttonHeightForWidth
import com.streampanel.core.designsystem.gridColumnsForWidth
import com.streampanel.core.designsystem.toWidthSizeClass
import com.streampanel.core.model.DashboardButton
import com.streampanel.core.model.DashboardLayoutSettings
import com.streampanel.core.model.DashboardPanelId
import com.streampanel.core.model.DashboardZone
import com.streampanel.core.model.QuickLaunchApp
import com.streampanel.core.model.QuickOpenFolder
import com.streampanel.core.model.QuickOpenUrlGroup
import com.streampanel.core.model.ToolsColumnWidth
import com.streampanel.core.network.ConnectionStatus

private enum class DashboardTab { Deck, Tools, Menu }

private fun DashboardZone.toTab(): DashboardTab = when (this) {
    DashboardZone.Sidebar -> DashboardTab.Menu
    DashboardZone.Deck -> DashboardTab.Deck
    DashboardZone.ToolsColumn -> DashboardTab.Tools
}

private fun toolsColumnWidthDp(width: ToolsColumnWidth, sizeClass: WindowWidthSizeClass) = when (width) {
    ToolsColumnWidth.Narrow -> if (sizeClass == WindowWidthSizeClass.Medium) 240.dp else 280.dp
    ToolsColumnWidth.Medium -> if (sizeClass == WindowWidthSizeClass.Medium) 280.dp else 328.dp
    ToolsColumnWidth.Wide -> if (sizeClass == WindowWidthSizeClass.Medium) 340.dp else 400.dp
}

@Composable
fun AdaptiveDashboardLayout(
    state: DashboardUiState,
    s: AppStrings,
    connected: Boolean,
    modifier: Modifier = Modifier,
    callbacks: DashboardCallbacks,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val sizeClass = maxWidth.toWidthSizeClass()
        when (sizeClass) {
            WindowWidthSizeClass.Compact -> CompactDashboardLayout(state, s, connected, sizeClass, callbacks)
            else -> WideDashboardLayout(state, s, connected, sizeClass, callbacks)
        }
    }
}

@Composable
private fun CompactDashboardLayout(
    state: DashboardUiState,
    s: AppStrings,
    connected: Boolean,
    sizeClass: WindowWidthSizeClass,
    callbacks: DashboardCallbacks,
) {
    val layout = state.layout
    val tabOrder = layout.compactTabOrder.map { it.toTab() }
    var tab by rememberSaveable { mutableStateOf(tabOrder.firstOrNull() ?: DashboardTab.Deck) }
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
                tabOrder.forEach { tabItem ->
                    NavigationBarItem(
                        selected = tab == tabItem,
                        onClick = { tab = tabItem },
                        icon = {
                            Icon(
                                when (tabItem) {
                                    DashboardTab.Deck -> Icons.Default.Dashboard
                                    DashboardTab.Tools -> Icons.Default.Tune
                                    DashboardTab.Menu -> Icons.Default.Menu
                                },
                                null,
                            )
                        },
                        label = {
                            Text(
                                when (tabItem) {
                                    DashboardTab.Deck -> s.tabDeck
                                    DashboardTab.Tools -> s.tabTools
                                    DashboardTab.Menu -> s.tabMenu
                                },
                            )
                        },
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (tab) {
                DashboardTab.Deck -> {
                    CompactDeckHeader(state, s, connected, callbacks)
                    DeckGrid(state, sizeClass, callbacks)
                }
                DashboardTab.Tools -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DashboardPanelsContent(state, layout, callbacks)
                    }
                }
                DashboardTab.Menu -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CompactMenuContent(state, s, connected, callbacks)
                    }
                }
            }
        }
    }
}

@Composable
private fun WideDashboardLayout(
    state: DashboardUiState,
    s: AppStrings,
    connected: Boolean,
    sizeClass: WindowWidthSizeClass,
    callbacks: DashboardCallbacks,
) {
    val layout = state.layout
    val sideWidth = if (sizeClass == WindowWidthSizeClass.Medium) 220.dp else 258.dp
    val panelWidth = toolsColumnWidthDp(layout.toolsColumnWidth, sizeClass)
    val zones = layout.zoneOrder.filter { zone ->
        zone != DashboardZone.Sidebar || layout.sidebarVisible
    }
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(if (sizeClass == WindowWidthSizeClass.Medium) 12.dp else 20.dp),
    ) {
        zones.forEach { zone ->
            when (zone) {
                DashboardZone.Sidebar -> {
                    GlassSurface(modifier = Modifier.width(sideWidth).fillMaxHeight(), elevated = true) {
                        SidebarContent(state, s, connected, callbacks)
                    }
                }
                DashboardZone.Deck -> {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        DeckHeader(state, s, connected, callbacks)
                        DeckGrid(state, sizeClass, callbacks)
                    }
                }
                DashboardZone.ToolsColumn -> {
                    Column(
                        modifier = Modifier.width(panelWidth).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        DashboardPanelsContent(state, layout, callbacks)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactDeckHeader(state: DashboardUiState, s: AppStrings, connected: Boolean, callbacks: DashboardCallbacks) {
    GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        state.currentPage?.title ?: s.deckTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(s.controlsCount(state.gridLayout.columns, state.gridLayout.rows, state.buttons.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(if (connected) s.ready else s.offline, style = MaterialTheme.typography.labelMedium, color = if (connected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                state.pages.forEach { page ->
                    OutlinedButton(onClick = { callbacks.onPageSelected(page.id) }) {
                        Text(page.title, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = callbacks.onOpenLayerGridSettings, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.GridView, null, modifier = Modifier.padding(end = 4.dp))
                    Text(s.grid)
                }
                OutlinedButton(onClick = callbacks.onCreateButton, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.padding(end = 4.dp))
                    Text(s.newMacro)
                }
            }
        }
    }
}

@Composable
private fun CompactMenuContent(state: DashboardUiState, s: AppStrings, connected: Boolean, callbacks: DashboardCallbacks) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientBrandText(text = s.appName, style = MaterialTheme.typography.headlineMedium)
            Text(state.profile?.name ?: s.controlDeck, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ConnectionPill(
                label = connectionLabel(state.connectionStatus, s),
                connected = connected,
                onClick = {
                    if (connected) callbacks.onDisconnect() else callbacks.onConnect()
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { callbacks.onCreatePage("Layer ${state.pages.size + 1}") }, modifier = Modifier.weight(1f)) {
                    Text(s.newLayer)
                }
                if (state.pages.size > 1) {
                    OutlinedButton(onClick = callbacks.onDeletePage) { Text(s.delete) }
                }
            }
            state.pages.forEach { page ->
                SidebarNavItem(
                    label = page.title,
                    selected = page.id == state.currentPage?.id,
                    onClick = { callbacks.onPageSelected(page.id) },
                )
            }
            SidebarIconActions(callbacks.onOpenObs, callbacks.onOpenConnections, callbacks.onOpenSettings)
        }
    }
}

@Composable
private fun SidebarContent(state: DashboardUiState, s: AppStrings, connected: Boolean, callbacks: DashboardCallbacks) {
    Column(Modifier.fillMaxSize().padding(22.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GradientBrandText(text = s.appName, style = MaterialTheme.typography.headlineLarge)
                Text(state.profile?.name ?: s.controlDeck, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ConnectionPill(
                label = connectionLabel(state.connectionStatus, s),
                connected = connected,
                onClick = {
                    if (connected) callbacks.onDisconnect() else callbacks.onConnect()
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { callbacks.onCreatePage("Layer ${state.pages.size + 1}") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.padding(end = 4.dp))
                    Text(s.newLayer)
                }
                if (state.pages.size > 1) {
                    OutlinedButton(onClick = callbacks.onDeletePage) { Text(s.delete) }
                }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            state.pages.forEach { page ->
                SidebarNavItem(
                    label = page.title,
                    selected = page.id == state.currentPage?.id,
                    onClick = { callbacks.onPageSelected(page.id) },
                )
            }
        }
        SidebarIconActions(callbacks.onOpenObs, callbacks.onOpenConnections, callbacks.onOpenSettings)
    }
}

@Composable
private fun DeckHeader(state: DashboardUiState, s: AppStrings, connected: Boolean, callbacks: DashboardCallbacks) {
    GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    state.currentPage?.title ?: s.deckTitle,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(s.controlsCount(state.gridLayout.columns, state.gridLayout.rows, state.buttons.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = callbacks.onOpenLayerGridSettings) {
                    Icon(Icons.Default.GridView, null, modifier = Modifier.padding(end = 4.dp))
                    Text(s.grid)
                }
                OutlinedButton(onClick = callbacks.onCreateButton) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.padding(end = 4.dp))
                    Text(s.newMacro)
                }
                Text(if (connected) s.ready else s.offline, style = MaterialTheme.typography.labelLarge, color = if (connected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DeckGrid(state: DashboardUiState, sizeClass: WindowWidthSizeClass, callbacks: DashboardCallbacks) {
    val columns = gridColumnsForWidth(sizeClass, state.gridLayout.columns)
    val buttonHeight = buttonHeightForWidth(sizeClass)
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(if (sizeClass == WindowWidthSizeClass.Compact) 10.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(if (sizeClass == WindowWidthSizeClass.Compact) 10.dp else 16.dp),
    ) {
        items(state.buttons, key = { it.id }) { button ->
            var dragX = 0f
            var dragY = 0f
            ControlButtonCard(
                button = button,
                executing = button.id == state.executingButtonId,
                modifier = Modifier
                    .height(buttonHeight)
                    .pointerInput(button.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { dragX = 0f; dragY = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragX += dragAmount.x
                                dragY += dragAmount.y
                            },
                            onDragEnd = {
                                val columnDelta = when { dragX > 80f -> 1; dragX < -80f -> -1; else -> 0 }
                                val rowDelta = when { dragY > 80f -> 1; dragY < -80f -> -1; else -> 0 }
                                callbacks.onMoveButton(button, rowDelta, columnDelta)
                            },
                        )
                    },
                onClick = { callbacks.onButtonPressed(button) },
                onLongClick = { callbacks.onShowButtonActions(button) },
            )
        }
    }
}

@Composable
fun DashboardPanelsContent(
    state: DashboardUiState,
    layout: DashboardLayoutSettings,
    callbacks: DashboardCallbacks,
) {
    layout.orderedVisiblePanels.forEach { panelId ->
        when (panelId) {
            DashboardPanelId.HardwareMonitor -> HardwareMonitorPanel(
                cpuPercent = state.cpuPercent,
                ramPercent = state.ramPercent,
                foregroundProcess = state.foregroundProcess,
                downloadMbps = state.downloadMbps,
                uploadMbps = state.uploadMbps,
                diskFreePercent = state.diskFreePercent,
                storageDrives = state.storageDrives,
                networkInterface = state.networkInterface,
            )
            DashboardPanelId.ProcessMonitor -> if (state.topProcesses.isNotEmpty()) {
                ProcessMonitorPanel(state.topProcesses, callbacks.onKillProcessByPid, callbacks.onCleanTemp)
            }
            DashboardPanelId.DevTools -> DevToolsPanel(
                onGitPull = callbacks.onGitPull,
                onGitStatus = callbacks.onGitStatus,
                onDockerPs = callbacks.onDockerPs,
                onDockerUp = callbacks.onDockerUp,
                onVscodeTest = callbacks.onVscodeTest,
                onVscodeFormat = callbacks.onVscodeFormat,
                onVscodeTerminal = callbacks.onVscodeTerminal,
                onWifiRestart = callbacks.onWifiRestart,
                onFlushDns = callbacks.onFlushDns,
                onEmptyRecycleBin = callbacks.onEmptyRecycleBin,
            )
            DashboardPanelId.StreamTools -> StreamToolsPanel(
                onOpenObs = callbacks.onOpenObs,
                onOpenTwitch = { callbacks.onOpenUrlGroup(QuickOpenUrlGroup("twitch-tools", "Twitch", "https://dashboard.twitch.tv")) },
                onOpenYoutube = { callbacks.onOpenUrlGroup(QuickOpenUrlGroup("youtube-tools", "YouTube", "https://studio.youtube.com|https://www.youtube.com/live_dashboard")) },
                onOpenChat = {
                    callbacks.onOpenUrlGroup(
                        state.quickOpenUrlGroups.firstOrNull { it.id == "stream-kit" }
                            ?: QuickOpenUrlGroup("stream-chat", "Chat", "https://dashboard.twitch.tv|https://studio.youtube.com"),
                    )
                },
                onSaveReplay = callbacks.onOpenObs,
            )
            DashboardPanelId.StreamChat -> StreamChatPanel(
                settings = state.streamChatSettings,
                onOpenSettings = callbacks.onOpenSettings,
            )
            DashboardPanelId.GameStatus -> GameStatusPanel(
                settings = state.gameOverlaySettings,
                gameInfo = state.gameInfo,
                onOpenSettings = callbacks.onOpenSettings,
            )
            DashboardPanelId.PcConfigurator -> PcConfiguratorPanel(
                url = state.pcConfiguratorUrl,
                onOpenConfigurator = {
                    state.pcConfiguratorUrl?.let {
                        callbacks.onOpenUrlGroup(QuickOpenUrlGroup("pc-configurator", "PC Configurator", it))
                    }
                },
                onOpenSettings = callbacks.onOpenSettings,
            )
            DashboardPanelId.Discord -> DiscordPanel(
                callbacks.onOpenDiscord,
                callbacks.onDiscordMute,
                callbacks.onDiscordDeafen,
                callbacks.onDiscordPushToTalk,
            )
            DashboardPanelId.StudyMode -> StudyModePanel(
                onStartFocus = callbacks.onStartPomodoro,
                onOpenStudyPack = { callbacks.onOpenUrlGroup(QuickOpenUrlGroup("study-pack", "Study", "https://calendar.google.com|https://docs.google.com|https://chat.openai.com")) },
                onOpenNotes = { callbacks.onOpenUrlGroup(QuickOpenUrlGroup("notes", "Notes", "https://docs.google.com/document/u/0/")) },
            )
            DashboardPanelId.MeetingMode -> MeetingModePanel(
                onMicMute = callbacks.onMicMute,
                onOpenDiscord = callbacks.onOpenDiscord,
                onOpenTeams = { callbacks.onLaunchApp(QuickLaunchApp("teams", "Teams", "msteams:", "chat", "#6264A7")) },
                onOpenZoom = { callbacks.onLaunchApp(QuickLaunchApp("zoom", "Zoom", "zoommtg:", "videocam", "#2D8CFF")) },
            )
            DashboardPanelId.Pomodoro -> PomodoroPanel(
                state.pomodoroRemainingSeconds,
                state.pomodoroFocusMinutes * 60,
                state.pomodoroRunning,
                state.pomodoroFocusMinutes,
                callbacks.onSetPomodoroFocusMinutes,
                callbacks.onStartPomodoro,
                callbacks.onStopPomodoro,
            )
            DashboardPanelId.TimeTracker -> if (state.timeTrackerProjects.isNotEmpty()) {
                TimeTrackerPanel(
                    state.timeTrackerProjects,
                    state.activeTimeProjectId,
                    state.timeTrackerElapsedSeconds,
                    callbacks.onStartTimeTracker,
                    callbacks.onStopTimeTracker,
                )
            }
            DashboardPanelId.Clipboard -> ClipboardSharePanel(state.clipboardPreview, callbacks.onPasteFromPc)
            DashboardPanelId.QuickActions -> QuickActionsStrip(state.quickActions, callbacks.onQuickAction, callbacks.onOpenSettings)
            DashboardPanelId.Media -> MediaControlPanel(
                volume = state.volumeLevel,
                onVolumeChange = callbacks.onVolumeChange,
                onVolumeSet = { _ -> callbacks.onVolumeSet() },
                onPlayPause = { callbacks.onMediaAction("play_pause") },
                onPrevious = { callbacks.onMediaAction("previous") },
                onNext = { callbacks.onMediaAction("next") },
                onMute = { callbacks.onMediaAction("mute") },
                onMicMute = callbacks.onMicMute,
                onSpeakers = callbacks.onSpeakers,
                onHeadphones = callbacks.onHeadphones,
                isMuted = state.isMuted,
                isMicMuted = state.isMicMuted,
                isPlaying = state.isPlaying,
                nowPlaying = state.nowPlaying,
            )
            DashboardPanelId.SystemTools -> SystemToolsPanel(
                callbacks.onMinimizeAll,
                callbacks.onMaximize,
                callbacks.onMinimizeActive,
                callbacks.onSnapLeft,
                callbacks.onSnapRight,
                callbacks.onMoveToMonitor,
                callbacks.onScreenshot,
                callbacks.onTaskManager,
                callbacks.onKillProcess,
                callbacks.onLock,
                callbacks.onAltTab,
                callbacks.onCloseWindow,
                callbacks.onFullscreen,
                {},
                callbacks.onSleepLongPress,
            )
            DashboardPanelId.NavigationShortcuts -> NavigationShortcutsPanel(
                state.quickOpenFolders,
                state.quickOpenUrlGroups,
                callbacks.onOpenFolder,
                callbacks.onOpenUrlGroup,
                callbacks.onOpenSettings,
            )
            DashboardPanelId.QuickLaunch -> QuickLaunchGrid(state.quickLaunchApps, callbacks.onLaunchApp, callbacks.onOpenSettings)
        }
    }
}

data class DashboardCallbacks(
    val onPageSelected: (String) -> Unit,
    val onButtonPressed: (DashboardButton) -> Unit,
    val onMoveButton: (DashboardButton, Int, Int) -> Unit,
    val onShowButtonActions: (DashboardButton) -> Unit,
    val onVolumeChange: (Float) -> Unit,
    val onVolumeSet: () -> Unit,
    val onMediaAction: (String) -> Unit,
    val onMicMute: () -> Unit,
    val onSpeakers: () -> Unit,
    val onHeadphones: () -> Unit,
    val onLaunchApp: (QuickLaunchApp) -> Unit,
    val onOpenFolder: (QuickOpenFolder) -> Unit,
    val onOpenUrlGroup: (QuickOpenUrlGroup) -> Unit,
    val onMinimizeAll: () -> Unit,
    val onMaximize: () -> Unit,
    val onSnapLeft: () -> Unit,
    val onSnapRight: () -> Unit,
    val onMoveToMonitor: (Int) -> Unit,
    val onLock: () -> Unit,
    val onAltTab: () -> Unit,
    val onCloseWindow: () -> Unit,
    val onFullscreen: () -> Unit,
    val onMinimizeActive: () -> Unit,
    val onScreenshot: () -> Unit,
    val onTaskManager: () -> Unit,
    val onKillProcess: () -> Unit,
    val onSleepLongPress: () -> Unit,
    val onQuickAction: (String) -> Unit,
    val onOpenSettings: () -> Unit,
    val onOpenObs: () -> Unit,
    val onOpenConnections: () -> Unit,
    val onConnect: () -> Unit,
    val onDisconnect: () -> Unit,
    val onCreatePage: (String) -> Unit,
    val onDeletePage: () -> Unit,
    val onCreateButton: () -> Unit,
    val onOpenLayerGridSettings: () -> Unit,
    val onRenamePage: (String) -> Unit,
    val onPasteFromPc: () -> Unit,
    val onStartPomodoro: () -> Unit,
    val onSetPomodoroFocusMinutes: (Int) -> Unit,
    val onStopPomodoro: () -> Unit,
    val onStartTimeTracker: (String) -> Unit,
    val onStopTimeTracker: () -> Unit,
    val onKillProcessByPid: (Int) -> Unit,
    val onCleanTemp: () -> Unit,
    val onOpenDiscord: () -> Unit,
    val onDiscordMute: () -> Unit,
    val onDiscordDeafen: () -> Unit,
    val onDiscordPushToTalk: () -> Unit,
    val onGitPull: () -> Unit,
    val onGitStatus: () -> Unit,
    val onDockerPs: () -> Unit,
    val onDockerUp: () -> Unit,
    val onVscodeTest: () -> Unit,
    val onVscodeFormat: () -> Unit,
    val onVscodeTerminal: () -> Unit,
    val onWifiRestart: () -> Unit,
    val onFlushDns: () -> Unit,
    val onEmptyRecycleBin: () -> Unit,
)

private fun connectionLabel(status: ConnectionStatus, s: AppStrings): String = when (status) {
    ConnectionStatus.Connecting -> s.connecting
    ConnectionStatus.Disconnected -> s.offline
    is ConnectionStatus.Connected -> s.online
    is ConnectionStatus.Failed -> s.connectionError
}
