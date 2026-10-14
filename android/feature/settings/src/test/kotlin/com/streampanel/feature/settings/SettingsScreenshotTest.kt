package com.streampanel.feature.settings

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.streampanel.core.designsystem.LocalAppStrings
import com.streampanel.core.designsystem.StreamPanelTheme
import com.streampanel.core.designsystem.stringsFor
import com.streampanel.core.model.AppLanguage
import com.streampanel.core.model.AppearanceSettings
import com.streampanel.core.model.DashboardLayoutSettings
import com.streampanel.core.model.QuickActionItem
import com.streampanel.core.model.QuickActionKeys
import com.streampanel.core.model.QuickLaunchApp
import com.streampanel.core.model.QuickOpenFolder
import com.streampanel.core.model.QuickOpenUrlGroup
import com.streampanel.core.model.ServerConnectionSettings
import com.streampanel.core.model.ThemeMode
import com.streampanel.core.model.TimeTrackerProject
import com.streampanel.core.network.ConnectionStatus
import com.streampanel.core.network.DiscoveredServer
import java.io.File
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the real settings screens on the JVM and writes PNGs to build/screens, so the layout can be looked at
 * without a phone. Each test also proves the screen composes without crashing.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class SettingsScreenshotTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi") fun hub_phone() = shot("hub_phone", null)
    @Test @Config(qualifiers = "w1280dp-h800dp-hdpi") fun hub_tablet() = shot("hub_tablet", null)
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi") fun connection() = shot("connection", "Connection")
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi") fun look() = shot("look", "Look")
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi") fun deck() = shot("deck", "Deck")
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi") fun language() = shot("language", "Language")
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi") fun quick_actions() = shot("quick_actions", "QuickActions")
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi") fun programs() = shot("programs", "Programs")
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi") fun links() = shot("links", "Links")
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi") fun stream() = shot("stream", "Stream")
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi") fun game() = shot("game", "Game")
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi") fun timer() = shot("timer", "Timer")
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi") fun backup() = shot("backup", "Backup")
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi") fun about() = shot("about", "About")
    @Test @Config(qualifiers = "w1280dp-h800dp-hdpi") fun connection_tablet() = shot("connection_tablet", "Connection")
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi") fun hub_phone_english() = shot("hub_phone_en", null, language = AppLanguage.English)
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi") fun hub_phone_light() = shot("hub_phone_light", null, theme = ThemeMode.Light)

    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun layout_customizer() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.Russian)) {
                StreamPanelTheme {
                    LayoutCustomizerScreen(
                        layout = DashboardLayoutSettings(),
                        onBack = {},
                        onMovePanel = { _, _ -> },
                        onPanelVisibleChanged = { _, _ -> },
                        onAllPanelsVisibleChanged = {},
                        onPresetSelected = {},
                        onRestoreDefaults = {},
                    )
                }
            }
        }
        save("layout_customizer")
    }

    private fun shot(
        name: String,
        page: String?,
        theme: ThemeMode = ThemeMode.Dark,
        language: AppLanguage = AppLanguage.Russian,
    ) {
        val vm = Mockito.mock(SettingsViewModel::class.java)
        rule.mainClock.autoAdvance = false // the app has endless animations (glows, status dots)
        rule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(language)) {
                StreamPanelTheme(appearance = AppearanceSettings(themeMode = theme)) {
                    SettingsScreen(
                        state = sampleState(language),
                        vm = vm,
                        onBack = {},
                        onOpenLayoutCustomizer = {},
                        initialPage = page,
                    )
                }
            }
        }
        save(name)
    }

    private fun save(name: String) {
        rule.mainClock.advanceTimeBy(800)
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

    private fun sampleState(language: AppLanguage) = SettingsUiState(
        language = language,
        appearance = AppearanceSettings(),
        quickLaunchApps = listOf(
            QuickLaunchApp("1", "Chrome", "chrome.exe", "public", "#2563EB"),
            QuickLaunchApp("2", "OBS Studio", "obs64.exe", "live_tv", "#7C3AED"),
            QuickLaunchApp("3", "Discord", "discord.exe", "chat", "#5865F2", enabled = false),
        ),
        quickOpenFolders = listOf(QuickOpenFolder("f1", "Загрузки", "%USERPROFILE%\\Downloads"), QuickOpenFolder("f2", "Документы", "%USERPROFILE%\\Documents")),
        quickOpenUrlGroups = listOf(QuickOpenUrlGroup("u1", "Рабочие вкладки", "https://a.com|https://b.com|https://c.com")),
        quickActions = listOf(
            QuickActionKeys.LOCK, QuickActionKeys.PASTE, QuickActionKeys.DESKTOP, QuickActionKeys.COPY, QuickActionKeys.ALT_TAB,
            QuickActionKeys.SCREENSHOT, QuickActionKeys.UNDO, QuickActionKeys.SAVE, QuickActionKeys.SEARCH, QuickActionKeys.TASK_MANAGER,
        ).mapIndexed { i, key -> QuickActionItem("q$i", key, enabled = i % 3 != 2, row = i / 5, sortOrder = i) },
        server = ServerConnectionSettings(host = "192.168.1.76", port = 17820, autoConnect = true, pin = "1234"),
        serverHostDraft = "192.168.1.76",
        serverPinDraft = "1234",
        timeTrackerProjects = listOf(TimeTrackerProject("t1", "Учёба", "#3B82F6"), TimeTrackerProject("t2", "Стрим", "#EC4899")),
        connectionStatus = ConnectionStatus.Connected("ws://192.168.1.76:17820/ws"),
        discoveredServers = listOf(DiscoveredServer("192.168.1.76", 17820, "DESKTOP-ZIBIR"), DiscoveredServer("192.168.1.90", 17820, "LAPTOP")),
        scanFinished = true,
    )
}