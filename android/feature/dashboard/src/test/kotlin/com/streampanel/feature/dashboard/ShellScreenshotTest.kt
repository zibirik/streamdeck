package com.streampanel.feature.dashboard

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import android.graphics.Canvas
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.streampanel.core.designsystem.AppBackdrop
import com.streampanel.core.designsystem.LocalAppStrings
import com.streampanel.core.designsystem.StreamPanelTheme
import com.streampanel.core.designsystem.stringsFor
import com.streampanel.core.model.AppLanguage
import com.streampanel.core.model.AppearanceSettings
import com.streampanel.core.model.ControlPage
import com.streampanel.core.model.ControlProfile
import com.streampanel.core.model.DashboardButton
import com.streampanel.core.model.GridLayout
import com.streampanel.core.model.PcProcessInfo
import com.streampanel.core.model.PcStorageDrive
import com.streampanel.core.model.QuickActionItem
import com.streampanel.core.model.QuickActionKeys
import com.streampanel.core.model.QuickLaunchApp
import com.streampanel.core.model.QuickOpenFolder
import com.streampanel.core.model.QuickOpenUrlGroup
import com.streampanel.core.model.ThemeMode
import com.streampanel.core.model.TimeTrackerProject
import com.streampanel.core.network.ConnectionStatus
import java.io.File
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the real screens on the JVM and writes PNGs to build/screens, so the layout can be looked at
 * (and kept from regressing) without a phone. Each test also proves the screen composes without crashing.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ShellScreenshotTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val phone = "w411dp-h891dp-xxhdpi"
    private val tablet = "w1280dp-h800dp-hdpi"

    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_deck() = shot("phone_deck")

    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_deck_offline() = shot("phone_deck_offline", state = sampleState(connected = false))

    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_control() = shot("phone_control", tab = "Пульт")

    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_stream() = shot("phone_stream", tab = "Стрим")

    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_pc() = shot("phone_pc", tab = "Мой ПК")

    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_work() = shot("phone_work", tab = "Работа")

    @Test @Config(qualifiers = "w1280dp-h800dp-hdpi")
    fun tablet_deck() = shot("tablet_deck")

    @Test @Config(qualifiers = "w1280dp-h800dp-hdpi")
    fun tablet_deck_edit() = shot("tablet_deck_edit", tab = null, click = "Изменить")

    @Test @Config(qualifiers = "w1280dp-h800dp-hdpi")
    fun tablet_control() = shot("tablet_control", tab = "Пульт")

    @Test @Config(qualifiers = "w1280dp-h800dp-hdpi")
    fun tablet_stream() = shot("tablet_stream", tab = "Стрим")

    @Test @Config(qualifiers = "w1280dp-h800dp-hdpi")
    fun tablet_pc() = shot("tablet_pc", tab = "Мой ПК")

    @Test @Config(qualifiers = "w1280dp-h800dp-hdpi")
    fun tablet_work() = shot("tablet_work", tab = "Работа")

    @Test @Config(qualifiers = "w1280dp-h800dp-hdpi")
    fun tablet_control_light() = shot("tablet_control_light", tab = "Пульт", theme = ThemeMode.Light)

    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_control_english() = shot("phone_control_en", tab = "Control", language = AppLanguage.English)

    private fun shot(
        name: String,
        state: DashboardUiState = sampleState(connected = true),
        tab: String? = null,
        click: String? = null,
        theme: ThemeMode = ThemeMode.Dark,
        language: AppLanguage = AppLanguage.Russian,
    ) {
        rule.mainClock.autoAdvance = false // the app has endless animations (glows, status dots)
        rule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(language)) {
                StreamPanelTheme(appearance = AppearanceSettings(themeMode = theme)) {
                    AppBackdrop {
                        AdaptiveDashboardLayout(
                            state = state,
                            s = stringsFor(language),
                            connected = state.connectionStatus is ConnectionStatus.Connected,
                            callbacks = stubCallbacks(),
                        )
                    }
                }
            }
        }
        rule.mainClock.advanceTimeBy(200)
        if (tab != null) { rule.onAllNodesWithText(tab)[0].performClick(); rule.mainClock.advanceTimeBy(300) }
        if (click != null) { rule.onAllNodesWithText(click)[0].performClick(); rule.mainClock.advanceTimeBy(300) }
        rule.mainClock.advanceTimeBy(600)
        val view = rule.activity.window.decorView
        val full = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(full))
        // Crop the test harness's own title bar: keep only the app's content.
        val location = IntArray(2)
        rule.activity.findViewById<android.view.View>(android.R.id.content).getLocationInWindow(location)
        val bitmap = Bitmap.createBitmap(full, 0, location[1], full.width, full.height - location[1])
        val dir = File(System.getProperty("user.dir"), "build/screens").apply { mkdirs() }
        FileOutputStream(File(dir, "$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun sampleState(connected: Boolean): DashboardUiState {
        val profile = ControlProfile("p", "Main", true)
        val pages = listOf(
            ControlPage("a", "p", null, "Основной", 0),
            ControlPage("b", "p", null, "Офис", 1),
            ControlPage("c", "p", null, "Игры", 2),
        )
        fun button(i: Int, title: String, icon: String, c1: String, c2: String) = DashboardButton(
            id = "b$i", pageId = "a", title = title, subtitle = if (i % 2 == 0) "Нажми" else null, iconName = icon,
            backgroundColor = c1, gradientEndColor = c2, row = i / 4, column = i % 4, isFolder = i == 5,
        )
        val buttons = listOf(
            button(0, "Браузер", "public", "#2563EB", "#38BDF8"),
            button(1, "Микрофон", "mic_off", "#DC2626", "#F97316"),
            button(2, "Чат", "chat", "#7C3AED", "#EC4899"),
            button(3, "Эфир", "live_tv", "#059669", "#34D399"),
            button(4, "Музыка", "music_note", "#DB2777", "#F472B6"),
            button(5, "Проекты", "folder", "#B45309", "#F59E0B"),
            button(6, "Терминал", "terminal", "#334155", "#64748B"),
            button(7, "Игры", "sports_esports", "#4F46E5", "#818CF8"),
        )
        val actions = listOf(
            QuickActionKeys.LOCK, QuickActionKeys.PASTE, QuickActionKeys.DESKTOP, QuickActionKeys.COPY,
            QuickActionKeys.ALT_TAB, QuickActionKeys.SCREENSHOT, QuickActionKeys.UNDO, QuickActionKeys.SAVE,
        ).mapIndexed { i, key -> QuickActionItem("q$i", key, true, i / 4, i) }
        return DashboardUiState(
            profile = profile,
            pages = pages,
            currentPage = pages.first(),
            buttons = buttons,
            gridLayout = GridLayout(4, 4),
            quickLaunchApps = listOf(
                QuickLaunchApp("1", "Chrome", "chrome.exe", "public", "#2563EB"),
                QuickLaunchApp("2", "OBS", "obs64.exe", "live_tv", "#7C3AED"),
                QuickLaunchApp("3", "Discord", "discord.exe", "chat", "#5865F2"),
                QuickLaunchApp("4", "Терминал", "wt.exe", "terminal", "#334155"),
            ),
            quickOpenFolders = listOf(QuickOpenFolder("f1", "Загрузки", "x"), QuickOpenFolder("f2", "Документы", "x")),
            quickOpenUrlGroups = listOf(QuickOpenUrlGroup("u1", "Рабочие вкладки", "https://a.com")),
            quickActions = actions,
            connectionStatus = if (connected) ConnectionStatus.Connected("ws://192.168.1.76:17820/ws") else ConnectionStatus.Disconnected,
            volumeLevel = 62f,
            isPlaying = true,
            nowPlaying = "Daft Punk — Instant Crush",
            cpuPercent = 34f,
            ramPercent = 78f,
            foregroundProcess = "chrome.exe",
            downloadMbps = 12.4f,
            uploadMbps = 3.1f,
            networkInterface = "Wi-Fi",
            storageDrives = listOf(
                PcStorageDrive("C:", "System", "Fixed", 476.0, 120.5, 25.3),
                PcStorageDrive("D:", "Games", "Fixed", 931.0, 640.0, 68.7),
            ),
            topProcesses = listOf(PcProcessInfo("chrome.exe", 100, 1450), PcProcessInfo("obs64.exe", 200, 820)),
            pomodoroRemainingSeconds = 18 * 60 + 24,
            pomodoroFocusMinutes = 25,
            timeTrackerProjects = listOf(
                TimeTrackerProject("t1", "Учёба", "#3B82F6"),
                TimeTrackerProject("t2", "Стрим", "#EC4899"),
                TimeTrackerProject("t3", "Работа", "#F59E0B"),
            ),
            activeTimeProjectId = "t2",
            timeTrackerElapsedSeconds = 754,
            clipboardPreview = "https://example.com/some/long/link",
        )
    }

    private fun stubCallbacks() = DashboardCallbacks(
        onPageSelected = { _ -> },
        onButtonPressed = { _ -> },
        onMoveButton = { _, _, _ -> },
        onShowButtonActions = { _ -> },
        onVolumeChange = { _ -> },
        onVolumeSet = {},
        onMediaAction = { _ -> },
        onMicMute = {},
        onSpeakers = {},
        onHeadphones = {},
        onLaunchApp = { _ -> },
        onOpenFolder = { _ -> },
        onOpenUrlGroup = { _ -> },
        onMinimizeAll = {},
        onMaximize = {},
        onSnapLeft = {},
        onSnapRight = {},
        onMoveToMonitor = { _ -> },
        onLock = {},
        onAltTab = {},
        onCloseWindow = {},
        onFullscreen = {},
        onMinimizeActive = {},
        onScreenshot = {},
        onTaskManager = {},
        onKillProcess = {},
        onSleepLongPress = {},
        onQuickAction = { _ -> },
        onOpenSettings = {},
        onOpenObs = {},
        onOpenConnections = {},
        onConnect = {},
        onDisconnect = {},
        onCreatePage = { _ -> },
        onDeletePage = {},
        onCreateButton = {},
        onOpenLayerGridSettings = {},
        onRenamePage = { _ -> },
        onPasteFromPc = {},
        onStartPomodoro = {},
        onSetPomodoroFocusMinutes = { _ -> },
        onStopPomodoro = {},
        onStartTimeTracker = { _ -> },
        onStopTimeTracker = {},
        onKillProcessByPid = { _ -> },
        onCleanTemp = {},
        onOpenDiscord = {},
        onDiscordMute = {},
        onDiscordDeafen = {},
        onDiscordPushToTalk = {},
        onGitPull = {},
        onGitStatus = {},
        onDockerPs = {},
        onDockerUp = {},
        onVscodeTest = {},
        onVscodeFormat = {},
        onVscodeTerminal = {},
        onWifiRestart = {},
        onFlushDns = {},
        onEmptyRecycleBin = {},

    )
}