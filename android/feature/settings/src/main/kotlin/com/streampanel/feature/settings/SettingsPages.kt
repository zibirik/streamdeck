package com.streampanel.feature.settings

import com.streampanel.core.designsystem.BigButton
import com.streampanel.core.designsystem.BigField
import com.streampanel.core.designsystem.ChoicePill
import com.streampanel.core.designsystem.ColorDots
import com.streampanel.core.designsystem.HintText
import com.streampanel.core.designsystem.IconBubble
import com.streampanel.core.designsystem.MessagePill
import com.streampanel.core.designsystem.MoreOptions
import com.streampanel.core.designsystem.PageHeader
import com.streampanel.core.designsystem.SectionLabel
import com.streampanel.core.designsystem.SoftCard
import com.streampanel.core.designsystem.StatusDot
import com.streampanel.core.designsystem.Stepper
import com.streampanel.core.designsystem.ToggleCard
import com.streampanel.core.designsystem.UiPalette
import com.streampanel.core.designsystem.tr

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streampanel.core.designsystem.AppStrings
import com.streampanel.core.designsystem.LocalStreamPanelExtras
import com.streampanel.core.designsystem.baseSchemeFor
import com.streampanel.core.designsystem.parseHexColor
import com.streampanel.core.designsystem.strings
import com.streampanel.core.model.AppLanguage
import com.streampanel.core.model.QuickActionItem
import com.streampanel.core.model.QuickActionKeys
import com.streampanel.core.model.ThemeMode
import com.streampanel.core.network.ConnectionStatus
import kotlin.math.roundToInt

private const val GITHUB = "https://github.com/zibirik/streamdeck"
private const val CONTACT_EMAIL = "zibirik122@yandex.ru"

@Composable
fun SettingsPageContent(page: SettingsPage, state: SettingsUiState, vm: SettingsViewModel, version: String) {
    when (page) {
        SettingsPage.Connection -> ConnectionPage(state, vm)
        SettingsPage.Look -> LookPage(state, vm)
        SettingsPage.Deck -> DeckPage(state, vm)
        SettingsPage.Language -> LanguagePage(state, vm)
        SettingsPage.QuickActions -> QuickActionsPage(state, vm)
        SettingsPage.Programs -> ProgramsPage(state, vm)
        SettingsPage.Links -> LinksPage(state, vm)
        SettingsPage.Stream -> StreamPage(state, vm)
        SettingsPage.Game -> GamePage(state, vm)
        SettingsPage.Timer -> TimerPage(state, vm)
        SettingsPage.Backup -> BackupPage(state, vm)
        SettingsPage.About -> AboutPage(version)
        SettingsPage.Layout -> Unit
    }
}

// ---------------------------------------------------------------- My PC

