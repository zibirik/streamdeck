package com.streampanel.feature.dashboard

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.ui.Alignment
import com.streampanel.core.designsystem.BigButton
import com.streampanel.core.designsystem.BigField
import com.streampanel.core.designsystem.ChoicePill
import com.streampanel.core.designsystem.MessagePill
import com.streampanel.core.designsystem.SectionLabel
import com.streampanel.core.designsystem.Stepper
import com.streampanel.core.designsystem.StreamDialog
import com.streampanel.core.designsystem.UiPalette
import com.streampanel.core.designsystem.tr
import kotlinx.coroutines.delay

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
    var messageShown by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.lastMessage) {
        val message = state.lastMessage
        if (message != null) {
            messageShown = message
            delay(2600)
            messageShown = null
        }
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
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            AdaptiveDashboardLayout(
                state = state,
                s = s,
                connected = connected,
                callbacks = callbacks,
            )
            MessagePill(
                text = messageShown,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp, start = 16.dp, end = 16.dp),
            )
        }
    }

    state.buttonActionDialog?.let { button ->
        StreamDialog(onDismiss = onDismissButtonActions, title = button.title, message = s.macroActionsBody) {
            BigButton(
                text = s.edit,
                onClick = {
                    onDismissButtonActions()
                    onEditButton(button.id)
                },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Edit,
            )
            BigButton(
                text = s.delete,
                onClick = { onDeleteButton(button.id) },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Delete,
                color = UiPalette.Red,
            )
            BigButton(s.cancel, onDismissButtonActions, Modifier.fillMaxWidth(), filled = false)
        }
    }

    if (state.showLayerGridDialog) {
        LayerSettingsDialog(
            s = s,
            initialTitle = state.currentPage?.title ?: "",
            initialRows = state.gridLayout.rows,
            initialColumns = state.gridLayout.columns,
            onDismiss = onDismissLayerGridSettings,
            onApply = { title, rows, cols ->
                onRenamePage(title)
                onSetPageGrid(rows, cols)
                onDismissLayerGridSettings()
            },
        )
    }

    if (state.showSleepConfirm) {
        StreamDialog(onDismiss = onDismissSleep, title = s.sleepConfirmTitle, message = s.sleepConfirmBody) {
            BigButton(s.sleepNow, onConfirmSleep, Modifier.fillMaxWidth(), icon = Icons.Rounded.Bedtime, color = UiPalette.Violet)
            BigButton(s.cancel, onDismissSleep, Modifier.fillMaxWidth(), filled = false)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LayerSettingsDialog(
    s: com.streampanel.core.designsystem.AppStrings,
    initialTitle: String,
    initialRows: Int,
    initialColumns: Int,
    onDismiss: () -> Unit,
    onApply: (String, Int, Int) -> Unit,
) {
    var title by remember { mutableStateOf(initialTitle) }
    var rows by remember { mutableStateOf(initialRows.coerceIn(2, 8)) }
    var cols by remember { mutableStateOf(initialColumns.coerceIn(2, 8)) }
    StreamDialog(onDismiss = onDismiss, title = tr("Слой и сетка", "Layer and grid"), message = s.layerGridHint) {
        BigField(title, { title = it }, s.name)
        SectionLabel(tr("Быстрый выбор", "Quick pick"))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(2, 3, 4, 5, 6).forEach { preset ->
                ChoicePill("$preset × $preset", selected = rows == preset && cols == preset, onClick = { rows = preset; cols = preset })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stepper(s.columns, cols, 2..8, { cols = it })
            Stepper(s.rows, rows, 2..8, { rows = it })
        }
        BigButton(s.apply, { onApply(title, rows, cols) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Check)
        BigButton(s.cancel, onDismiss, Modifier.fillMaxWidth(), filled = false)
    }
}
