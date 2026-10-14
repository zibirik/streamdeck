package com.streampanel.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streampanel.core.database.ControlPanelRepository
import com.streampanel.core.datastore.PreferencesDataSource
import com.streampanel.core.model.AppLanguage
import com.streampanel.core.model.AppearanceSettings
import com.streampanel.core.model.DeckExport
import com.streampanel.core.model.GameOverlaySettings
import com.streampanel.core.model.GridLayout
import com.streampanel.core.model.QuickActionItem
import com.streampanel.core.model.QuickLaunchApp
import com.streampanel.core.model.QuickOpenFolder
import com.streampanel.core.model.QuickOpenUrlGroup
import com.streampanel.core.model.ServerConnectionSettings
import com.streampanel.core.model.StreamChatSettings
import com.streampanel.core.model.ThemeMode
import com.streampanel.core.model.TimeLogEntry
import com.streampanel.core.model.TimeTrackerProject
import com.streampanel.core.network.ConnectionStatus
import com.streampanel.core.network.DiscoveredServer
import com.streampanel.core.network.LanServerDiscovery
import com.streampanel.core.network.PcConnectionClient
import com.streampanel.core.network.PcConnectionManager
import com.streampanel.core.network.PcStatusClient
import com.streampanel.core.network.Cs2InstallOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

/** What happened, without any text: the screen turns it into the user's language. */
enum class SettingsMessageKind {
    ConnectionSaved,
    Connecting,
    DeckExported,
    DeckImported,
    ImportFailed,
    NothingToExport,
    NoTimeEntries,
    TimeCsvReady,
    Cs2Installed,
    Cs2NotFound,
    Cs2WrongPin,
    Cs2Failed,
}

