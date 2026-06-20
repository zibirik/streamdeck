package com.streampanel.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streampanel.core.designsystem.AppBackdrop
import com.streampanel.core.designsystem.strings
import com.streampanel.core.model.DashboardButton
import com.streampanel.core.network.ConnectionStatus

@Composable
fun DashboardRoute(
    onEditButton: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenConnections: () -> Unit,
    onOpenObs: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreen(
        state = state,
        onPageSelected = viewModel::selectPage,
        onButtonPressed = viewModel::press,
        onEditButton = onEditButton,
        onOpenSettings = onOpenSettings,
        onOpenConnections = onOpenConnections,
        onOpenObs = onOpenObs,
        onMoveButton = viewModel::moveButton,
        onVolumeChange = viewModel::updateVolumeDraft,
        onVolumeSet = { viewModel.applyVolume() },
        onMediaAction = viewModel::mediaAction,
        onMicMute = viewModel::toggleMicMute,
        onSpeakers = viewModel::switchToSpeakers,
        onHeadphones = viewModel::switchToHeadphones,
        onLaunchApp = viewModel::launchApp,
        onOpenFolder = viewModel::openFolder,
        onOpenUrlGroup = viewModel::openUrlGroup,
        onLock = viewModel::lockPc,
        onPaste = viewModel::pasteClipboard,
        onDesktop = viewModel::showDesktop,
        onRefreshVolume = viewModel::refreshVolume,
        onMinimizeAll = { viewModel.windowAction("minimize_all", "All windows minimized") },
        onMaximize = { viewModel.windowAction("maximize_active", "Window maximized") },
        onSnapLeft = { viewModel.windowAction("snap_left", "Snapped left") },
        onSnapRight = { viewModel.windowAction("snap_right", "Snapped right") },
        onMoveToMonitor = viewModel::moveToMonitor,
        onCopy = { viewModel.sendHotkey("CTRL+C", "Copy sent") },
        onAltTab = { viewModel.windowAction("alt_tab", "Alt+Tab") },
        onCloseWindow = { viewModel.windowAction("close_active", "Window closed") },
        onFullscreen = { viewModel.windowAction("fullscreen", "Fullscreen toggled") },
        onRefresh = { viewModel.sendHotkey("CTRL+R", "Refresh sent") },
        onUndo = { viewModel.sendHotkey("CTRL+Z", "Undo sent") },
        onMinimizeActive = { viewModel.windowAction("minimize_active", "Window minimized") },
        onScreenshot = { viewModel.systemAction("screenshot", "Screenshot tool opened") },
        onTaskManager = { viewModel.systemAction("task_manager", "Task Manager opened") },
        onSleepLongPress = viewModel::requestSleepConfirm,
        onDismissSleep = viewModel::dismissSleepConfirm,
        onConfirmSleep = viewModel::confirmSleep,
        onConnect = viewModel::connect,
        onDisconnect = viewModel::disconnect,
        onCreatePage = viewModel::createPage,
        onDeletePage = viewModel::deleteCurrentPage,
        onCreateButton = viewModel::createButton,
        onKillProcess = viewModel::killActiveProcess,
        onDeleteButton = viewModel::deleteButton,
        onShowButtonActions = viewModel::showButtonActions,
        onDismissButtonActions = viewModel::dismissButtonActions,
        onQuickAction = viewModel::quickAction,
        onOpenLayerGridSettings = viewModel::openLayerGridSettings,
        onDismissLayerGridSettings = viewModel::dismissLayerGridSettings,
        onSetPageGrid = viewModel::setCurrentPageGrid,
        onRenamePage = viewModel::renameCurrentPage,
        onPasteFromPc = viewModel::pasteFromPcClipboard,
        onStartPomodoro = viewModel::startPomodoro,
        onSetPomodoroFocusMinutes = viewModel::setPomodoroFocusMinutes,
        onStopPomodoro = viewModel::stopPomodoro,
        onStartTimeTracker = viewModel::startTimeTracker,
        onStopTimeTracker = viewModel::stopTimeTracker,
        onKillProcessByPid = viewModel::killProcessByPid,
        onCleanTemp = viewModel::cleanTemp,
        onOpenDiscord = viewModel::openDiscord,
        onDiscordMute = viewModel::discordMute,
        onDiscordDeafen = viewModel::discordDeafen,
        onDiscordPushToTalk = viewModel::discordPushToTalk,
        onGitPull = viewModel::gitPull,
        onGitStatus = viewModel::gitStatus,
        onDockerPs = viewModel::dockerPs,
        onDockerUp = viewModel::dockerUp,
        onVscodeTest = viewModel::vscodeRunTests,
        onVscodeFormat = viewModel::vscodeFormat,
        onVscodeTerminal = viewModel::vscodeTerminal,
        onWifiRestart = viewModel::restartWifi,
        onFlushDns = viewModel::flushDns,
        onEmptyRecycleBin = viewModel::emptyRecycleBin,
    )
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onPageSelected: (String) -> Unit,
    onButtonPressed: (DashboardButton) -> Unit,
    onEditButton: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenConnections: () -> Unit,
    onOpenObs: () -> Unit,
    onMoveButton: (DashboardButton, Int, Int) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeSet: () -> Unit,
    onMediaAction: (String) -> Unit,
    onMicMute: () -> Unit,
    onSpeakers: () -> Unit,
    onHeadphones: () -> Unit,
    onLaunchApp: (com.streampanel.core.model.QuickLaunchApp) -> Unit,
    onOpenFolder: (com.streampanel.core.model.QuickOpenFolder) -> Unit,
    onOpenUrlGroup: (com.streampanel.core.model.QuickOpenUrlGroup) -> Unit,
    onLock: () -> Unit,
    onPaste: () -> Unit,
    onDesktop: () -> Unit,
    onRefreshVolume: () -> Unit,
    onMinimizeAll: () -> Unit,
    onMaximize: () -> Unit,
    onSnapLeft: () -> Unit,
    onSnapRight: () -> Unit,
    onMoveToMonitor: (Int) -> Unit,
    onCopy: () -> Unit,
    onAltTab: () -> Unit,
    onCloseWindow: () -> Unit,
    onFullscreen: () -> Unit,
    onRefresh: () -> Unit,
    onUndo: () -> Unit,
    onMinimizeActive: () -> Unit,
    onScreenshot: () -> Unit,
    onTaskManager: () -> Unit,
    onSleepLongPress: () -> Unit,
    onDismissSleep: () -> Unit,
    onConfirmSleep: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onCreatePage: (String) -> Unit,
    onDeletePage: () -> Unit,
    onCreateButton: () -> Unit,
    onKillProcess: () -> Unit,
    onDeleteButton: (String) -> Unit,
    onShowButtonActions: (DashboardButton) -> Unit,
    onDismissButtonActions: () -> Unit,
    onQuickAction: (String) -> Unit,
    onOpenLayerGridSettings: () -> Unit,
    onDismissLayerGridSettings: () -> Unit,
    onSetPageGrid: (Int, Int) -> Unit,
    onRenamePage: (String) -> Unit,
    onPasteFromPc: () -> Unit,
    onStartPomodoro: () -> Unit,
    onSetPomodoroFocusMinutes: (Int) -> Unit,
    onStopPomodoro: () -> Unit,
    onStartTimeTracker: (String) -> Unit,
    onStopTimeTracker: () -> Unit,
    onKillProcessByPid: (Int) -> Unit,
    onCleanTemp: () -> Unit,
    onOpenDiscord: () -> Unit,
    onDiscordMute: () -> Unit,
    onDiscordDeafen: () -> Unit,
    onDiscordPushToTalk: () -> Unit,
    onGitPull: () -> Unit,
    onGitStatus: () -> Unit,
    onDockerPs: () -> Unit,
    onDockerUp: () -> Unit,
    onVscodeTest: () -> Unit,
    onVscodeFormat: () -> Unit,
    onVscodeTerminal: () -> Unit,
    onWifiRestart: () -> Unit,
    onFlushDns: () -> Unit,
    onEmptyRecycleBin: () -> Unit,
) {
    val s = strings()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.lastMessage) {
        state.lastMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    val connected = state.connectionStatus is ConnectionStatus.Connected
    val callbacks = DashboardCallbacks(
        onPageSelected = onPageSelected,
        onButtonPressed = onButtonPressed,
        onMoveButton = onMoveButton,
        onShowButtonActions = onShowButtonActions,
        onVolumeChange = onVolumeChange,
        onVolumeSet = onVolumeSet,
        onMediaAction = onMediaAction,
        onMicMute = onMicMute,
        onSpeakers = onSpeakers,
        onHeadphones = onHeadphones,
        onLaunchApp = onLaunchApp,
        onOpenFolder = onOpenFolder,
        onOpenUrlGroup = onOpenUrlGroup,
        onMinimizeAll = onMinimizeAll,
        onMaximize = onMaximize,
        onSnapLeft = onSnapLeft,
        onSnapRight = onSnapRight,
        onMoveToMonitor = onMoveToMonitor,
        onLock = onLock,
        onAltTab = onAltTab,
        onCloseWindow = onCloseWindow,
        onFullscreen = onFullscreen,
        onMinimizeActive = onMinimizeActive,
        onScreenshot = onScreenshot,
        onTaskManager = onTaskManager,
        onKillProcess = onKillProcess,
        onSleepLongPress = onSleepLongPress,
        onQuickAction = onQuickAction,
        onOpenSettings = onOpenSettings,
        onOpenObs = onOpenObs,
        onOpenConnections = onOpenConnections,
        onConnect = onConnect,
        onDisconnect = onDisconnect,
        onCreatePage = onCreatePage,
        onDeletePage = onDeletePage,
        onCreateButton = onCreateButton,
        onOpenLayerGridSettings = onOpenLayerGridSettings,
        onRenamePage = onRenamePage,
        onPasteFromPc = onPasteFromPc,
        onStartPomodoro = onStartPomodoro,
        onSetPomodoroFocusMinutes = onSetPomodoroFocusMinutes,
        onStopPomodoro = onStopPomodoro,
        onStartTimeTracker = onStartTimeTracker,
        onStopTimeTracker = onStopTimeTracker,
        onKillProcessByPid = onKillProcessByPid,
        onCleanTemp = onCleanTemp,
        onOpenDiscord = onOpenDiscord,
        onDiscordMute = onDiscordMute,
        onDiscordDeafen = onDiscordDeafen,
        onDiscordPushToTalk = onDiscordPushToTalk,
        onGitPull = onGitPull,
        onGitStatus = onGitStatus,
        onDockerPs = onDockerPs,
        onDockerUp = onDockerUp,
        onVscodeTest = onVscodeTest,
        onVscodeFormat = onVscodeFormat,
        onVscodeTerminal = onVscodeTerminal,
        onWifiRestart = onWifiRestart,
        onFlushDns = onFlushDns,
        onEmptyRecycleBin = onEmptyRecycleBin,
    )

    AppBackdrop {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            AdaptiveDashboardLayout(
                state = state,
                s = s,
                connected = connected,
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                callbacks = callbacks,
            )
        }
    }

    state.buttonActionDialog?.let { button ->
        AlertDialog(
            onDismissRequest = onDismissButtonActions,
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text(s.macroActionsTitle(button.title)) },
            text = { Text(s.macroActionsBody) },
            confirmButton = {
                TextButton(onClick = {
                    onDismissButtonActions()
                    onEditButton(button.id)
                }) {
                    Text(s.edit, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onDeleteButton(button.id)
                }) {
                    Text(s.delete, color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }

    if (state.showLayerGridDialog) {
        var layerTitle by remember(state.currentPage?.id) { mutableStateOf(state.currentPage?.title ?: "") }
        var rows by remember(state.gridLayout.rows) { mutableStateOf(state.gridLayout.rows.toString()) }
        var cols by remember(state.gridLayout.columns) { mutableStateOf(state.gridLayout.columns.toString()) }
        AlertDialog(
            onDismissRequest = onDismissLayerGridSettings,
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(s.layerGridTitle(state.currentPage?.title ?: s.deckTitle)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(s.layerGridHint, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    com.streampanel.core.designsystem.StreamPanelTextField(
                        value = layerTitle,
                        onValueChange = { layerTitle = it },
                        label = s.name,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(2, 3, 4, 5, 6).forEach { preset ->
                            OutlinedButton(onClick = {
                                rows = preset.toString()
                                cols = preset.toString()
                            }) {
                                Text("${preset}×$preset")
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        com.streampanel.core.designsystem.StreamPanelTextField(
                            value = cols,
                            onValueChange = { cols = it },
                            label = s.columns,
                            modifier = Modifier.weight(1f),
                        )
                        com.streampanel.core.designsystem.StreamPanelTextField(
                            value = rows,
                            onValueChange = { rows = it },
                            label = s.rows,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onRenamePage(layerTitle)
                    onSetPageGrid(rows.toIntOrNull() ?: 4, cols.toIntOrNull() ?: 4)
                    onDismissLayerGridSettings()
                }) {
                    Text(s.apply, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissLayerGridSettings) {
                    Text(s.cancel, color = MaterialTheme.colorScheme.onSurface)
                }
            },
        )
    }

    if (state.showSleepConfirm) {
        AlertDialog(
            onDismissRequest = onDismissSleep,
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text(s.sleepConfirmTitle) },
            text = { Text(s.sleepConfirmBody) },
            confirmButton = {
                TextButton(onClick = onConfirmSleep) {
                    Text(s.sleepNow, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSleep) {
                    Text(s.cancel, color = MaterialTheme.colorScheme.onSurface)
                }
            },
        )
    }
}
