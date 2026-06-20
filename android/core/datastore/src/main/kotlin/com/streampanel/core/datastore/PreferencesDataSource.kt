package com.streampanel.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.streampanel.core.model.AppLanguage
import com.streampanel.core.model.AppearanceSettings
import com.streampanel.core.model.DashboardLayoutSettings
import com.streampanel.core.model.FocusProfileRule
import com.streampanel.core.model.GridLayout
import com.streampanel.core.model.QuickActionItem
import com.streampanel.core.model.QuickActionKeys
import com.streampanel.core.model.QuickLaunchApp
import com.streampanel.core.model.QuickOpenFolder
import com.streampanel.core.model.QuickOpenUrlGroup
import com.streampanel.core.model.GameOverlaySettings
import com.streampanel.core.model.TimeLogEntry
import com.streampanel.core.model.TimeTrackerProject
import com.streampanel.core.model.ServerConnectionSettings
import com.streampanel.core.model.StreamChatSettings
import com.streampanel.core.model.ThemeMode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.streamPanelDataStore by preferencesDataStore(name = "stream_panel_preferences")

@Singleton
class PreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore: DataStore<Preferences> = context.streamPanelDataStore
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val safeData: Flow<Preferences> = dataStore.data.catch { emit(emptyPreferences()) }

    val defaultQuickOpenFolders = listOf(
        QuickOpenFolder("desktop", "Desktop", "%USERPROFILE%\\Desktop"),
        QuickOpenFolder("downloads", "Downloads", "%USERPROFILE%\\Downloads"),
        QuickOpenFolder("videos", "Videos", "%USERPROFILE%\\Videos"),
        QuickOpenFolder("projects", "Projects", "%USERPROFILE%\\Documents"),
    )

    val defaultQuickOpenUrlGroups = listOf(
        QuickOpenUrlGroup(
            id = "stream-kit",
            name = "Stream Kit",
            urls = "https://studio.youtube.com|https://dashboard.twitch.tv|https://chat.openai.com",
        ),
        QuickOpenUrlGroup(
            id = "social",
            name = "Social",
            urls = "https://discord.com/channels/@me|https://web.whatsapp.com",
        ),
    )

    val defaultQuickActions = listOf(
        QuickActionItem("lock", QuickActionKeys.LOCK, row = 0, sortOrder = 0),
        QuickActionItem("paste", QuickActionKeys.PASTE, row = 0, sortOrder = 1),
        QuickActionItem("desktop", QuickActionKeys.DESKTOP, row = 0, sortOrder = 2),
        QuickActionItem("sync_volume", QuickActionKeys.SYNC_VOLUME, row = 0, sortOrder = 3),
        QuickActionItem("copy", QuickActionKeys.COPY, row = 1, sortOrder = 0),
        QuickActionItem("alt_tab", QuickActionKeys.ALT_TAB, row = 1, sortOrder = 1),
        QuickActionItem("close_window", QuickActionKeys.CLOSE_WINDOW, row = 1, sortOrder = 2),
        QuickActionItem("fullscreen", QuickActionKeys.FULLSCREEN, row = 1, sortOrder = 3),
        QuickActionItem("refresh", QuickActionKeys.REFRESH, row = 2, sortOrder = 0),
        QuickActionItem("undo", QuickActionKeys.UNDO, row = 2, sortOrder = 1),
        QuickActionItem("cut", QuickActionKeys.CUT, row = 2, sortOrder = 2),
        QuickActionItem("redo", QuickActionKeys.REDO, row = 2, sortOrder = 3),
        QuickActionItem("save", QuickActionKeys.SAVE, row = 3, sortOrder = 0),
        QuickActionItem("search", QuickActionKeys.SEARCH, row = 3, sortOrder = 1),
        QuickActionItem("new_tab", QuickActionKeys.NEW_TAB, row = 3, sortOrder = 2),
        QuickActionItem("screenshot", QuickActionKeys.SCREENSHOT, row = 3, sortOrder = 3),
        QuickActionItem("task_manager", QuickActionKeys.TASK_MANAGER, row = 4, sortOrder = 0),
        QuickActionItem("snap_left", QuickActionKeys.SNAP_LEFT, row = 4, sortOrder = 1),
        QuickActionItem("snap_right", QuickActionKeys.SNAP_RIGHT, row = 4, sortOrder = 2),
        QuickActionItem("play_pause", QuickActionKeys.PLAY_PAUSE, row = 4, sortOrder = 3),
        QuickActionItem("previous_track", QuickActionKeys.PREVIOUS_TRACK, enabled = false, row = 5, sortOrder = 0),
        QuickActionItem("next_track", QuickActionKeys.NEXT_TRACK, enabled = false, row = 5, sortOrder = 1),
    )

    val defaultQuickLaunchApps = listOf(
        QuickLaunchApp("chrome", "Chrome", "chrome.exe", "public", "#4285F4"),
        QuickLaunchApp("discord", "Discord", "%LOCALAPPDATA%\\Discord\\Update.exe --processStart Discord.exe", "chat", "#5865F2"),
        QuickLaunchApp("spotify", "Spotify", "spotify.exe", "music_note", "#1DB954"),
        QuickLaunchApp("obs", "OBS", "obs64.exe", "live_tv", "#9B8CFF"),
        QuickLaunchApp("steam", "Steam", "steam://open/main", "sports_esports", "#1B2838"),
        QuickLaunchApp("explorer", "Explorer", "explorer.exe", "folder", "#38BDF8"),
    )

    val appLanguage: Flow<AppLanguage> = safeData.map { prefs ->
        val raw = prefs[Keys.appLanguage]
        raw?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.Russian
    }

    val focusProfileRules: Flow<List<FocusProfileRule>> = safeData.map { prefs ->
        prefs[Keys.focusProfileRules]?.let { raw ->
            runCatching { json.decodeFromString<List<FocusProfileRule>>(raw) }.getOrNull()
        } ?: emptyList()
    }

    val focusTrackingEnabled: Flow<Boolean> = safeData.map { prefs ->
        prefs[Keys.focusTrackingEnabled] ?: false
    }

    val obsSettings: Flow<ObsConnectionSettings> = safeData.map { prefs ->
        ObsConnectionSettings(
            url = prefs[Keys.obsUrl] ?: "ws://127.0.0.1:4455",
            password = prefs[Keys.obsPassword] ?: "",
        )
    }

    val appearance: Flow<AppearanceSettings> = safeData.map { prefs ->
        val themeRaw = prefs[Keys.themeMode]
        val themeMode = themeRaw?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.Dark
        AppearanceSettings(
            themeMode = themeMode,
            accentColor = prefs[Keys.accentColor] ?: "#8B5CF6",
            enableGlass = prefs[Keys.enableGlass] ?: true,
            cornerRadius = prefs[Keys.cornerRadius] ?: 28,
            keepScreenOn = prefs[Keys.keepScreenOn] ?: true,
            backgroundImageUrl = prefs[Keys.backgroundImageUrl] ?: "",
        )
    }

    val timeTrackerProjects: Flow<List<TimeTrackerProject>> = safeData.map { prefs ->
        prefs[Keys.timeTrackerProjects]?.let { raw ->
            runCatching { json.decodeFromString<List<TimeTrackerProject>>(raw) }.getOrNull()
        } ?: defaultTimeTrackerProjects
    }

    val timeLogEntries: Flow<List<TimeLogEntry>> = safeData.map { prefs ->
        prefs[Keys.timeLogEntries]?.let { raw ->
            runCatching { json.decodeFromString<List<TimeLogEntry>>(raw) }.getOrNull()
        } ?: emptyList()
    }

    val defaultTimeTrackerProjects = listOf(
        TimeTrackerProject("work", "Work", "#8B5CF6"),
        TimeTrackerProject("stream", "Stream", "#EF4444"),
        TimeTrackerProject("routine", "Routine", "#38BDF8"),
    )

    val dashboardLayout: Flow<DashboardLayoutSettings> = safeData.map { prefs ->
        prefs[Keys.dashboardLayout]?.let { raw ->
            runCatching { json.decodeFromString<DashboardLayoutSettings>(raw) }.getOrNull()
        }.let { DashboardLayoutSettings.mergeWithDefaults(it) }
    }

    val gridLayout: Flow<GridLayout> = safeData.map { prefs ->
        GridLayout(
            rows = (prefs[Keys.gridRows] ?: 4).coerceIn(1, 12),
            columns = (prefs[Keys.gridColumns] ?: 4).coerceIn(1, 12),
        )
    }

    val quickLaunchApps: Flow<List<QuickLaunchApp>> = safeData.map { prefs ->
        prefs[Keys.quickLaunchApps]?.let { raw ->
            runCatching { json.decodeFromString<List<QuickLaunchApp>>(raw) }.getOrNull()
        } ?: defaultQuickLaunchApps
    }

    val quickOpenFolders: Flow<List<QuickOpenFolder>> = safeData.map { prefs ->
        prefs[Keys.quickOpenFolders]?.let { raw ->
            runCatching { json.decodeFromString<List<QuickOpenFolder>>(raw) }.getOrNull()
        } ?: defaultQuickOpenFolders
    }

    val quickOpenUrlGroups: Flow<List<QuickOpenUrlGroup>> = safeData.map { prefs ->
        prefs[Keys.quickOpenUrlGroups]?.let { raw ->
            runCatching { json.decodeFromString<List<QuickOpenUrlGroup>>(raw) }.getOrNull()
        } ?: defaultQuickOpenUrlGroups
    }

    val quickActions: Flow<List<QuickActionItem>> = safeData.map { prefs ->
        val saved = prefs[Keys.quickActions]?.let { raw ->
            runCatching { json.decodeFromString<List<QuickActionItem>>(raw) }.getOrNull()
        }
        mergeQuickActionsWithDefaults(saved)
    }

    val pageGridLayouts: Flow<Map<String, GridLayout>> = safeData.map { prefs ->
        prefs[Keys.pageGridLayouts]?.let { raw ->
            runCatching { json.decodeFromString<Map<String, GridLayout>>(raw) }.getOrNull()
        } ?: emptyMap()
    }

    val pomodoroFocusMinutes: Flow<Int> = safeData.map { prefs ->
        (prefs[Keys.pomodoroFocusMinutes] ?: 25).coerceIn(1, 240)
    }

    val streamChatSettings: Flow<StreamChatSettings> = safeData.map { prefs ->
        prefs[Keys.streamChatSettings]?.let { raw ->
            runCatching { json.decodeFromString<StreamChatSettings>(raw) }.getOrNull()
        } ?: StreamChatSettings()
    }

    val gameOverlaySettings: Flow<GameOverlaySettings> = safeData.map { prefs ->
        prefs[Keys.gameOverlaySettings]?.let { raw ->
            runCatching { json.decodeFromString<GameOverlaySettings>(raw) }.getOrNull()
        } ?: GameOverlaySettings()
    }

    val serverConnection: Flow<ServerConnectionSettings> = safeData.map { prefs ->
        ServerConnectionSettings(
            host = prefs[Keys.serverHost] ?: "192.168.1.10",
            port = prefs[Keys.serverPort] ?: 17820,
            autoConnect = prefs[Keys.autoConnect] ?: true,
            pin = prefs[Keys.serverPin] ?: "",
        )
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        dataStore.edit { it[Keys.appLanguage] = language.name }
    }

    suspend fun setFocusTrackingEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.focusTrackingEnabled] = enabled }
    }

    suspend fun setFocusProfileRules(rules: List<FocusProfileRule>) {
        dataStore.edit { it[Keys.focusProfileRules] = json.encodeToString(rules) }
    }

    suspend fun setObsSettings(settings: ObsConnectionSettings) {
        dataStore.edit {
            it[Keys.obsUrl] = settings.url
            it[Keys.obsPassword] = settings.password
        }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { it[Keys.themeMode] = themeMode.name }
    }

    suspend fun setAccentColor(color: String) {
        dataStore.edit { it[Keys.accentColor] = color }
    }

    suspend fun setGlassEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.enableGlass] = enabled }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { it[Keys.keepScreenOn] = enabled }
    }

    suspend fun setBackgroundImageUrl(url: String) {
        dataStore.edit { it[Keys.backgroundImageUrl] = url }
    }

    suspend fun setTimeTrackerProjects(projects: List<TimeTrackerProject>) {
        dataStore.edit { it[Keys.timeTrackerProjects] = json.encodeToString(projects) }
    }

    suspend fun appendTimeLogEntry(entry: TimeLogEntry) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.timeLogEntries]?.let { raw ->
                runCatching { json.decodeFromString<List<TimeLogEntry>>(raw) }.getOrNull()
            } ?: emptyList()
            prefs[Keys.timeLogEntries] = json.encodeToString(current + entry)
        }
    }

    suspend fun setDashboardLayout(settings: DashboardLayoutSettings) {
        dataStore.edit { it[Keys.dashboardLayout] = json.encodeToString(settings) }
    }

    suspend fun setCornerRadius(radius: Int) {
        dataStore.edit { it[Keys.cornerRadius] = radius.coerceIn(0, 48) }
    }

    suspend fun setPomodoroFocusMinutes(minutes: Int) {
        dataStore.edit { it[Keys.pomodoroFocusMinutes] = minutes.coerceIn(1, 240) }
    }

    suspend fun setStreamChatSettings(settings: StreamChatSettings) {
        dataStore.edit { it[Keys.streamChatSettings] = json.encodeToString(settings) }
    }

    suspend fun setGameOverlaySettings(settings: GameOverlaySettings) {
        dataStore.edit { it[Keys.gameOverlaySettings] = json.encodeToString(settings) }
    }

    suspend fun setGridLayout(layout: GridLayout) {
        dataStore.edit {
            it[Keys.gridRows] = layout.rows
            it[Keys.gridColumns] = layout.columns
        }
    }

    suspend fun setQuickLaunchApps(apps: List<QuickLaunchApp>) {
        dataStore.edit { it[Keys.quickLaunchApps] = json.encodeToString(apps) }
    }

    suspend fun setQuickOpenFolders(folders: List<QuickOpenFolder>) {
        dataStore.edit { it[Keys.quickOpenFolders] = json.encodeToString(folders) }
    }

    suspend fun setQuickOpenUrlGroups(groups: List<QuickOpenUrlGroup>) {
        dataStore.edit { it[Keys.quickOpenUrlGroups] = json.encodeToString(groups) }
    }

    suspend fun setQuickActions(actions: List<QuickActionItem>) {
        dataStore.edit { it[Keys.quickActions] = json.encodeToString(actions) }
    }

    suspend fun setPageGridLayout(pageId: String, layout: GridLayout) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.pageGridLayouts]?.let { raw ->
                runCatching { json.decodeFromString<Map<String, GridLayout>>(raw) }.getOrNull()
            } ?: emptyMap()
            prefs[Keys.pageGridLayouts] = json.encodeToString(current + (pageId to layout))
        }
    }

    suspend fun setServerConnection(settings: ServerConnectionSettings) {
        dataStore.edit {
            it[Keys.serverHost] = settings.host
            it[Keys.serverPort] = settings.port
            it[Keys.autoConnect] = settings.autoConnect
            it[Keys.serverPin] = settings.pin
        }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    private fun mergeQuickActionsWithDefaults(saved: List<QuickActionItem>?): List<QuickActionItem> {
        if (saved == null) return defaultQuickActions
        val seen = saved.map { it.id }.toSet()
        return saved + defaultQuickActions.filter { it.id !in seen }
    }

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val accentColor = stringPreferencesKey("accent_color")
        val enableGlass = booleanPreferencesKey("enable_glass")
        val cornerRadius = intPreferencesKey("corner_radius")
        val gridRows = intPreferencesKey("grid_rows")
        val gridColumns = intPreferencesKey("grid_columns")
        val serverHost = stringPreferencesKey("server_host")
        val serverPort = intPreferencesKey("server_port")
        val autoConnect = booleanPreferencesKey("auto_connect")
        val serverPin = stringPreferencesKey("server_pin")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val quickLaunchApps = stringPreferencesKey("quick_launch_apps")
        val quickOpenFolders = stringPreferencesKey("quick_open_folders")
        val quickOpenUrlGroups = stringPreferencesKey("quick_open_url_groups")
        val quickActions = stringPreferencesKey("quick_actions")
        val pageGridLayouts = stringPreferencesKey("page_grid_layouts")
        val appLanguage = stringPreferencesKey("app_language")
        val focusProfileRules = stringPreferencesKey("focus_profile_rules")
        val focusTrackingEnabled = booleanPreferencesKey("focus_tracking_enabled")
        val obsUrl = stringPreferencesKey("obs_url")
        val obsPassword = stringPreferencesKey("obs_password")
        val backgroundImageUrl = stringPreferencesKey("background_image_url")
        val timeTrackerProjects = stringPreferencesKey("time_tracker_projects")
        val timeLogEntries = stringPreferencesKey("time_log_entries")
        val dashboardLayout = stringPreferencesKey("dashboard_layout")
        val pomodoroFocusMinutes = intPreferencesKey("pomodoro_focus_minutes")
        val streamChatSettings = stringPreferencesKey("stream_chat_settings")
        val gameOverlaySettings = stringPreferencesKey("game_overlay_settings")
    }
}

data class ObsConnectionSettings(
    val url: String = "ws://127.0.0.1:4455",
    val password: String = "",
)