data class SettingsMessage(val kind: SettingsMessageKind, val detail: String? = null)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource,
    private val repository: ControlPanelRepository,
    private val connectionManager: PcConnectionManager,
    private val pcConnectionClient: PcConnectionClient,
    private val lanServerDiscovery: LanServerDiscovery,
    private val pcStatusClient: PcStatusClient,
    private val json: Json,
) : ViewModel() {
    private val exportJson = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<SettingsMessage?>(null)
    private val serverHostDraft = MutableStateFlow<String?>(null)
    private val serverPortDraft = MutableStateFlow<String?>(null)
    private val serverPinDraft = MutableStateFlow<String?>(null)
    private val discovered = MutableStateFlow<List<DiscoveredServer>>(emptyList())
    private val scanning = MutableStateFlow(false)
    private val scanFinished = MutableStateFlow(false)

    val uiState = combine(
        combine(
            combine(
                preferencesDataSource.appearance,
                preferencesDataSource.gridLayout,
                preferencesDataSource.quickLaunchApps,
                preferencesDataSource.pomodoroFocusMinutes,
            ) { appearance, grid, apps, pomodoroMinutes ->
                SettingsAppearanceBundle(appearance, grid, apps, pomodoroMinutes)
            },
            preferencesDataSource.streamChatSettings,
            preferencesDataSource.gameOverlaySettings,
        ) { appearanceBundle, streamChat, gameOverlay ->
            appearanceBundle.copy(streamChatSettings = streamChat, gameOverlaySettings = gameOverlay)
        },
        combine(
            combine(
                preferencesDataSource.quickOpenFolders,
                preferencesDataSource.quickOpenUrlGroups,
                preferencesDataSource.serverConnection,
            ) { folders, urlGroups, server -> Triple(folders, urlGroups, server) },
            combine(
                preferencesDataSource.quickActions,
                preferencesDataSource.appLanguage,
                preferencesDataSource.focusTrackingEnabled,
            ) { quickActions, language, focusTracking -> Triple(quickActions, language, focusTracking) },
            combine(
                preferencesDataSource.timeTrackerProjects,
                preferencesDataSource.timeLogEntries,
            ) { trackerProjects, timeLogs -> trackerProjects to timeLogs },
        ) { (folders, urlGroups, server), (quickActions, language, focusTracking), (trackerProjects, timeLogs) ->
            SettingsPrefsBundle(folders, urlGroups, server, quickActions, language, focusTracking, trackerProjects, timeLogs)
        },
    ) { appearanceBundle, prefs ->
        SettingsUiState(
            appearanceBundle.appearance,
            appearanceBundle.grid,
            appearanceBundle.apps,
            prefs.folders,
            prefs.urlGroups,
            prefs.quickActions,
            prefs.server,
            prefs.language, prefs.focusTracking, prefs.timeTrackerProjects, prefs.timeLogEntries,
            pomodoroFocusMinutes = appearanceBundle.pomodoroMinutes,
            streamChatSettings = appearanceBundle.streamChatSettings,
            gameOverlaySettings = appearanceBundle.gameOverlaySettings,
        )
    }.combine(
        combine(serverHostDraft, serverPortDraft, serverPinDraft) { host, port, pin ->
            Triple(host, port, pin)
        },
    ) { base, (host, port, pin) ->
        base.copy(
            serverHostDraft = host ?: base.server.host,
            serverPortDraft = port ?: base.server.port.toString(),
            serverPinDraft = pin ?: base.server.pin,
        )
    }.combine(exportJson) { base, export -> base.copy(exportJson = export) }
        .combine(message) { base, msg -> base.copy(message = msg) }
        .combine(pcConnectionClient.status) { base, status -> base.copy(connectionStatus = status) }
        .combine(
            combine(discovered, scanning, scanFinished) { found, busy, done -> Triple(found, busy, done) },
        ) { base, (found, busy, done) ->
            base.copy(discoveredServers = found, scanning = busy, scanFinished = done)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsUiState(),
        )

    fun setLanguage(language: AppLanguage) = viewModelScope.launch {
        preferencesDataSource.setAppLanguage(language)
    }

    fun setFocusTracking(enabled: Boolean) = viewModelScope.launch {
        preferencesDataSource.setFocusTrackingEnabled(enabled)
    }

    fun setTheme(themeMode: ThemeMode) = viewModelScope.launch {
        preferencesDataSource.setThemeMode(themeMode)
    }

    fun setAccent(color: String) = viewModelScope.launch {
        preferencesDataSource.setAccentColor(color)
    }

    fun setGlass(enabled: Boolean) = viewModelScope.launch {
        preferencesDataSource.setGlassEnabled(enabled)
    }

    fun setPomodoroFocusMinutes(value: Int) = viewModelScope.launch {
        preferencesDataSource.setPomodoroFocusMinutes(value)
    }

    fun setStreamChatSettings(settings: StreamChatSettings) = viewModelScope.launch {
        preferencesDataSource.setStreamChatSettings(settings)
    }

    fun setGameOverlaySettings(settings: GameOverlaySettings) = viewModelScope.launch {
        preferencesDataSource.setGameOverlaySettings(settings)
    }

    /** Sets rows and columns in one write, so neither value is lost to a stale read. */
    fun setGrid(rows: Int, columns: Int) = viewModelScope.launch {
        preferencesDataSource.setGridLayout(GridLayout(rows.coerceIn(1, 12), columns.coerceIn(1, 12)))
    }

    fun setServerHost(host: String) {
        serverHostDraft.value = host
    }

    fun setServerPort(port: String) {
        serverPortDraft.value = port.filter { it.isDigit() }
    }

    fun setAutoConnect(enabled: Boolean) = viewModelScope.launch {
        preferencesDataSource.setServerConnection(uiState.value.server.copy(autoConnect = enabled))
    }

    fun setServerPin(pin: String) {
        serverPinDraft.value = pin
    }

    fun saveServerConnection(connectAfterSave: Boolean = false) = viewModelScope.launch {
        val settings = ServerConnectionSettings(
            host = normalizeHost(uiState.value.serverHostDraft),
            port = uiState.value.serverPortDraft.toIntOrNull()?.coerceIn(1, 65535) ?: 17820,
            autoConnect = uiState.value.server.autoConnect,
            pin = uiState.value.serverPinDraft,
        )
        preferencesDataSource.setServerConnection(settings)
        serverHostDraft.value = null
        serverPortDraft.value = null
        serverPinDraft.value = null
        if (connectAfterSave) {
            message.value = SettingsMessage(SettingsMessageKind.Connecting, settings.host)
            connectionManager.connect(settings)
        } else {
            message.value = SettingsMessage(SettingsMessageKind.ConnectionSaved)
        }
    }

    fun disconnect() = viewModelScope.launch {
        connectionManager.disconnect()
    }

    /** Looks for StreamPanel servers on the Wi-Fi network the tablet is connected to. */
    fun scanNetwork() = viewModelScope.launch {
        if (scanning.value) return@launch
        scanning.value = true
        scanFinished.value = false
        discovered.value = runCatching {
            lanServerDiscovery.discover(uiState.value.serverPortDraft.toIntOrNull() ?: 17820)
        }.getOrDefault(emptyList())
        scanning.value = false
        scanFinished.value = true
    }

    /** Tapping a found PC only picks it; the big "Connect" button then uses it together with the PIN. */
    fun selectDiscovered(server: DiscoveredServer) {
        serverHostDraft.value = server.host
        serverPortDraft.value = server.port.toString()
    }
    /** Asks the PC to put the Counter-Strike 2 config file where the game reads it. */
    fun installCs2() = viewModelScope.launch {
        val server = uiState.value.server
        message.value = when (val outcome = pcStatusClient.installCs2(server.host, server.port, server.pin)) {
            Cs2InstallOutcome.Installed -> SettingsMessage(SettingsMessageKind.Cs2Installed)
            Cs2InstallOutcome.GameNotFound -> SettingsMessage(SettingsMessageKind.Cs2NotFound)
            Cs2InstallOutcome.WrongPin -> SettingsMessage(SettingsMessageKind.Cs2WrongPin)
            is Cs2InstallOutcome.Failed -> SettingsMessage(SettingsMessageKind.Cs2Failed, outcome.reason)
        }
    }

    fun setKeepScreenOn(enabled: Boolean) = viewModelScope.launch {
        preferencesDataSource.setKeepScreenOn(enabled)
    }

    private fun normalizeHost(raw: String): String {
        val host = raw
            .trim()
            .removePrefix("ws://")
            .removePrefix("wss://")
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore("/")
            .substringBefore(":")
        return host.ifBlank { ServerConnectionSettings().host }
    }

    fun setBackgroundImageUrl(url: String) = viewModelScope.launch {
        preferencesDataSource.setBackgroundImageUrl(url)
    }

    fun exportDeck() = viewModelScope.launch {
        val export = repository.exportDeck()
        if (export == null) {
            message.value = SettingsMessage(SettingsMessageKind.NothingToExport)
        } else {
            exportJson.value = json.encodeToString(DeckExport.serializer(), export)
            message.value = SettingsMessage(SettingsMessageKind.DeckExported)
        }
    }

    fun importDeck(raw: String) = viewModelScope.launch {
        runCatching {
            val export = json.decodeFromString(DeckExport.serializer(), raw)
            repository.importDeck(export)
            message.value = SettingsMessage(SettingsMessageKind.DeckImported)
        }.onFailure {
            message.value = SettingsMessage(SettingsMessageKind.ImportFailed, it.message)
        }
    }

    fun clearExport() {
        exportJson.value = null
    }

    fun clearMessage() {
        message.value = null
    }

    fun updateQuickLaunchApp(
        appId: String,
        name: String? = null,
        path: String? = null,
        enabled: Boolean? = null,
    ) = viewModelScope.launch {
        val updated = uiState.value.quickLaunchApps.map { app ->
            if (app.id != appId) {
                app
            } else {
                app.copy(
                    name = name ?: app.name,
                    path = path ?: app.path,
                    enabled = enabled ?: app.enabled,
                )
            }
        }
        preferencesDataSource.setQuickLaunchApps(updated)
    }

    fun addQuickLaunchApp() = viewModelScope.launch {
        preferencesDataSource.setQuickLaunchApps(
            uiState.value.quickLaunchApps + QuickLaunchApp(
                UUID.randomUUID().toString(), "New App", "notepad.exe", "terminal", "#6366F1",
            ),
        )
    }

    fun removeQuickLaunchApp(appId: String) = viewModelScope.launch {
        val apps = uiState.value.quickLaunchApps.filterNot { it.id == appId }
        if (apps.isNotEmpty()) preferencesDataSource.setQuickLaunchApps(apps)
    }

    fun restoreQuickLaunchDefaults() = viewModelScope.launch {
        preferencesDataSource.setQuickLaunchApps(preferencesDataSource.defaultQuickLaunchApps)
    }

    fun updateFolder(id: String, name: String? = null, path: String? = null) = viewModelScope.launch {
        val updated = uiState.value.quickOpenFolders.map { folder ->
            if (folder.id != id) folder else folder.copy(name = name ?: folder.name, path = path ?: folder.path)
        }
        preferencesDataSource.setQuickOpenFolders(updated)
    }

    fun addFolder() = viewModelScope.launch {
        preferencesDataSource.setQuickOpenFolders(
            uiState.value.quickOpenFolders + QuickOpenFolder(
                UUID.randomUUID().toString(), "New Folder", "%USERPROFILE%\\Desktop",
            ),
        )
    }

    fun removeFolder(id: String) = viewModelScope.launch {
        val folders = uiState.value.quickOpenFolders.filterNot { it.id == id }
        if (folders.isNotEmpty()) preferencesDataSource.setQuickOpenFolders(folders)
    }

    fun restoreFolderDefaults() = viewModelScope.launch {
        preferencesDataSource.setQuickOpenFolders(preferencesDataSource.defaultQuickOpenFolders)
    }

    fun updateUrlGroup(id: String, name: String? = null, urls: String? = null) = viewModelScope.launch {
        val updated = uiState.value.quickOpenUrlGroups.map { group ->
            if (group.id != id) group else group.copy(name = name ?: group.name, urls = urls ?: group.urls)
        }
        preferencesDataSource.setQuickOpenUrlGroups(updated)
    }

    fun addUrlGroup() = viewModelScope.launch {
        preferencesDataSource.setQuickOpenUrlGroups(
            uiState.value.quickOpenUrlGroups + QuickOpenUrlGroup(
                UUID.randomUUID().toString(), "New Links", "https://google.com",
            ),
        )
    }

    fun removeUrlGroup(id: String) = viewModelScope.launch {
        val groups = uiState.value.quickOpenUrlGroups.filterNot { it.id == id }
        if (groups.isNotEmpty()) preferencesDataSource.setQuickOpenUrlGroups(groups)
    }

    fun restoreUrlGroupDefaults() = viewModelScope.launch {
        preferencesDataSource.setQuickOpenUrlGroups(preferencesDataSource.defaultQuickOpenUrlGroups)
    }

    fun setQuickActionEnabled(id: String, enabled: Boolean) = viewModelScope.launch {
        val updated = uiState.value.quickActions.map { item ->
            if (item.id == id) item.copy(enabled = enabled) else item
        }
        preferencesDataSource.setQuickActions(updated)
    }

    fun setQuickActionRow(id: String, row: Int) = viewModelScope.launch {
        val updated = uiState.value.quickActions.map { item ->
            if (item.id == id) item.copy(row = row.coerceIn(0, 5)) else item
        }
        preferencesDataSource.setQuickActions(updated)
    }

    fun restoreQuickActionDefaults() = viewModelScope.launch {
        preferencesDataSource.setQuickActions(preferencesDataSource.defaultQuickActions)
    }

    fun addTimeTrackerProject() = viewModelScope.launch {
        preferencesDataSource.setTimeTrackerProjects(
            uiState.value.timeTrackerProjects + TimeTrackerProject(
                UUID.randomUUID().toString(),
                "Project ${uiState.value.timeTrackerProjects.size + 1}",
            ),
        )
    }

    fun updateTimeTrackerProject(
        id: String,
        name: String? = null,
        color: String? = null,
        enabled: Boolean? = null,
    ) = viewModelScope.launch {
        val updated = uiState.value.timeTrackerProjects.map { project ->
            if (project.id != id) {
                project
            } else {
                project.copy(
                    name = name ?: project.name,
                    color = color ?: project.color,
                    enabled = enabled ?: project.enabled,
                )
            }
        }
        preferencesDataSource.setTimeTrackerProjects(updated)
    }

    fun removeTimeTrackerProject(id: String) = viewModelScope.launch {
        val projects = uiState.value.timeTrackerProjects.filterNot { it.id == id }
        if (projects.isNotEmpty()) preferencesDataSource.setTimeTrackerProjects(projects)
    }

    fun restoreTimeTrackerDefaults() = viewModelScope.launch {
        preferencesDataSource.setTimeTrackerProjects(preferencesDataSource.defaultTimeTrackerProjects)
    }

    fun exportTimeLogCsv() = viewModelScope.launch {
        val logs = uiState.value.timeLogEntries
        if (logs.isEmpty()) {
            message.value = SettingsMessage(SettingsMessageKind.NoTimeEntries)
            return@launch
        }
        val header = "project,started,ended,minutes"
        val rows = logs.joinToString("\n") { entry ->
            "${entry.projectName},${entry.startedAtEpochMs},${entry.endedAtEpochMs},${entry.durationMinutes}"
        }
        exportJson.value = "$header\n$rows"
        message.value = SettingsMessage(SettingsMessageKind.TimeCsvReady)
    }
}

