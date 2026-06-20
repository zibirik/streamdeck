package com.streampanel.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ControlProfile(
    val id: String,
    val name: String,
    val isActive: Boolean,
)

@Serializable
data class ControlPage(
    val id: String,
    val profileId: String,
    val parentPageId: String? = null,
    val title: String,
    val sortOrder: Int,
)

@Serializable
data class DashboardButton(
    val id: String,
    val pageId: String,
    val title: String,
    val subtitle: String? = null,
    val iconName: String? = null,
    val imageUri: String? = null,
    val gifUri: String? = null,
    val backgroundColor: String = "#242836",
    val gradientEndColor: String? = null,
    val row: Int = 0,
    val column: Int = 0,
    val rowSpan: Int = 1,
    val columnSpan: Int = 1,
    val state: ButtonState = ButtonState.Idle,
    val targetPageId: String? = null,
    val isFolder: Boolean = false,
    val isToggle: Boolean = false,
    val activeIconName: String? = null,
    val activeBackgroundColor: String? = null,
)

@Serializable
enum class ButtonState {
    Idle,
    Active,
    Disabled,
    Warning,
}

@Serializable
data class ControlAction(
    val id: String,
    val buttonId: String,
    val type: ActionType,
    val label: String,
    val payload: Map<String, String> = emptyMap(),
    val sortOrder: Int = 0,
    val whenState: ActionWhen = ActionWhen.Always,
)

@Serializable
enum class ActionWhen {
    Always,
    On,
    Off,
}

@Serializable
enum class ActionType {
    OpenUrl,
    LaunchProcess,
    HttpRequest,
    WebSocketCommand,
    TcpPacket,
    UdpPacket,
    SendText,
    Hotkey,
    MouseCommand,
    MediaCommand,
    VolumeCommand,
    WindowCommand,
    SystemCommand,
    OpenFolder,
    OpenUrlGroup,
    Delay,
    Sequence,
    ObsCommand,
    StreamlabsCommand,
    SpotifyCommand,
    DiscordWebhook,
    HueCommand,
    HomeAssistant,
    Mqtt,
    NavigatePage,
    Custom,
}

@Serializable
data class QuickActionItem(
    val id: String,
    val actionKey: String,
    val enabled: Boolean = true,
    val row: Int = 0,
    val sortOrder: Int = 0,
)

object QuickActionKeys {
    const val LOCK = "lock"
    const val PASTE = "paste"
    const val DESKTOP = "desktop"
    const val SYNC_VOLUME = "sync_volume"
    const val COPY = "copy"
    const val ALT_TAB = "alt_tab"
    const val CLOSE_WINDOW = "close_window"
    const val FULLSCREEN = "fullscreen"
    const val REFRESH = "refresh"
    const val UNDO = "undo"
    const val CUT = "cut"
    const val REDO = "redo"
    const val SAVE = "save"
    const val SEARCH = "search"
    const val NEW_TAB = "new_tab"
    const val SCREENSHOT = "screenshot"
    const val TASK_MANAGER = "task_manager"
    const val SNAP_LEFT = "snap_left"
    const val SNAP_RIGHT = "snap_right"
    const val PLAY_PAUSE = "play_pause"
    const val NEXT_TRACK = "next_track"
    const val PREVIOUS_TRACK = "previous_track"
}

@Serializable
data class GridLayout(
    val rows: Int = 4,
    val columns: Int = 4,
) {
    init {
        require(rows in 1..12) { "Rows must be between 1 and 12." }
        require(columns in 1..12) { "Columns must be between 1 and 12." }
    }
}

@Serializable
enum class ThemeMode {
    Dark,
    Midnight,
    OLED,
    Ocean,
    Sunset,
    Forest,
    Neon,
    Light,
    System,
}

@Serializable
enum class AppLanguage {
    English,
    Russian,
}

@Serializable
data class FocusProfileRule(
    val id: String,
    val processPattern: String,
    val pageId: String,
    val enabled: Boolean = true,
)

@Serializable
data class QuickOpenFolder(
    val id: String,
    val name: String,
    val path: String,
    val iconName: String = "folder",
    val accent: String = "#38BDF8",
)

@Serializable
data class QuickOpenUrlGroup(
    val id: String,
    val name: String,
    val urls: String,
    val iconName: String = "public",
    val accent: String = "#6366F1",
)

@Serializable
data class QuickLaunchApp(
    val id: String,
    val name: String,
    val path: String,
    val iconName: String,
    val accent: String = "#6366F1",
    val enabled: Boolean = true,
)

@Serializable
data class ServerConnectionSettings(
    val host: String = "192.168.1.10",
    val port: Int = 17820,
    val autoConnect: Boolean = true,
    val pin: String = "",
) {
    val websocketUrl: String
        get() = "ws://$host:$port/ws"
}

@Serializable
data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.Dark,
    val accentColor: String = "#8B5CF6",
    val enableGlass: Boolean = true,
    val cornerRadius: Int = 28,
    val keepScreenOn: Boolean = true,
    val backgroundImageUrl: String = "",
)

@Serializable
data class TimeTrackerProject(
    val id: String,
    val name: String,
    val color: String = "#8B5CF6",
    val enabled: Boolean = true,
)

@Serializable
data class TimeLogEntry(
    val projectId: String,
    val projectName: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val durationMinutes: Int,
)

@Serializable
data class PcProcessInfo(
    val name: String,
    val pid: Int,
    val memoryMb: Long,
)

@Serializable
data class PcStorageDrive(
    val name: String,
    val label: String? = null,
    val driveType: String? = null,
    val totalGb: Double = 0.0,
    val freeGb: Double = 0.0,
    val freePercent: Double = 0.0,
)

@Serializable
data class GameTelemetryInfo(
    val detected: Boolean = false,
    val provider: String? = null,
    val processName: String? = null,
    val windowTitle: String? = null,
    val mapName: String? = null,
    val phase: String? = null,
    val playerName: String? = null,
    val team: String? = null,
    val health: Int? = null,
    val armor: Int? = null,
    val ammoClip: Int? = null,
    val ammoReserve: Int? = null,
    val teamScore: Int? = null,
    val enemyScore: Int? = null,
    val pingMs: Int? = null,
    val note: String? = null,
)

@Serializable
data class StreamChatSettings(
    val twitchChannel: String = "",
    val youtubeVideoId: String = "",
    val showTwitchChat: Boolean = true,
    val showYoutubeChat: Boolean = false,
    val embedChatInDashboard: Boolean = true,
)

@Serializable
data class GameOverlaySettings(
    val enabled: Boolean = true,
    val autoShowProcessPatterns: String = "cs2.exe;csgo.exe;Counter-Strike",
    val showHealth: Boolean = true,
    val showAmmo: Boolean = true,
    val showMap: Boolean = true,
    val showScore: Boolean = true,
)

@Serializable
data class DeckExport(
    val version: Int = 1,
    val profile: ControlProfile,
    val pages: List<ControlPage>,
    val buttons: List<DashboardButton>,
    val actions: List<ControlAction>,
)