@Composable
private fun ConnectionPage(state: SettingsUiState, vm: SettingsViewModel) {
    val look = connectionLook(state)
    val connected = state.connectionStatus is ConnectionStatus.Connected

    SoftCard(accent = look.color) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconBubble(Icons.Rounded.Computer, look.color, size = 64)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusDot(look.color, pulsing = connected || look.busy, size = 10)
                    Text(look.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(look.hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (connected) BigButton(tr("Отключить", "Disconnect"), vm::disconnect, Modifier.fillMaxWidth(), filled = false)
    }

    SectionLabel(tr("Шаг 1. Найдите свой ПК", "Step 1. Find your PC"))
    SoftCard {
        HintText(tr("ПК и планшет должны быть в одной Wi-Fi сети, а программа StreamPanel на ПК — запущена.", "The PC and the tablet must be on the same Wi-Fi, and StreamPanel must be running on the PC."))
        BigButton(
            text = if (state.scanning) tr("Ищу в Wi-Fi…", "Searching Wi-Fi…") else tr("Найти мой ПК", "Find my PC"),
            onClick = vm::scanNetwork,
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Rounded.Search,
            enabled = !state.scanning,
        )
        if (state.scanning) {
            LinearProgressIndicator(Modifier.fillMaxWidth().clip(RoundedCornerShape(99.dp)))
        }
        state.discoveredServers.forEach { server ->
            val picked = server.host == state.serverHostDraft.trim()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (picked) UiPalette.Green.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, if (picked) UiPalette.Green else Color.Transparent, RoundedCornerShape(18.dp))
                    .clickable { vm.selectDiscovered(server) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                IconBubble(Icons.Rounded.Computer, UiPalette.Green, size = 44)
                Column(Modifier.weight(1f)) {
                    Text(server.machineName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(server.host, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (picked) Icon(Icons.Rounded.Check, contentDescription = null, tint = UiPalette.Green)
            }
        }
        if (state.scanFinished && state.discoveredServers.isEmpty()) {
            HintText(
                tr(
                    "Не нашёл ни одного ПК. Проверьте: сервер запущен? Wi-Fi один и тот же? Брандмауэр разрешил доступ? Можно ввести адрес вручную — ниже.",
                    "No PC found. Is the server running? Same Wi-Fi? Is the firewall allowing it? You can also type the address below.",
                ),
            )
        }
    }

    SectionLabel(tr("Шаг 2. PIN-код", "Step 2. PIN"))
    SoftCard {
        BigField(
            value = state.serverPinDraft,
            onChange = vm::setServerPin,
            label = tr("PIN-код", "PIN"),
            leading = Icons.Rounded.Lock,
            secret = true,
        )
        HintText(
            tr(
                "Если PIN на ПК не задан — оставьте пустым. Задать его можно в файле appsettings.json рядом с программой на ПК.",
                "If the PC has no PIN, leave this empty. You can set one in appsettings.json next to the program on the PC.",
            ),
        )
    }

    SectionLabel(tr("Шаг 3. Подключитесь", "Step 3. Connect"))
    BigButton(
        text = tr("Сохранить и подключить", "Save and connect"),
        onClick = { vm.saveServerConnection(connectAfterSave = true) },
        modifier = Modifier.fillMaxWidth(),
        icon = Icons.Rounded.Link,
        enabled = !state.scanning && !look.busy,
    )
    ToggleCard(
        icon = Icons.Rounded.Wifi,
        color = UiPalette.Cyan,
        title = tr("Подключаться сам", "Connect automatically"),
        subtitle = tr("При запуске приложения", "When the app starts"),
        checked = state.server.autoConnect,
        onChange = vm::setAutoConnect,
    )

    MoreOptions(tr("Ввести адрес вручную", "Type the address by hand")) {
        BigField(
            value = state.serverHostDraft,
            onChange = vm::setServerHost,
            label = tr("Адрес ПК", "PC address"),
            placeholder = "192.168.1.76",
            leading = Icons.Rounded.Computer,
            keyboardType = KeyboardType.Uri,
        )
        BigField(
            value = state.serverPortDraft,
            onChange = vm::setServerPort,
            label = tr("Порт", "Port"),
            keyboardType = KeyboardType.Number,
        )
        BigButton(tr("Только сохранить", "Just save"), { vm.saveServerConnection(connectAfterSave = false) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Check, filled = false)
    }
}

// ---------------------------------------------------------------- Look

@Composable
private fun LookPage(state: SettingsUiState, vm: SettingsViewModel) {
    val s = strings()
    val modes = ThemeMode.entries.filter { it != ThemeMode.System || state.appearance.themeMode == ThemeMode.System }

    SectionLabel(tr("Тема", "Theme"))
    modes.chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            pair.forEach { mode ->
                ThemeCard(
                    mode = mode,
                    label = s.themeLabel(mode),
                    selected = state.appearance.themeMode == mode,
                    onClick = { vm.setTheme(mode) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }

    SectionLabel(tr("Главный цвет", "Main color"))
    SoftCard {
        ColorDots(selected = state.appearance.accentColor, colors = UiPalette.Accents, onPick = vm::setAccent)
    }

    SectionLabel(tr("Ещё", "More"))
    ToggleCard(
        icon = Icons.Rounded.BlurOn,
        color = UiPalette.Violet,
        title = tr("Стеклянные панели", "Glass panels"),
        subtitle = tr("Полупрозрачные карточки", "See-through cards"),
        checked = state.appearance.enableGlass,
        onChange = vm::setGlass,
    )
    ToggleCard(
        icon = Icons.Rounded.LightMode,
        color = UiPalette.Amber,
        title = s.keepScreenOn,
        subtitle = tr("Экран не гаснет, пока приложение открыто", "The screen stays on while the app is open"),
        checked = state.appearance.keepScreenOn,
        onChange = vm::setKeepScreenOn,
    )

    SectionLabel(tr("Фон", "Background"))
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { vm.setBackgroundImageUrl(it.toString()) }
    }
    SoftCard {
        BigButton(
            text = tr("Выбрать картинку из галереи", "Pick a picture from the gallery"),
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Rounded.PhotoLibrary,
        )
        if (state.appearance.backgroundImageUrl.isNotBlank()) {
            BigButton(tr("Убрать картинку", "Remove the picture"), { vm.setBackgroundImageUrl("") }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Delete, filled = false)
        }
    }
    MoreOptions(tr("Картинка по ссылке", "Picture from a link")) {
        BigField(
            value = state.appearance.backgroundImageUrl,
            onChange = vm::setBackgroundImageUrl,
            label = tr("Ссылка на картинку", "Picture link"),
            placeholder = "https://example.com/wallpaper.jpg",
            leading = Icons.Rounded.Link,
            keyboardType = KeyboardType.Uri,
        )
    }
}

@Composable
private fun ThemeCard(mode: ThemeMode, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = baseSchemeFor(mode)
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(scheme.background)
            .border(if (selected) 4.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(scheme.surface)
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(Modifier.fillMaxWidth(0.6f).height(8.dp).clip(CircleShape).background(scheme.onSurface.copy(alpha = 0.55f)))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(scheme.primary, scheme.secondary, scheme.tertiary).forEach { dot ->
                    Box(Modifier.size(18.dp).clip(RoundedCornerShape(6.dp)).background(dot))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = scheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(22.dp))
        }
    }
}

// ---------------------------------------------------------------- Buttons (grid)

@Composable
private fun DeckPage(state: SettingsUiState, vm: SettingsViewModel) {
    val rows = state.grid.rows
    val cols = state.grid.columns

    SoftCard {
        Text(
            tr("Так будут выглядеть кнопки", "This is how the buttons will look"),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val brush = LocalStreamPanelExtras.current.accentGradient
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(Modifier.width(280.dp).height(180.dp)) {
                repeat(rows) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        repeat(cols) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(if (rows > 8 || cols > 8) 1.5.dp else 4.dp)
                                    .clip(RoundedCornerShape(if (rows > 8 || cols > 8) 4.dp else 12.dp))
                                    .background(brush),
                            )
                        }
                    }
                }
            }
        }
        Text(
            tr("$cols кнопок в ряду · $rows рядов · всего ${cols * rows}", "$cols per row · $rows rows · ${cols * rows} total"),
            Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }

    SectionLabel(tr("Быстрый выбор", "Quick pick"))
    FlowPills {
        listOf(2, 3, 4, 5, 6).forEach { n ->
            ChoicePill("$n × $n", selected = rows == n && cols == n, onClick = { vm.setGrid(n, n) })
        }
    }

    SectionLabel(tr("Или настройте сами", "Or set it yourself"))
    SoftCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stepper(tr("В ряду", "Per row"), cols, 1..12, { vm.setGrid(rows, it) })
            Stepper(tr("Рядов", "Rows"), rows, 1..12, { vm.setGrid(it, cols) })
        }
    }

    ToggleCard(
        icon = Icons.Rounded.SwapHoriz,
        color = UiPalette.Blue,
        title = tr("Сам менять слой", "Switch layer automatically"),
        subtitle = tr("Когда на ПК открывается другая программа", "When another program opens on the PC"),
        checked = state.focusTrackingEnabled,
        onChange = vm::setFocusTracking,
    )
}

