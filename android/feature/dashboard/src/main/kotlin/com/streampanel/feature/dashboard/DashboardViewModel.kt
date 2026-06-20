package com.streampanel.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streampanel.core.database.ControlPanelRepository
import com.streampanel.core.datastore.PreferencesDataSource
import com.streampanel.core.execution.ActionExecutor
import com.streampanel.core.model.ActionType
import com.streampanel.core.model.ActionWhen
import com.streampanel.core.model.ButtonState
import com.streampanel.core.model.ControlAction
import com.streampanel.core.model.ControlPage
import com.streampanel.core.model.ControlProfile
import com.streampanel.core.model.DashboardButton
import com.streampanel.core.model.DashboardLayoutSettings
import com.streampanel.core.model.GameOverlaySettings
import com.streampanel.core.model.GameTelemetryInfo
import com.streampanel.core.model.GridLayout
import com.streampanel.core.model.PcStorageDrive
import com.streampanel.core.model.PcProcessInfo
import com.streampanel.core.model.QuickActionItem
import com.streampanel.core.model.QuickLaunchApp
import com.streampanel.core.model.TimeLogEntry
import com.streampanel.core.model.TimeTrackerProject
import com.streampanel.core.model.QuickOpenFolder
import com.streampanel.core.model.QuickOpenUrlGroup
import com.streampanel.core.model.ServerConnectionSettings
import com.streampanel.core.model.StreamChatSettings
import com.streampanel.core.network.ConnectionStatus
import com.streampanel.core.network.PcConnectionClient
import com.streampanel.core.network.PcConnectionManager
import com.streampanel.core.network.PcServerStatus
import com.streampanel.core.network.PcStatusClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: ControlPanelRepository,
    private val preferencesDataSource: PreferencesDataSource,
    private val actionExecutor: ActionExecutor,
    private val pcConnectionClient: PcConnectionClient,
    private val connectionManager: PcConnectionManager,
    private val pcStatusClient: PcStatusClient,
) : ViewModel() {
    private val selectedPageId = MutableStateFlow<String?>(null)
    private val executingButtonId = MutableStateFlow<String?>(null)
    private val lastMessage = MutableStateFlow<String?>(null)
    private val volumeDraft = MutableStateFlow(50f)
    private val showSleepConfirm = MutableStateFlow(false)
    private val isMuted = MutableStateFlow(false)
    private val isMicMuted = MutableStateFlow(false)
    private val isPlaying = MutableStateFlow(false)
    private val nowPlaying = MutableStateFlow<String?>(null)
    private val cpuPercent = MutableStateFlow<Float?>(null)
    private val ramPercent = MutableStateFlow<Float?>(null)
    private val foregroundProcess = MutableStateFlow<String?>(null)
    private val clipboardPreview = MutableStateFlow<String?>(null)
    private val downloadMbps = MutableStateFlow<Float?>(null)
    private val uploadMbps = MutableStateFlow<Float?>(null)
    private val diskFreePercent = MutableStateFlow<Float?>(null)
    private val storageDrives = MutableStateFlow<List<PcStorageDrive>>(emptyList())
    private val networkInterface = MutableStateFlow<String?>(null)
    private val topProcesses = MutableStateFlow<List<PcProcessInfo>>(emptyList())
    private val gameInfo = MutableStateFlow(GameTelemetryInfo())
    private val pomodoroRemaining = MutableStateFlow(25 * 60)
    private val pomodoroRunning = MutableStateFlow(false)
    private val timeTrackerProjects = MutableStateFlow<List<TimeTrackerProject>>(emptyList())
    private val activeTimeProjectId = MutableStateFlow<String?>(null)
    private val timeTrackerElapsed = MutableStateFlow(0)
    private val timeTrackerStartedAt = MutableStateFlow<Long?>(null)
    private val showLayerGridDialog = MutableStateFlow(false)
    private val buttonActionDialog = MutableStateFlow<DashboardButton?>(null)

    private val profile = repository.observeActiveProfile()
    private val pages = profile.flatMapLatest { prof ->
        if (prof == null) flowOf(emptyList()) else repository.observePages(prof.id)
    }
    private val currentPage = combine(pages, selectedPageId) { availablePages, selected ->
        availablePages.firstOrNull { it.id == selected } ?: availablePages.firstOrNull()
    }
    private val buttons = currentPage.flatMapLatest { page ->
        if (page == null) flowOf(emptyList()) else repository.observeButtons(page.id)
    }

    private val deckState = combine(profile, pages, currentPage, buttons) { prof, pageList, page, buttonList ->
        DeckSnapshot(prof, pageList, page, buttonList)
    }

    private val prefsState = combine(
        combine(
            preferencesDataSource.gridLayout,
            preferencesDataSource.pageGridLayouts,
            preferencesDataSource.quickLaunchApps,
        ) { grid, pageGrids, apps -> Triple(grid, pageGrids, apps) },
        combine(
            preferencesDataSource.quickOpenFolders,
            preferencesDataSource.quickOpenUrlGroups,
            preferencesDataSource.quickActions,
        ) { folders, urlGroups, quickActions -> Triple(folders, urlGroups, quickActions) },
        combine(
            preferencesDataSource.dashboardLayout,
            preferencesDataSource.pomodoroFocusMinutes,
            preferencesDataSource.streamChatSettings,
        ) { layout, pomodoroMinutes, streamChat -> Triple(layout, pomodoroMinutes, streamChat) },
        combine(
            preferencesDataSource.gameOverlaySettings,
            preferencesDataSource.serverConnection,
        ) { gameOverlay, server -> gameOverlay to server },
    ) { (grid, pageGrids, apps), (folders, urlGroups, quickActions), (layout, pomodoroMinutes, streamChat), (gameOverlay, server) ->
        PrefsSnapshot(grid, pageGrids, apps, folders, urlGroups, quickActions, layout, pomodoroMinutes, streamChat, gameOverlay, server)
    }

    private val runtimeState = combine(
        combine(
            pcConnectionClient.status,
            executingButtonId,
            lastMessage,
        ) { status, executing, message -> Triple(status, executing, message) },
        combine(volumeDraft, showSleepConfirm, showLayerGridDialog) { volume, sleep, layer ->
            Triple(volume, sleep, layer)
        },
        buttonActionDialog,
    ) { (status, executing, message), (volume, sleepConfirm, layerGrid), actionDialog ->
        RuntimeSnapshot(status, executing, message, volume, sleepConfirm, layerGrid, actionDialog)
    }

    private val extendedState = combine(
        combine(cpuPercent, ramPercent, foregroundProcess, clipboardPreview) { cpu, ram, fg, clip ->
            PcStatsSnapshot(cpu, ram, fg, clip)
        },
        combine(
            combine(downloadMbps, uploadMbps, diskFreePercent, networkInterface, topProcesses) { down, up, disk, nic, procs ->
                NetworkSnapshot(down, up, disk, nic, procs, emptyList())
            },
            storageDrives,
        ) { network, drives -> network.copy(storageDrives = drives) },
        combine(pomodoroRemaining, pomodoroRunning) { rem, run -> PomodoroSnapshot(rem, run) },
        combine(timeTrackerProjects, activeTimeProjectId, timeTrackerElapsed) { projects, active, elapsed ->
            TimeTrackerSnapshot(projects, active, elapsed)
        },
        gameInfo,
    ) { pc, network, pomodoro, tracker, game ->
        ExtendedSnapshot(pc, network, pomodoro, tracker, game)
    }

    val uiState = combine(
        deckState,
        prefsState,
        runtimeState,
        combine(isMuted, isMicMuted, isPlaying, nowPlaying) { muted, micMuted, playing, track ->
            AudioSnapshot(muted, micMuted, playing, track)
        },
        extendedState,
    ) { deck, prefs, runtime, audio, extended ->
        val pageGrid = deck.currentPage?.id?.let { prefs.pageGrids[it] } ?: prefs.defaultGrid
        DashboardUiState(
            profile = deck.profile,
            pages = deck.pages,
            currentPage = deck.currentPage,
            buttons = deck.buttons,
            gridLayout = pageGrid,
            quickLaunchApps = prefs.apps.filter { it.enabled },
            quickOpenFolders = prefs.folders,
            quickOpenUrlGroups = prefs.urlGroups,
            quickActions = prefs.quickActions,
            layout = prefs.layout,
            streamChatSettings = prefs.streamChatSettings,
            gameOverlaySettings = prefs.gameOverlaySettings,
            pcConfiguratorUrl = "http://${prefs.server.host}:${prefs.server.port}",
            connectionStatus = runtime.status,
            executingButtonId = runtime.executingId,
            lastMessage = runtime.message,
            volumeLevel = runtime.volume,
            showSleepConfirm = runtime.sleepConfirm,
            showLayerGridDialog = runtime.layerGridDialog,
            buttonActionDialog = runtime.actionDialog,
            isMuted = audio.muted,
            isMicMuted = audio.micMuted,
            isPlaying = audio.playing,
            nowPlaying = audio.track,
            cpuPercent = extended.pc.cpu,
            ramPercent = extended.pc.ram,
            foregroundProcess = extended.pc.foreground,
            clipboardPreview = extended.pc.clipboard,
            downloadMbps = extended.network.download,
            uploadMbps = extended.network.upload,
            diskFreePercent = extended.network.disk,
            storageDrives = extended.network.storageDrives,
            networkInterface = extended.network.nic,
            topProcesses = extended.network.processes,
            gameInfo = extended.game,
            pomodoroRemainingSeconds = extended.pomodoro.remainingSeconds,
            pomodoroRunning = extended.pomodoro.running,
            pomodoroFocusMinutes = prefs.pomodoroFocusMinutes,
            timeTrackerProjects = extended.tracker.projects,
            activeTimeProjectId = extended.tracker.activeId,
            timeTrackerElapsedSeconds = extended.tracker.elapsed,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )

    init {
        viewModelScope.launch {
            preferencesDataSource.timeTrackerProjects.collect { timeTrackerProjects.value = it }
        }
        viewModelScope.launch {
            preferencesDataSource.pomodoroFocusMinutes.collect { minutes ->
                if (!pomodoroRunning.value) {
                    pomodoroRemaining.value = minutes.coerceIn(1, 240) * 60
                }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                if (pomodoroRunning.value && pomodoroRemaining.value > 0) {
                    delay(1_000)
                    pomodoroRemaining.value = (pomodoroRemaining.value - 1).coerceAtLeast(0)
                    if (pomodoroRemaining.value == 0) {
                        pomodoroRunning.value = false
                        systemAction("focus_mode_off", "Pomodoro break")
                        lastMessage.value = "Pomodoro complete — take a break!"
                    }
                } else if (activeTimeProjectId.value != null) {
                    delay(1_000)
                    timeTrackerStartedAt.value?.let { start ->
                        timeTrackerElapsed.value = ((System.currentTimeMillis() - start) / 1000).toInt()
                    }
                } else {
                    delay(1_000)
                }
            }
        }
        viewModelScope.launch {
            runCatching { repository.seedIfEmpty() }
            delay(500)
            val settings = runCatching { preferencesDataSource.serverConnection.first() }.getOrNull()
            if (settings?.autoConnect == true) {
                runCatching { connectionManager.connect(settings) }
            }
        }
        viewModelScope.launch {
            runCatching {
                pcConnectionClient.status.collect { status ->
                    if (status is ConnectionStatus.Connected) {
                        refreshPcStatus()
                    }
                }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                runCatching {
                    if (pcConnectionClient.status.value is ConnectionStatus.Connected) {
                        refreshPcStatus()
                    }
                }
                delay(5_000)
            }
        }
    }

    fun connect() = viewModelScope.launch {
        val settings = preferencesDataSource.serverConnection.first()
        connectionManager.connect(settings)
        lastMessage.value = when (pcConnectionClient.status.value) {
            is ConnectionStatus.Connected -> "Connected to ${settings.host}"
            is ConnectionStatus.Failed -> "Connection failed"
            else -> "Connecting..."
        }
    }

    fun disconnect() = viewModelScope.launch {
        connectionManager.disconnect()
        lastMessage.value = "Disconnected"
    }

    fun createPage(title: String) {
        val profileId = uiState.value.profile?.id ?: return
        viewModelScope.launch {
            val page = repository.createPage(profileId, title)
            selectedPageId.value = page.id
            lastMessage.value = "Layer \"$title\" created"
        }
    }

    fun deleteCurrentPage() {
        val page = uiState.value.currentPage ?: return
        if (uiState.value.pages.size <= 1) return
        viewModelScope.launch {
            repository.deletePage(page.id)
            selectedPageId.value = null
            lastMessage.value = "Layer deleted"
        }
    }

    fun deleteButton(buttonId: String) {
        viewModelScope.launch {
            repository.deleteButton(buttonId)
            buttonActionDialog.value = null
            lastMessage.value = "Macro deleted"
        }
    }

    fun showButtonActions(button: DashboardButton) {
        buttonActionDialog.value = button
    }

    fun dismissButtonActions() {
        buttonActionDialog.value = null
    }

    fun openLayerGridSettings() {
        showLayerGridDialog.value = true
    }

    fun dismissLayerGridSettings() {
        showLayerGridDialog.value = false
    }

    fun setCurrentPageGrid(rows: Int, columns: Int) {
        val pageId = uiState.value.currentPage?.id ?: return
        viewModelScope.launch {
            preferencesDataSource.setPageGridLayout(
                pageId,
                GridLayout(rows.coerceIn(2, 8), columns.coerceIn(2, 8)),
            )
            lastMessage.value = "Layer grid: ${columns.coerceIn(2, 8)}×${rows.coerceIn(2, 8)}"
        }
    }

    fun renameCurrentPage(title: String) {
        val pageId = uiState.value.currentPage?.id ?: return
        val cleanTitle = title.trim().ifBlank { "Layer" }.take(40)
        viewModelScope.launch {
            repository.renamePage(pageId, cleanTitle)
            lastMessage.value = "Layer renamed: $cleanTitle"
        }
    }

    fun quickAction(actionKey: String) {
        when (actionKey) {
            com.streampanel.core.model.QuickActionKeys.LOCK -> lockPc()
            com.streampanel.core.model.QuickActionKeys.PASTE -> pasteClipboard()
            com.streampanel.core.model.QuickActionKeys.DESKTOP -> showDesktop()
            com.streampanel.core.model.QuickActionKeys.SYNC_VOLUME -> refreshVolume()
            com.streampanel.core.model.QuickActionKeys.COPY -> sendHotkey("CTRL+C", "Copy sent")
            com.streampanel.core.model.QuickActionKeys.ALT_TAB -> windowAction("alt_tab", "Alt+Tab")
            com.streampanel.core.model.QuickActionKeys.CLOSE_WINDOW -> windowAction("close_active", "Window closed")
            com.streampanel.core.model.QuickActionKeys.FULLSCREEN -> windowAction("fullscreen", "Fullscreen toggled")
            com.streampanel.core.model.QuickActionKeys.REFRESH -> sendHotkey("CTRL+R", "Refresh sent")
            com.streampanel.core.model.QuickActionKeys.UNDO -> sendHotkey("CTRL+Z", "Undo sent")
            com.streampanel.core.model.QuickActionKeys.CUT -> sendHotkey("CTRL+X", "Cut sent")
            com.streampanel.core.model.QuickActionKeys.REDO -> sendHotkey("CTRL+Y", "Redo sent")
            com.streampanel.core.model.QuickActionKeys.SAVE -> sendHotkey("CTRL+S", "Save sent")
            com.streampanel.core.model.QuickActionKeys.SEARCH -> sendHotkey("CTRL+F", "Search sent")
            com.streampanel.core.model.QuickActionKeys.NEW_TAB -> sendHotkey("CTRL+T", "New tab sent")
            com.streampanel.core.model.QuickActionKeys.SCREENSHOT -> systemAction("screenshot", "Screenshot tool opened")
            com.streampanel.core.model.QuickActionKeys.TASK_MANAGER -> systemAction("task_manager", "Task Manager opened")
            com.streampanel.core.model.QuickActionKeys.SNAP_LEFT -> windowAction("snap_left", "Snapped left")
            com.streampanel.core.model.QuickActionKeys.SNAP_RIGHT -> windowAction("snap_right", "Snapped right")
            com.streampanel.core.model.QuickActionKeys.PLAY_PAUSE -> mediaAction("play_pause")
            com.streampanel.core.model.QuickActionKeys.PREVIOUS_TRACK -> mediaAction("previous")
            com.streampanel.core.model.QuickActionKeys.NEXT_TRACK -> mediaAction("next")
        }
    }

    fun createButton() {
        val page = uiState.value.currentPage ?: return
        val grid = uiState.value.gridLayout
        val occupied = uiState.value.buttons.map { it.row to it.column }.toSet()
        val slot = (0 until grid.rows).firstNotNullOfOrNull { row ->
            (0 until grid.columns).firstOrNull { col -> (row to col) !in occupied }?.let { row to it }
        } ?: (0 to 0)
        viewModelScope.launch {
            val button = repository.createButton(page.id, slot.first, slot.second)
            lastMessage.value = "Button \"${button.title}\" created — long-press to edit"
        }
    }

    fun killActiveProcess() = systemAction("kill_process", "Active process terminated")

    fun selectPage(pageId: String) {
        selectedPageId.value = pageId
    }

    fun press(button: DashboardButton) {
        if (button.isFolder && button.targetPageId != null) {
            selectedPageId.value = button.targetPageId
            return
        }

        viewModelScope.launch {
            executingButtonId.value = button.id
            val allActions = repository.observeActions(button.id).first()
            val updatedButton = if (button.isToggle) {
                val nextState = if (button.state == ButtonState.Active) ButtonState.Idle else ButtonState.Active
                val toggled = button.copy(state = nextState)
                repository.upsertButton(toggled)
                toggled
            } else {
                button
            }
            val phase = if (updatedButton.state == ButtonState.Active) ActionWhen.On else ActionWhen.Off
            val actions = allActions.filter {
                it.whenState == ActionWhen.Always || it.whenState == phase
            }
            actions.filter { it.type == ActionType.NavigatePage }.forEach { action ->
                action.payload["pageId"]?.let { selectedPageId.value = it }
            }
            val report = actionExecutor.execute(actions.filter { it.type != ActionType.NavigatePage })
            lastMessage.value = if (report.ok) "Done: ${button.title}" else report.message
            executingButtonId.value = null
        }
    }

    fun moveButton(button: DashboardButton, rowDelta: Int, columnDelta: Int) {
        if (rowDelta == 0 && columnDelta == 0) return
        viewModelScope.launch {
            val grid = uiState.value.gridLayout
            repository.upsertButton(
                button.copy(
                    row = (button.row + rowDelta).coerceIn(0, grid.rows - 1),
                    column = (button.column + columnDelta).coerceIn(0, grid.columns - 1),
                ),
            )
        }
    }

    fun updateVolumeDraft(value: Float) {
        volumeDraft.value = value.coerceIn(0f, 100f)
    }

    fun applyVolume() {
        runPcAction(
            type = ActionType.VolumeCommand,
            payload = mapOf("action" to "set", "percent" to volumeDraft.value.toInt().toString()),
            successMessage = "Volume ${volumeDraft.value.toInt()}%",
        )
    }

    fun refreshVolume() = refreshPcStatus()

    private fun refreshPcStatus() {
        viewModelScope.launch {
            val settings = preferencesDataSource.serverConnection.first()
            pcStatusClient.fetch(settings.host, settings.port)?.let { status ->
                status.volumePercent?.toFloat()?.let { volumeDraft.value = it }
                status.muted?.let { isMuted.value = it }
                status.micMuted?.let { isMicMuted.value = it }
                nowPlaying.value = when {
                    !status.nowPlayingTitle.isNullOrBlank() && !status.nowPlayingArtist.isNullOrBlank() ->
                        "${status.nowPlayingTitle} — ${status.nowPlayingArtist}"
                    !status.nowPlayingTitle.isNullOrBlank() -> status.nowPlayingTitle
                    else -> null
                }
                status.cpuPercent?.toFloat()?.let { cpuPercent.value = it }
                status.ramPercent?.toFloat()?.let { ramPercent.value = it }
                foregroundProcess.value = status.foregroundProcess
                clipboardPreview.value = status.clipboardPreview
                status.mediaPlaying?.let { isPlaying.value = it }
                    ?: run {
                        if (!status.nowPlayingTitle.isNullOrBlank()) isPlaying.value = true
                    }
                status.downloadMbps?.toFloat()?.let { downloadMbps.value = it }
                status.uploadMbps?.toFloat()?.let { uploadMbps.value = it }
                status.diskFreePercent?.toFloat()?.let { diskFreePercent.value = it }
                storageDrives.value = status.storageDrives
                networkInterface.value = status.networkInterface
                topProcesses.value = status.topProcesses
                val overlaySettings = preferencesDataSource.gameOverlaySettings.first()
                gameInfo.value = resolveGameInfo(status, overlaySettings)
                applyFocusTracking(status.foregroundProcess)
            } ?: run {
                val report = executePc(ActionType.VolumeCommand, mapOf("action" to "get"))
                Regex("volume:(\\d+)").find(report.message)?.groupValues?.getOrNull(1)?.toFloatOrNull()?.let {
                    volumeDraft.value = it
                }
            }
        }
    }

    fun mediaAction(action: String) {
        viewModelScope.launch {
            runPcAction(
                type = ActionType.MediaCommand,
                payload = mapOf("action" to action),
                successMessage = "Media: $action",
            )
            delay(400)
            refreshPcStatus()
        }
    }

    fun startPomodoro() {
        pomodoroRemaining.value = uiState.value.pomodoroFocusMinutes.coerceIn(1, 240) * 60
        pomodoroRunning.value = true
        systemAction("focus_mode_on", "Focus mode enabled")
        lastMessage.value = "Pomodoro started"
    }

    fun setPomodoroFocusMinutes(minutes: Int) {
        viewModelScope.launch {
            val value = minutes.coerceIn(1, 240)
            preferencesDataSource.setPomodoroFocusMinutes(value)
            if (!pomodoroRunning.value) {
                pomodoroRemaining.value = value * 60
            }
        }
    }

    fun stopPomodoro() {
        pomodoroRunning.value = false
        systemAction("focus_mode_off", "Focus mode disabled")
    }

    fun startTimeTracker(projectId: String) {
        activeTimeProjectId.value = projectId
        timeTrackerStartedAt.value = System.currentTimeMillis()
        timeTrackerElapsed.value = 0
    }

    fun stopTimeTracker() {
        val projectId = activeTimeProjectId.value ?: return
        val project = timeTrackerProjects.value.firstOrNull { it.id == projectId } ?: return
        val started = timeTrackerStartedAt.value ?: return
        val ended = System.currentTimeMillis()
        val minutes = ((ended - started) / 60_000).toInt().coerceAtLeast(1)
        viewModelScope.launch {
            preferencesDataSource.appendTimeLogEntry(
                TimeLogEntry(projectId, project.name, started, ended, minutes),
            )
            lastMessage.value = "${project.name}: ${minutes}m logged"
        }
        activeTimeProjectId.value = null
        timeTrackerStartedAt.value = null
        timeTrackerElapsed.value = 0
    }

    fun killProcessByPid(pid: Int) = systemAction("kill_process_pid", "Process $pid killed", mapOf("pid" to pid.toString()))

    fun cleanTemp() = systemAction("clean_temp", "Temp folders cleaned")

    fun openDiscord() = launchApp(QuickLaunchApp("discord", "Discord", "%LOCALAPPDATA%\\Discord\\Update.exe --processStart Discord.exe", "chat"))

    fun discordMute() = systemAction("discord_mute", "Discord mute")
    fun discordDeafen() = systemAction("discord_deafen", "Discord deafen")
    fun discordPushToTalk() = systemAction("discord_ptt", "Discord PTT")

    fun gitPull() = systemAction("git_pull", "Git pull sent")
    fun gitStatus() = systemAction("git_status", "Git status sent")
    fun dockerPs() = systemAction("docker_ps", "Docker ps sent")
    fun dockerUp() = systemAction("docker_compose_up", "Docker compose up sent")
    fun vscodeRunTests() = sendHotkey("CTRL+SHIFT+T", "Run tests")
    fun vscodeFormat() = sendHotkey("SHIFT+ALT+F", "Format document")
    fun vscodeTerminal() = sendHotkey("CTRL+`", "Terminal toggled")
    fun restartWifi() = systemAction("wifi_restart", "Wi-Fi restarted")
    fun flushDns() = systemAction("flush_dns", "DNS cache flushed")
    fun emptyRecycleBin() = systemAction("empty_recycle_bin", "Recycle bin emptied")

    fun toggleMicMute() = volumeAction("mic_mute_toggle", "Microphone toggled")

    fun switchToSpeakers() = volumeAction("switch_output", "Speakers", mapOf("device" to "speakers"))

    fun switchToHeadphones() = volumeAction("switch_output", "Headphones", mapOf("device" to "headphones"))

    fun windowAction(action: String, label: String, extra: Map<String, String> = emptyMap()) {
        val payload = buildMap {
            put("action", action)
            putAll(extra)
        }
        runPcAction(ActionType.WindowCommand, payload, label)
    }

    fun moveToMonitor(monitor: Int) =
        windowAction("move_monitor", "Moved to monitor $monitor", mapOf("monitor" to monitor.toString()))

    fun sendHotkey(keys: String, label: String) =
        runPcAction(ActionType.Hotkey, mapOf("keys" to keys), label)

    fun systemAction(name: String, label: String, extra: Map<String, String> = emptyMap()) {
        runPcAction(
            ActionType.SystemCommand,
            buildMap {
                put("name", name)
                putAll(extra)
            },
            label,
        )
    }

    fun requestSleepConfirm() {
        showSleepConfirm.value = true
    }

    fun dismissSleepConfirm() {
        showSleepConfirm.value = false
    }

    fun confirmSleep() {
        showSleepConfirm.value = false
        systemAction("sleep", "PC is going to sleep")
    }

    fun lockPc() = systemAction("lock", "PC locked")

    fun pasteClipboard() = runPcAction(
        type = ActionType.Hotkey,
        payload = mapOf("keys" to "CTRL+V"),
        successMessage = "Paste sent",
    )

    fun pasteFromPcClipboard() {
        viewModelScope.launch {
            val report = executePc(ActionType.SystemCommand, mapOf("name" to "get_clipboard"))
            val text = Regex("clipboard:(.*)", RegexOption.DOT_MATCHES_ALL).find(report.message)?.groupValues?.getOrNull(1)
            if (!text.isNullOrBlank()) {
                clipboardPreview.value = text.take(120)
                lastMessage.value = text.take(80)
            } else {
                lastMessage.value = report.message
            }
        }
    }

    private suspend fun applyFocusTracking(processName: String?) {
        if (processName.isNullOrBlank()) return
        val enabled = preferencesDataSource.focusTrackingEnabled.first()
        if (!enabled) return
        val rules = preferencesDataSource.focusProfileRules.first()
        val match = rules.firstOrNull { rule ->
            rule.enabled && processName.contains(rule.processPattern, ignoreCase = true)
        } ?: return
        if (selectedPageId.value != match.pageId) {
            selectedPageId.value = match.pageId
        }
    }

    fun showDesktop() = windowAction("minimize_all", "Desktop shown")

    fun launchApp(app: QuickLaunchApp) {
        runPcAction(
            type = ActionType.LaunchProcess,
            payload = mapOf("path" to app.path),
            successMessage = "Launched ${app.name}",
        )
    }

    fun openFolder(folder: QuickOpenFolder) {
        runPcAction(
            type = ActionType.OpenFolder,
            payload = mapOf("path" to folder.path),
            successMessage = "Opened ${folder.name}",
        )
    }

    fun openUrlGroup(group: QuickOpenUrlGroup) {
        runPcAction(
            type = ActionType.OpenUrlGroup,
            payload = mapOf("urls" to group.urls),
            successMessage = "Opened ${group.name}",
        )
    }

    private fun volumeAction(action: String, successMessage: String, extra: Map<String, String> = emptyMap()) {
        runPcAction(
            type = ActionType.VolumeCommand,
            payload = buildMap {
                put("action", action)
                putAll(extra)
            },
            successMessage = successMessage,
        )
    }

    private fun runPcAction(
        type: ActionType,
        payload: Map<String, String>,
        successMessage: String,
    ) {
        viewModelScope.launch {
            val report = executePc(type, payload)
            lastMessage.value = if (report.ok) successMessage else report.message
        }
    }

    private suspend fun executePc(type: ActionType, payload: Map<String, String>) =
        actionExecutor.execute(
            listOf(
                ControlAction(
                    id = UUID.randomUUID().toString(),
                    buttonId = "system-panel",
                    type = type,
                    label = type.name,
                    payload = payload,
                ),
            ),
        )

    private fun resolveGameInfo(status: PcServerStatus, settings: GameOverlaySettings): GameTelemetryInfo {
        val serverInfo = status.gameInfo
        if (!settings.enabled || serverInfo.detected) return serverInfo

        val process = status.foregroundProcess.orEmpty()
        val title = status.foregroundTitle.orEmpty()
        val match = settings.autoShowProcessPatterns
            .split(';', ',', '\n', '|')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .firstOrNull { pattern ->
                process.contains(pattern, ignoreCase = true) ||
                    title.contains(pattern, ignoreCase = true) ||
                    process.removeSuffix(".exe").equals(pattern.removeSuffix(".exe"), ignoreCase = true)
            }

        return if (match != null) {
            GameTelemetryInfo(
                detected = true,
                provider = "Custom game",
                processName = status.foregroundProcess,
                windowTitle = status.foregroundTitle,
                note = "Matched '$match'. Detailed HP/map/ammo requires a game telemetry integration.",
            )
        } else {
            serverInfo
        }
    }
}

private data class DeckSnapshot(
    val profile: ControlProfile?,
    val pages: List<ControlPage>,
    val currentPage: ControlPage?,
    val buttons: List<DashboardButton>,
)

private data class PrefsSnapshot(
    val defaultGrid: GridLayout,
    val pageGrids: Map<String, GridLayout>,
    val apps: List<QuickLaunchApp>,
    val folders: List<QuickOpenFolder>,
    val urlGroups: List<QuickOpenUrlGroup>,
    val quickActions: List<QuickActionItem>,
    val layout: DashboardLayoutSettings,
    val pomodoroFocusMinutes: Int,
    val streamChatSettings: StreamChatSettings,
    val gameOverlaySettings: GameOverlaySettings,
    val server: ServerConnectionSettings,
)

private data class RuntimeSnapshot(
    val status: ConnectionStatus,
    val executingId: String?,
    val message: String?,
    val volume: Float,
    val sleepConfirm: Boolean,
    val layerGridDialog: Boolean,
    val actionDialog: DashboardButton?,
)

private data class AudioSnapshot(
    val muted: Boolean,
    val micMuted: Boolean,
    val playing: Boolean,
    val track: String?,
)

private data class PcStatsSnapshot(
    val cpu: Float?,
    val ram: Float?,
    val foreground: String?,
    val clipboard: String?,
)

data class DashboardUiState(
    val profile: ControlProfile? = null,
    val pages: List<ControlPage> = emptyList(),
    val currentPage: ControlPage? = null,
    val buttons: List<DashboardButton> = emptyList(),
    val gridLayout: GridLayout = GridLayout(),
    val quickLaunchApps: List<QuickLaunchApp> = emptyList(),
    val quickOpenFolders: List<QuickOpenFolder> = emptyList(),
    val quickOpenUrlGroups: List<QuickOpenUrlGroup> = emptyList(),
    val quickActions: List<QuickActionItem> = emptyList(),
    val layout: DashboardLayoutSettings = DashboardLayoutSettings(),
    val streamChatSettings: StreamChatSettings = StreamChatSettings(),
    val gameOverlaySettings: GameOverlaySettings = GameOverlaySettings(),
    val pcConfiguratorUrl: String? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val executingButtonId: String? = null,
    val lastMessage: String? = null,
    val volumeLevel: Float = 50f,
    val showSleepConfirm: Boolean = false,
    val showLayerGridDialog: Boolean = false,
    val buttonActionDialog: DashboardButton? = null,
    val isMuted: Boolean = false,
    val isMicMuted: Boolean = false,
    val isPlaying: Boolean = false,
    val nowPlaying: String? = null,
    val cpuPercent: Float? = null,
    val ramPercent: Float? = null,
    val foregroundProcess: String? = null,
    val clipboardPreview: String? = null,
    val downloadMbps: Float? = null,
    val uploadMbps: Float? = null,
    val diskFreePercent: Float? = null,
    val storageDrives: List<PcStorageDrive> = emptyList(),
    val networkInterface: String? = null,
    val topProcesses: List<PcProcessInfo> = emptyList(),
    val gameInfo: GameTelemetryInfo = GameTelemetryInfo(),
    val pomodoroRemainingSeconds: Int = 25 * 60,
    val pomodoroRunning: Boolean = false,
    val pomodoroFocusMinutes: Int = 25,
    val timeTrackerProjects: List<TimeTrackerProject> = emptyList(),
    val activeTimeProjectId: String? = null,
    val timeTrackerElapsedSeconds: Int = 0,
)

private data class NetworkSnapshot(
    val download: Float?,
    val upload: Float?,
    val disk: Float?,
    val nic: String?,
    val processes: List<PcProcessInfo>,
    val storageDrives: List<PcStorageDrive>,
)

private data class PomodoroSnapshot(
    val remainingSeconds: Int,
    val running: Boolean,
)

private data class TimeTrackerSnapshot(
    val projects: List<TimeTrackerProject>,
    val activeId: String?,
    val elapsed: Int,
)

private data class ExtendedSnapshot(
    val pc: PcStatsSnapshot,
    val network: NetworkSnapshot,
    val pomodoro: PomodoroSnapshot,
    val tracker: TimeTrackerSnapshot,
    val game: GameTelemetryInfo,
)