private data class SettingsAppearanceBundle(
    val appearance: AppearanceSettings,
    val grid: GridLayout,
    val apps: List<QuickLaunchApp>,
    val pomodoroMinutes: Int,
    val streamChatSettings: StreamChatSettings = StreamChatSettings(),
    val gameOverlaySettings: GameOverlaySettings = GameOverlaySettings(),
)

private data class SettingsPrefsBundle(
    val folders: List<QuickOpenFolder>,
    val urlGroups: List<QuickOpenUrlGroup>,
    val server: ServerConnectionSettings,
    val quickActions: List<QuickActionItem>,
    val language: AppLanguage,
    val focusTracking: Boolean,
    val timeTrackerProjects: List<TimeTrackerProject>,
    val timeLogEntries: List<TimeLogEntry>,
)

data class SettingsUiState(
    val appearance: AppearanceSettings = AppearanceSettings(),
    val grid: GridLayout = GridLayout(),
    val quickLaunchApps: List<QuickLaunchApp> = emptyList(),
    val quickOpenFolders: List<QuickOpenFolder> = emptyList(),
    val quickOpenUrlGroups: List<QuickOpenUrlGroup> = emptyList(),
    val quickActions: List<QuickActionItem> = emptyList(),
    val server: ServerConnectionSettings = ServerConnectionSettings(),
    val language: AppLanguage = AppLanguage.Russian,
    val focusTrackingEnabled: Boolean = false,
    val timeTrackerProjects: List<TimeTrackerProject> = emptyList(),
    val timeLogEntries: List<TimeLogEntry> = emptyList(),
    val pomodoroFocusMinutes: Int = 25,
    val streamChatSettings: StreamChatSettings = StreamChatSettings(),
    val gameOverlaySettings: GameOverlaySettings = GameOverlaySettings(),
    val serverHostDraft: String = ServerConnectionSettings().host,
    val serverPortDraft: String = ServerConnectionSettings().port.toString(),
    val serverPinDraft: String = "",
    val exportJson: String? = null,
    val message: SettingsMessage? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val discoveredServers: List<DiscoveredServer> = emptyList(),
    val scanning: Boolean = false,
    val scanFinished: Boolean = false,
)