// ---------------------------------------------------------------- Language

@Composable
private fun LanguagePage(state: SettingsUiState, vm: SettingsViewModel) {
    listOf(
        Triple(AppLanguage.Russian, "🇷🇺", "Русский"),
        Triple(AppLanguage.English, "🇬🇧", "English"),
    ).forEach { (language, flag, name) ->
        val selected = state.language == language
        SoftCard(accent = if (selected) MaterialTheme.colorScheme.primary else null, onClick = { vm.setLanguage(language) }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(flag, fontSize = 44.sp)
                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (selected) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Quick actions

@Composable
private fun QuickActionsPage(state: SettingsUiState, vm: SettingsViewModel) {
    val s = strings()
    val actions = state.quickActions.sortedWith(compareBy({ it.row }, { it.sortOrder }))
    val on = actions.count { it.enabled }

    HintText(
        tr("Нажмите на действие, чтобы включить или выключить его. Включено: $on из ${actions.size}.", "Tap an action to turn it on or off. On: $on of ${actions.size}."),
    )
    FlowPills {
        actions.forEach { action ->
            ChoicePill(quickActionLabel(action, s), selected = action.enabled, onClick = { vm.setQuickActionEnabled(action.id, !action.enabled) })
        }
    }

    MoreOptions(tr("В каком ряду показывать", "Which row to show in")) {
        HintText(tr("Ряд 0 — самый верхний.", "Row 0 is the top one."))
        actions.filter { it.enabled }.forEach { action ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(quickActionLabel(action, s), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                FlowPills {
                    (0..5).forEach { row ->
                        ChoicePill("$row", selected = action.row == row, onClick = { vm.setQuickActionRow(action.id, row) })
                    }
                }
            }
        }
    }

    RestoreButton(vm::restoreQuickActionDefaults)
}

// ---------------------------------------------------------------- Programs

@Composable
private fun ProgramsPage(state: SettingsUiState, vm: SettingsViewModel) {
    HintText(tr("Нажмите на программу, чтобы изменить её. Выключатель — показывать ли её на панели.", "Tap a program to edit it. The switch decides whether it shows on the panel."))
    state.quickLaunchApps.forEach { app ->
        ItemCard(
            key = app.id,
            title = app.name,
            subtitle = app.path,
            color = parseHexColor(app.accent, UiPalette.Orange),
            enabled = app.enabled,
            onEnabled = { vm.updateQuickLaunchApp(app.id, enabled = it) },
            onDelete = if (state.quickLaunchApps.size > 1) ({ vm.removeQuickLaunchApp(app.id) }) else null,
        ) {
            BigField(app.name, { vm.updateQuickLaunchApp(app.id, name = it) }, tr("Название", "Name"))
            BigField(app.path, { vm.updateQuickLaunchApp(app.id, path = it) }, tr("Что запустить", "What to launch"), placeholder = "chrome.exe")
        }
    }
    AddRestoreButtons(tr("Добавить программу", "Add a program"), vm::addQuickLaunchApp, vm::restoreQuickLaunchDefaults)
}

// ---------------------------------------------------------------- Folders and links

@Composable
private fun LinksPage(state: SettingsUiState, vm: SettingsViewModel) {
    SectionLabel(tr("Папки", "Folders"))
    state.quickOpenFolders.forEach { folder ->
        ItemCard(
            key = folder.id,
            title = folder.name,
            subtitle = folder.path,
            color = parseHexColor(folder.accent, UiPalette.Lime),
            enabled = null,
            onDelete = if (state.quickOpenFolders.size > 1) ({ vm.removeFolder(folder.id) }) else null,
        ) {
            BigField(folder.name, { vm.updateFolder(folder.id, name = it) }, tr("Название", "Name"))
            BigField(folder.path, { vm.updateFolder(folder.id, path = it) }, tr("Путь к папке", "Folder path"), placeholder = "%USERPROFILE%\\Downloads")
        }
    }
    AddRestoreButtons(tr("Добавить папку", "Add a folder"), vm::addFolder, vm::restoreFolderDefaults)

    SectionLabel(tr("Группы ссылок", "Link groups"))
    HintText(tr("Одно касание — и все ссылки группы откроются на ПК.", "One tap opens every link in the group on the PC."))
    state.quickOpenUrlGroups.forEach { group ->
        val count = group.urls.split('|', ';', '\n').count { it.isNotBlank() }
        ItemCard(
            key = group.id,
            title = group.name,
            subtitle = tr("Ссылок: $count", "Links: $count"),
            color = parseHexColor(group.accent, UiPalette.Blue),
            enabled = null,
            onDelete = if (state.quickOpenUrlGroups.size > 1) ({ vm.removeUrlGroup(group.id) }) else null,
        ) {
            BigField(group.name, { vm.updateUrlGroup(group.id, name = it) }, tr("Название", "Name"))
            BigField(
                value = group.urls.replace("|", "\n"),
                onChange = { vm.updateUrlGroup(group.id, urls = it.replace("\n", "|")) },
                label = tr("Ссылки — каждая с новой строки", "Links — one per line"),
                placeholder = "https://example.com",
                leading = Icons.Rounded.Link,
                keyboardType = KeyboardType.Uri,
                singleLine = false,
            )
        }
    }
    AddRestoreButtons(tr("Добавить группу", "Add a group"), vm::addUrlGroup, vm::restoreUrlGroupDefaults)
}

// ---------------------------------------------------------------- Stream and chat

@Composable
private fun StreamPage(state: SettingsUiState, vm: SettingsViewModel) {
    val chat = state.streamChatSettings
    SoftCard {
        BigField(
            value = chat.twitchChannel,
            onChange = { vm.setStreamChatSettings(chat.copy(twitchChannel = it)) },
            label = tr("Ваш канал на Twitch", "Your Twitch channel"),
            placeholder = "your_channel",
            leading = Icons.Rounded.LiveTv,
        )
        BigField(
            value = chat.youtubeVideoId,
            onChange = { vm.setStreamChatSettings(chat.copy(youtubeVideoId = it)) },
            label = tr("Код трансляции на YouTube", "YouTube stream ID"),
            placeholder = "jfKfPfyJRdk",
            leading = Icons.Rounded.PlayCircle,
        )
        HintText(tr("Код — это последняя часть ссылки: youtube.com/watch?v=КОД", "The ID is the last part of the link: youtube.com/watch?v=ID"))
    }
    ToggleCard(Icons.Rounded.Dashboard, UiPalette.Blue, tr("Чат на главном экране", "Chat on the main screen"), chat.embedChatInDashboard, { vm.setStreamChatSettings(chat.copy(embedChatInDashboard = it)) })
    ToggleCard(Icons.Rounded.Chat, UiPalette.Violet, tr("Показывать чат Twitch", "Show Twitch chat"), chat.showTwitchChat, { vm.setStreamChatSettings(chat.copy(showTwitchChat = it)) })
    ToggleCard(Icons.Rounded.PlayCircle, UiPalette.Red, tr("Показывать чат YouTube", "Show YouTube chat"), chat.showYoutubeChat, { vm.setStreamChatSettings(chat.copy(showYoutubeChat = it)) })
}

// ---------------------------------------------------------------- Games

@Composable
private fun GamePage(state: SettingsUiState, vm: SettingsViewModel) {
    val s = strings()
    val game = state.gameOverlaySettings
    ToggleCard(
        icon = Icons.Rounded.AutoAwesome,
        color = UiPalette.Red,
        title = tr("Данные из игры", "Game data"),
        subtitle = tr("Здоровье, патроны, счёт — прямо на планшете", "Health, ammo, score — right on the tablet"),
        checked = game.enabled,
        onChange = { vm.setGameOverlaySettings(game.copy(enabled = it)) },
    )
    AnimatedVisibility(game.enabled, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionLabel(tr("Что показывать", "What to show"))
            FlowPills {
                ChoicePill(s.showHealth, game.showHealth, { vm.setGameOverlaySettings(game.copy(showHealth = !game.showHealth)) })
                ChoicePill(s.showAmmo, game.showAmmo, { vm.setGameOverlaySettings(game.copy(showAmmo = !game.showAmmo)) })
                ChoicePill(s.showMap, game.showMap, { vm.setGameOverlaySettings(game.copy(showMap = !game.showMap)) })
                ChoicePill(s.showScore, game.showScore, { vm.setGameOverlaySettings(game.copy(showScore = !game.showScore)) })
            }
            var patterns by rememberSaveable(game.autoShowProcessPatterns) { mutableStateOf(game.autoShowProcessPatterns) }
            MoreOptions(tr("Для каких игр включать", "Which games to react to")) {
                BigField(
                    value = patterns,
                    onChange = { patterns = it },
                    label = s.gameAutoProcesses,
                    placeholder = "cs2.exe;valorant.exe;dota2.exe",
                    singleLine = false,
                )
                HintText(tr("Названия программ через «;»", "Program names separated by “;”"))
                BigButton(
                    text = tr("Сохранить", "Save"),
                    onClick = { vm.setGameOverlaySettings(game.copy(autoShowProcessPatterns = patterns)) },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.Check,
                    enabled = patterns != game.autoShowProcessPatterns,
                )
            }
            HintText(s.gameStatusHint)
        }
    }
}

// ---------------------------------------------------------------- Timer

@Composable
private fun TimerPage(state: SettingsUiState, vm: SettingsViewModel) {
    SectionLabel(tr("Сколько минут работать без перерыва", "Minutes of focus"))
    SoftCard {
        var drag by remember(state.pomodoroFocusMinutes) { mutableFloatStateOf(state.pomodoroFocusMinutes.toFloat()) }
        Text(
            tr("${drag.roundToInt()} минут", "${drag.roundToInt()} minutes"),
            Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Slider(
            value = drag,
            onValueChange = { drag = it },
            onValueChangeFinished = { vm.setPomodoroFocusMinutes(drag.roundToInt()) },
            valueRange = 5f..120f,
            steps = 22,
        )
        FlowPills {
            listOf(15, 25, 30, 45, 60).forEach { minutes ->
                ChoicePill("$minutes", selected = state.pomodoroFocusMinutes == minutes, onClick = { vm.setPomodoroFocusMinutes(minutes) })
            }
        }
    }

    SectionLabel(tr("Проекты", "Projects"))
    HintText(tr("Таймер запоминает, сколько времени вы потратили на каждый проект.", "The timer remembers how long you spent on each project."))
    state.timeTrackerProjects.forEach { project ->
        ItemCard(
            key = project.id,
            title = project.name,
            subtitle = if (project.enabled) tr("Показывается", "Shown") else tr("Скрыт", "Hidden"),
            color = parseHexColor(project.color, UiPalette.Violet),
            enabled = project.enabled,
            onEnabled = { vm.updateTimeTrackerProject(project.id, enabled = it) },
            onDelete = if (state.timeTrackerProjects.size > 1) ({ vm.removeTimeTrackerProject(project.id) }) else null,
        ) {
            BigField(project.name, { vm.updateTimeTrackerProject(project.id, name = it) }, tr("Название проекта", "Project name"))
            ColorDots(project.color, UiPalette.Accents, { vm.updateTimeTrackerProject(project.id, color = it) })
        }
    }
    AddRestoreButtons(tr("Добавить проект", "Add a project"), vm::addTimeTrackerProject, vm::restoreTimeTrackerDefaults)

    if (state.timeLogEntries.isNotEmpty()) {
        SectionLabel(tr("Записи времени", "Time log"))
        SoftCard {
            HintText(tr("Записей: ${state.timeLogEntries.size}", "Entries: ${state.timeLogEntries.size}"))
            BigButton(tr("Сделать таблицу (CSV)", "Make a table (CSV)"), vm::exportTimeLogCsv, Modifier.fillMaxWidth(), icon = Icons.Rounded.Description, filled = false)
        }
        state.exportJson?.let { ExportBox(it, vm) }
    }
}

// ---------------------------------------------------------------- Backup

@Composable
private fun BackupPage(state: SettingsUiState, vm: SettingsViewModel) {
    val clipboard = LocalClipboardManager.current
    var confirmText by remember { mutableStateOf<String?>(null) }

    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBubble(Icons.Rounded.Download, UiPalette.Green)
            Column {
                Text(tr("Сохранить кнопки", "Save your buttons"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(tr("Чтобы перенести на другой планшет", "To move them to another tablet"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        BigButton(tr("Сделать копию", "Make a backup"), vm::exportDeck, Modifier.fillMaxWidth(), icon = Icons.Rounded.ContentCopy)
    }
    if (state.exportJson != null) ExportBox(state.exportJson, vm)

    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBubble(Icons.Rounded.Restore, UiPalette.Blue)
            Column {
                Text(tr("Вернуть кнопки", "Restore your buttons"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(tr("Скопируйте копию, потом нажмите кнопку", "Copy a backup, then tap the button"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        BigButton(
            text = tr("Вставить из буфера", "Paste from clipboard"),
            onClick = { confirmText = clipboard.getText()?.text.orEmpty() },
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Rounded.ContentPaste,
            filled = false,
        )
    }

    confirmText?.let { text ->
        AlertDialog(
            onDismissRequest = { confirmText = null },
            title = { Text(tr("Заменить текущие кнопки?", "Replace your current buttons?")) },
            text = { Text(tr("Кнопки из копии заменят те, что есть сейчас.", "The buttons from the backup will replace the ones you have now.")) },
            confirmButton = {
                TextButton(onClick = { vm.importDeck(text); confirmText = null }, enabled = text.isNotBlank()) {
                    Text(tr("Заменить", "Replace"))
                }
            },
            dismissButton = { TextButton(onClick = { confirmText = null }) { Text(tr("Отмена", "Cancel")) } },
        )
    }
}

@Composable
private fun ExportBox(text: String, vm: SettingsViewModel) {
    val clipboard = LocalClipboardManager.current
    var copied by remember(text) { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { vm.clearExport() } }
    SoftCard {
        Text(
            text,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(14.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
        )
        BigButton(
            text = if (copied) tr("Скопировано ✓", "Copied ✓") else tr("Копировать", "Copy"),
            onClick = { clipboard.setText(AnnotatedString(text)); copied = true },
            modifier = Modifier.fillMaxWidth(),
            icon = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
            color = if (copied) UiPalette.Green else null,
        )
    }
}

// ---------------------------------------------------------------- About

@Composable
private fun AboutPage(version: String) {
    val uri = LocalUriHandler.current
    SoftCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(LocalStreamPanelExtras.current.accentGradient),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(52.dp))
            }
            Text("StreamPanel", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (version.isNotBlank()) {
                Text(
                    "v$version",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
            Text(
                tr("Пульт управления вашим ПК: планшет в руках, всё под пальцем.", "A remote for your PC: the tablet in your hands, everything under your thumb."),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    SectionLabel(tr("Помощь", "Help"))
    LinkCard(Icons.Rounded.Language, UiPalette.Blue, tr("Страница проекта", "Project page"), "github.com/zibirik/streamdeck") { uri.openUri(GITHUB) }
    LinkCard(Icons.Rounded.BugReport, UiPalette.Red, tr("Что-то сломалось?", "Something broke?"), tr("Напишите нам — починим", "Tell us — we will fix it")) { uri.openUri("$GITHUB/issues/new/choose") }
    LinkCard(Icons.Rounded.Download, UiPalette.Green, tr("Новая версия", "New version"), tr("Скачать последнюю", "Download the latest")) { uri.openUri("$GITHUB/releases/latest") }

    SectionLabel(tr("Условия", "Terms"))
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBubble(Icons.Rounded.Shield, UiPalette.Cyan)
            Column {
                Text(tr("Ваши данные — только ваши", "Your data stays yours"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    tr("Приложение общается только с вашим ПК. Ни рекламы, ни слежки.", "The app talks only to your PC. No ads, no tracking."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    LinkCard(
        Icons.Rounded.Description,
        UiPalette.Violet,
        tr("Бесплатно для личного использования", "Free for personal use"),
        tr("Для бизнеса нужна лицензия — нажмите, чтобы узнать", "Business use needs a license — tap to learn more"),
    ) { uri.openUri("$GITHUB/blob/main/COMMERCIAL.md") }
    LinkCard(Icons.Rounded.Email, UiPalette.Amber, tr("Написать автору", "Write to the author"), CONTACT_EMAIL) { uri.openUri("mailto:$CONTACT_EMAIL") }
}

@Composable
private fun LinkCard(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, title: String, subtitle: String, onClick: () -> Unit) {
    SoftCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBubble(icon, color)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---------------------------------------------------------------- Shared bits for the pages

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowPills(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
}

@Composable
private fun RestoreButton(onClick: () -> Unit) {
    BigButton(tr("Вернуть как было", "Reset to default"), onClick, Modifier.fillMaxWidth(), icon = Icons.Rounded.Restore, filled = false)
}

@Composable
private fun AddRestoreButtons(addLabel: String, onAdd: () -> Unit, onRestore: () -> Unit) {
    BigButton(addLabel, onAdd, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
    RestoreButton(onRestore)
}

/** A row in a list: shows the name, opens to the fields when tapped. */
@Composable
private fun ItemCard(
    key: String,
    title: String,
    subtitle: String,
    color: Color,
    enabled: Boolean?,
    onDelete: (() -> Unit)?,
    onEnabled: (Boolean) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by rememberSaveable(key) { mutableStateOf(false) }
    val turn by androidx.compose.animation.core.animateFloatAsState(if (open) 180f else 0f, label = "item-arrow")
    SoftCard {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { open = !open },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.65f)))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    title.trim().take(1).uppercase().ifBlank { "?" },
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(title.ifBlank { "…" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (enabled != null) Switch(checked = enabled, onCheckedChange = onEnabled)
            Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.rotate(turn), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(open, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
                if (onDelete != null) {
                    BigButton(tr("Удалить", "Delete"), onDelete, Modifier.fillMaxWidth(), icon = Icons.Rounded.Delete, color = UiPalette.Red)
                }
            }
        }
    }
}

private fun quickActionLabel(action: QuickActionItem, s: AppStrings): String = when (action.actionKey) {
    QuickActionKeys.LOCK -> s.lock
    QuickActionKeys.PASTE -> s.paste
    QuickActionKeys.DESKTOP -> s.desktop
    QuickActionKeys.SYNC_VOLUME -> s.sync
    QuickActionKeys.COPY -> s.copy
    QuickActionKeys.ALT_TAB -> s.altTab
    QuickActionKeys.CLOSE_WINDOW -> s.closeWin
    QuickActionKeys.FULLSCREEN -> s.fullscreen
    QuickActionKeys.REFRESH -> s.refresh
    QuickActionKeys.UNDO -> s.undo
    QuickActionKeys.CUT -> s.cut
    QuickActionKeys.REDO -> s.redo
    QuickActionKeys.SAVE -> s.save
    QuickActionKeys.SEARCH -> s.search
    QuickActionKeys.NEW_TAB -> s.newTab
    QuickActionKeys.SCREENSHOT -> s.screenshot
    QuickActionKeys.TASK_MANAGER -> s.taskManager
    QuickActionKeys.SNAP_LEFT -> s.snapLeft
    QuickActionKeys.SNAP_RIGHT -> s.snapRight
    QuickActionKeys.PLAY_PAUSE -> s.media
    QuickActionKeys.PREVIOUS_TRACK -> s.previousTrack
    QuickActionKeys.NEXT_TRACK -> s.nextTrack
    else -> action.actionKey
}
