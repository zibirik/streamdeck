package com.streampanel.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streampanel.core.designsystem.AppBackdrop
import com.streampanel.core.designsystem.strings
import com.streampanel.core.model.AppLanguage
import com.streampanel.core.network.ConnectionStatus
import kotlinx.coroutines.delay

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenLayoutCustomizer: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        vm = viewModel,
        onBack = onBack,
        onOpenLayoutCustomizer = onOpenLayoutCustomizer,
    )
}

/** Every section of the settings. [External] ones open another screen instead of a page here. */
enum class SettingsPage(
    val icon: ImageVector,
    val color: Color,
    val ru: String,
    val en: String,
    val subRu: String,
    val subEn: String,
    val external: Boolean = false,
) {
    Connection(Icons.Rounded.Computer, SettingsPalette.Green, "Мой ПК", "My PC", "Подключите планшет к компьютеру", "Link the tablet to your computer"),
    Look(Icons.Rounded.Palette, SettingsPalette.Violet, "Внешний вид", "Look", "Цвета, тема и фон", "Colors, theme and background"),
    Deck(Icons.Rounded.GridView, SettingsPalette.Blue, "Кнопки", "Buttons", "Сколько кнопок на экране", "How many buttons fit the screen"),
    Language(Icons.Rounded.Translate, SettingsPalette.Cyan, "Язык", "Language", "На каком языке приложение", "The language of the app"),
    QuickActions(Icons.Rounded.FlashOn, SettingsPalette.Amber, "Быстрые действия", "Quick actions", "Копировать, вставить, скриншот…", "Copy, paste, screenshot…"),
    Programs(Icons.Rounded.RocketLaunch, SettingsPalette.Orange, "Программы", "Programs", "Что запускать одним касанием", "What to launch with one tap"),
    Links(Icons.Rounded.FolderOpen, SettingsPalette.Lime, "Папки и ссылки", "Folders & links", "Быстрый доступ на ПК", "Quick access on the PC"),
    Stream(Icons.Rounded.LiveTv, SettingsPalette.Pink, "Стрим и чат", "Stream & chat", "Twitch и YouTube", "Twitch and YouTube"),
    Game(Icons.Rounded.SportsEsports, SettingsPalette.Red, "Игры", "Games", "Показывать данные из игры", "Show data from the game"),
    Timer(Icons.Rounded.Timer, SettingsPalette.Cyan, "Таймер", "Timer", "Помодоро и учёт времени", "Pomodoro and time tracking"),
    Layout(Icons.Rounded.Dashboard, SettingsPalette.Blue, "Панели", "Panels", "Что и где показывать", "What to show and where", external = true),
    Backup(Icons.Rounded.Backup, SettingsPalette.Slate, "Копия", "Backup", "Сохранить или перенести кнопки", "Save or move your buttons"),
    About(Icons.Rounded.Info, SettingsPalette.Violet, "О приложении", "About", "Версия, помощь и лицензия", "Version, help and license"),
    ;

    fun title(t: Txt) = t.t(ru, en)
    fun subtitle(t: Txt) = t.t(subRu, subEn)
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    vm: SettingsViewModel,
    onBack: () -> Unit,
    onOpenLayoutCustomizer: () -> Unit,
) {
    val txt = remember(state.language) { Txt(russian = state.language == AppLanguage.Russian) }
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    var pageName by rememberSaveable { mutableStateOf<String?>(null) }
    val page = pageName?.let { name -> SettingsPage.entries.firstOrNull { it.name == name } }

    BackHandler(enabled = page != null) {
        vm.clearExport()
        pageName = null
    }

    val message = state.message
    LaunchedEffect(message) {
        if (message != null) {
            delay(3200)
            vm.clearMessage()
        }
    }

    CompositionLocalProvider(LocalTxt provides txt) {
        AppBackdrop {
            Scaffold(containerColor = Color.Transparent) { padding ->
                Box(Modifier.padding(padding).fillMaxSize().imePadding(), contentAlignment = Alignment.TopCenter) {
                    AnimatedContent(
                        targetState = page,
                        transitionSpec = {
                            if (targetState != null) {
                                (slideInHorizontally { it / 6 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 6 } + fadeOut())
                            } else {
                                (slideInHorizontally { -it / 6 } + fadeIn()) togetherWith (slideOutHorizontally { it / 6 } + fadeOut())
                            }
                        },
                        label = "settings-page",
                    ) { current ->
                        if (current == null) {
                            SettingsHub(
                                state = state,
                                version = version,
                                onBack = onBack,
                                onOpen = { target ->
                                    if (target.external) onOpenLayoutCustomizer() else pageName = target.name
                                },
                                onConnect = { vm.saveServerConnection(connectAfterSave = true) },
                                onDisconnect = vm::disconnect,
                            )
                        } else {
                            PageFrame(
                                title = current.title(txt),
                                subtitle = current.subtitle(txt),
                                onBack = {
                                    vm.clearExport()
                                    pageName = null
                                },
                            ) {
                                SettingsPageContent(current, state, vm, version)
                            }
                        }
                    }
                    MessagePill(
                        text = message?.let { messageText(it, txt) },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PageFrame(title: String, subtitle: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PageHeader(title, subtitle, onBack)
            content()
            Spacer96()
        }
    }
}

@Composable
private fun Spacer96() = androidx.compose.foundation.layout.Spacer(Modifier.height(96.dp))

@Composable
private fun SettingsHub(
    state: SettingsUiState,
    version: String,
    onBack: () -> Unit,
    onOpen: (SettingsPage) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val txt = LocalTxt.current
    val s = strings()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 168.dp),
            modifier = Modifier.widthIn(max = 920.dp).fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                PageHeader(title = s.settings, subtitle = tr("Нажмите на картинку, чтобы изменить", "Tap a picture to change it"), onBack = onBack)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                ConnectionHero(
                    state = state,
                    onFind = { onOpen(SettingsPage.Connection) },
                    onConnect = onConnect,
                    onDisconnect = onDisconnect,
                )
            }
            SettingsPage.entries.filter { it != SettingsPage.Connection }.forEach { target ->
                item(key = target.name) {
                    HubTile(
                        icon = target.icon,
                        color = target.color,
                        title = target.title(txt),
                        value = hubSummary(target, state, version, txt, s),
                        onClick = { onOpen(target) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HubTile(icon: ImageVector, color: Color, title: String, value: String, onClick: () -> Unit) {
    SoftCard(onClick = onClick, modifier = Modifier.height(150.dp)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            IconBubble(icon, color, size = 56)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** How the connection looks to a person: one color, one headline, one hint. */
data class ConnectionLook(val color: Color, val title: String, val hint: String, val busy: Boolean)

@Composable
fun connectionLook(state: SettingsUiState): ConnectionLook {
    val address = "${state.server.host}:${state.server.port}"
    return when (val status = state.connectionStatus) {
        is ConnectionStatus.Connected -> ConnectionLook(
            SettingsPalette.Green, tr("ПК подключён", "PC connected"), address, busy = false,
        )
        ConnectionStatus.Connecting -> ConnectionLook(
            SettingsPalette.Amber, tr("Подключаюсь…", "Connecting…"), address, busy = true,
        )
        is ConnectionStatus.Failed -> ConnectionLook(
            SettingsPalette.Red,
            if (status.reason.contains("PIN", ignoreCase = true)) tr("Неверный PIN-код", "Wrong PIN") else tr("Не получилось подключиться", "Could not connect"),
            if (status.reason.contains("PIN", ignoreCase = true)) {
                tr("Проверьте PIN в настройках «Мой ПК»", "Check the PIN in “My PC”")
            } else {
                tr("Включён ли ПК, запущен ли сервер, одна ли у вас Wi-Fi сеть?", "Is the PC on, the server running, and both on the same Wi-Fi?")
            },
            busy = false,
        )
        ConnectionStatus.Disconnected -> ConnectionLook(
            SettingsPalette.Slate, tr("Не подключено", "Not connected"), tr("Нажмите «Найти ПК»", "Tap “Find PC”"), busy = false,
        )
    }
}

@Composable
private fun ConnectionHero(
    state: SettingsUiState,
    onFind: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val look = connectionLook(state)
    val connected = state.connectionStatus is ConnectionStatus.Connected
    SoftCard(accent = look.color, onClick = onFind) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(contentAlignment = Alignment.Center) {
                IconBubble(Icons.Rounded.Computer, look.color, size = 64)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusDot(look.color, pulsing = connected || look.busy, size = 10)
                    Text(
                        look.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                    )
                }
                Text(look.hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                connected -> BigButton(tr("Отключить", "Disconnect"), onDisconnect, Modifier.weight(1f), filled = false)
                look.busy -> BigButton(tr("Подключаюсь…", "Connecting…"), {}, Modifier.weight(1f), enabled = false)
                else -> {
                    BigButton(tr("Подключить", "Connect"), onConnect, Modifier.weight(1f), icon = Icons.Rounded.Link)
                    BigButton(tr("Найти ПК", "Find PC"), onFind, Modifier.weight(1f), icon = Icons.Rounded.Search, filled = false)
                }
            }
        }
    }
}

private fun hubSummary(
    page: SettingsPage,
    state: SettingsUiState,
    version: String,
    t: Txt,
    s: com.streampanel.core.designsystem.AppStrings,
): String = when (page) {
    SettingsPage.Connection -> state.server.host
    SettingsPage.Look -> s.themeLabel(state.appearance.themeMode)
    SettingsPage.Deck -> "${state.grid.columns} × ${state.grid.rows}"
    SettingsPage.Language -> if (state.language == AppLanguage.Russian) "Русский" else "English"
    SettingsPage.QuickActions -> {
        val on = state.quickActions.count { it.enabled }
        t.t("Включено $on из ${state.quickActions.size}", "$on of ${state.quickActions.size} on")
    }
    SettingsPage.Programs -> {
        val n = state.quickLaunchApps.count { it.enabled }
        t.t("На панели: $n", "On the panel: $n")
    }
    SettingsPage.Links -> t.t(
        "${state.quickOpenFolders.size} папок · ${state.quickOpenUrlGroups.size} групп",
        "${state.quickOpenFolders.size} folders · ${state.quickOpenUrlGroups.size} groups",
    )
    SettingsPage.Stream -> state.streamChatSettings.twitchChannel.ifBlank { t.t("Не настроено", "Not set up") }
    SettingsPage.Game -> if (state.gameOverlaySettings.enabled) t.t("Включено", "On") else t.t("Выключено", "Off")
    SettingsPage.Timer -> t.t("${state.pomodoroFocusMinutes} минут", "${state.pomodoroFocusMinutes} minutes")
    SettingsPage.Layout -> t.t("Вкладки и панели", "Tabs and panels")
    SettingsPage.Backup -> t.t("Сохранить и вернуть", "Save and restore")
    SettingsPage.About -> if (version.isBlank()) "StreamPanel" else "v$version"
}

private fun messageText(message: SettingsMessage, t: Txt): String = when (message.kind) {
    SettingsMessageKind.ConnectionSaved -> t.t("Сохранено ✓", "Saved ✓")
    SettingsMessageKind.Connecting -> t.t("Подключаюсь к ${message.detail.orEmpty()}…", "Connecting to ${message.detail.orEmpty()}…")
    SettingsMessageKind.DeckExported -> t.t("Готово! Теперь нажмите «Копировать»", "Done! Now tap “Copy”")
    SettingsMessageKind.DeckImported -> t.t("Кнопки загружены ✓", "Buttons restored ✓")
    SettingsMessageKind.ImportFailed -> t.t("Не получилось прочитать копию", "Could not read that backup")
    SettingsMessageKind.NothingToExport -> t.t("Пока нечего сохранять", "Nothing to save yet")
    SettingsMessageKind.NoTimeEntries -> t.t("Пока нет записей времени", "No time entries yet")
    SettingsMessageKind.TimeCsvReady -> t.t("Таблица готова — скопируйте её", "Table is ready — copy it")
}
