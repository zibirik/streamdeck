package com.streampanel.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streampanel.core.designsystem.AppBackdrop
import com.streampanel.core.designsystem.GlassSurface
import com.streampanel.core.designsystem.SectionHeader
import com.streampanel.core.designsystem.StreamPanelTextField
import com.streampanel.core.model.QuickActionItem
import com.streampanel.core.model.QuickActionKeys
import com.streampanel.core.model.QuickLaunchApp
import com.streampanel.core.model.QuickOpenFolder
import com.streampanel.core.model.QuickOpenUrlGroup
import com.streampanel.core.model.AppLanguage
import com.streampanel.core.model.GameOverlaySettings
import com.streampanel.core.model.StreamChatSettings
import com.streampanel.core.model.ThemeMode
import com.streampanel.core.designsystem.strings

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenLayoutCustomizer: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onBack = onBack,
        onOpenLayoutCustomizer = onOpenLayoutCustomizer,
        onThemeSelected = viewModel::setTheme,
        onAccentChanged = viewModel::setAccent,
        onGlassChanged = viewModel::setGlass,
        onCornerRadiusChanged = viewModel::setCornerRadius,
        onRowsChanged = viewModel::setRows,
        onColumnsChanged = viewModel::setColumns,
        onServerHostChanged = viewModel::setServerHost,
        onServerPortChanged = viewModel::setServerPort,
        onAutoConnectChanged = viewModel::setAutoConnect,
        onServerPinChanged = viewModel::setServerPin,
        onSaveConnection = { viewModel.saveServerConnection(connectAfterSave = false) },
        onSaveAndConnect = { viewModel.saveServerConnection(connectAfterSave = true) },
        onKeepScreenOnChanged = viewModel::setKeepScreenOn,
        onExportDeck = viewModel::exportDeck,
        onImportDeck = viewModel::importDeck,
        onQuickLaunchNameChanged = { id, name -> viewModel.updateQuickLaunchApp(id, name = name) },
        onQuickLaunchPathChanged = { id, path -> viewModel.updateQuickLaunchApp(id, path = path) },
        onAddQuickLaunch = viewModel::addQuickLaunchApp,
        onRemoveQuickLaunch = viewModel::removeQuickLaunchApp,
        onRestoreQuickLaunchDefaults = viewModel::restoreQuickLaunchDefaults,
        onFolderNameChanged = { id, name -> viewModel.updateFolder(id, name = name) },
        onFolderPathChanged = { id, path -> viewModel.updateFolder(id, path = path) },
        onAddFolder = viewModel::addFolder,
        onRemoveFolder = viewModel::removeFolder,
        onRestoreFolderDefaults = viewModel::restoreFolderDefaults,
        onUrlGroupNameChanged = { id, name -> viewModel.updateUrlGroup(id, name = name) },
        onUrlGroupUrlsChanged = { id, urls -> viewModel.updateUrlGroup(id, urls = urls) },
        onAddUrlGroup = viewModel::addUrlGroup,
        onRemoveUrlGroup = viewModel::removeUrlGroup,
        onRestoreUrlGroupDefaults = viewModel::restoreUrlGroupDefaults,
        onQuickActionEnabledChanged = viewModel::setQuickActionEnabled,
        onQuickActionRowChanged = viewModel::setQuickActionRow,
        onRestoreQuickActionDefaults = viewModel::restoreQuickActionDefaults,
        onLanguageSelected = viewModel::setLanguage,
        onFocusTrackingChanged = viewModel::setFocusTracking,
        onStreamChatSettingsChanged = viewModel::setStreamChatSettings,
        onGameOverlaySettingsChanged = viewModel::setGameOverlaySettings,
        onBackgroundImageChanged = viewModel::setBackgroundImageUrl,
        onQuickLaunchEnabledChanged = { id, enabled -> viewModel.updateQuickLaunchApp(id, enabled = enabled) },
        onPomodoroFocusMinutesChanged = viewModel::setPomodoroFocusMinutes,
        onAddTimeTrackerProject = viewModel::addTimeTrackerProject,
        onTimeTrackerNameChanged = { id, name -> viewModel.updateTimeTrackerProject(id, name = name) },
        onTimeTrackerColorChanged = { id, color -> viewModel.updateTimeTrackerProject(id, color = color) },
        onTimeTrackerEnabledChanged = { id, enabled -> viewModel.updateTimeTrackerProject(id, enabled = enabled) },
        onRemoveTimeTrackerProject = viewModel::removeTimeTrackerProject,
        onRestoreTimeTrackerDefaults = viewModel::restoreTimeTrackerDefaults,
        onExportTimeLog = viewModel::exportTimeLogCsv,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onOpenLayoutCustomizer: () -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onAccentChanged: (String) -> Unit,
    onGlassChanged: (Boolean) -> Unit,
    onCornerRadiusChanged: (Int) -> Unit,
    onRowsChanged: (Int) -> Unit,
    onColumnsChanged: (Int) -> Unit,
    onServerHostChanged: (String) -> Unit,
    onServerPortChanged: (String) -> Unit,
    onAutoConnectChanged: (Boolean) -> Unit,
    onServerPinChanged: (String) -> Unit,
    onSaveConnection: () -> Unit,
    onSaveAndConnect: () -> Unit,
    onKeepScreenOnChanged: (Boolean) -> Unit,
    onExportDeck: () -> Unit,
    onImportDeck: (String) -> Unit,
    onQuickLaunchNameChanged: (String, String) -> Unit,
    onQuickLaunchPathChanged: (String, String) -> Unit,
    onAddQuickLaunch: () -> Unit,
    onRemoveQuickLaunch: (String) -> Unit,
    onRestoreQuickLaunchDefaults: () -> Unit,
    onFolderNameChanged: (String, String) -> Unit,
    onFolderPathChanged: (String, String) -> Unit,
    onAddFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onRestoreFolderDefaults: () -> Unit,
    onUrlGroupNameChanged: (String, String) -> Unit,
    onUrlGroupUrlsChanged: (String, String) -> Unit,
    onAddUrlGroup: () -> Unit,
    onRemoveUrlGroup: (String) -> Unit,
    onRestoreUrlGroupDefaults: () -> Unit,
    onQuickActionEnabledChanged: (String, Boolean) -> Unit,
    onQuickActionRowChanged: (String, Int) -> Unit,
    onRestoreQuickActionDefaults: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onFocusTrackingChanged: (Boolean) -> Unit,
    onStreamChatSettingsChanged: (StreamChatSettings) -> Unit,
    onGameOverlaySettingsChanged: (GameOverlaySettings) -> Unit,
    onBackgroundImageChanged: (String) -> Unit,
    onQuickLaunchEnabledChanged: (String, Boolean) -> Unit,
    onPomodoroFocusMinutesChanged: (Int) -> Unit,
    onAddTimeTrackerProject: () -> Unit,
    onTimeTrackerNameChanged: (String, String) -> Unit,
    onTimeTrackerColorChanged: (String, String) -> Unit,
    onTimeTrackerEnabledChanged: (String, Boolean) -> Unit,
    onRemoveTimeTrackerProject: (String) -> Unit,
    onRestoreTimeTrackerDefaults: () -> Unit,
    onExportTimeLog: () -> Unit,
) {
    val s = strings()
    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onBackgroundImageChanged(it.toString()) }
    }
    AppBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(s.settings, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingsOverviewCard(state)

                SettingsSection(s.language, s.languageSub) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = state.language == AppLanguage.Russian,
                            onClick = { onLanguageSelected(AppLanguage.Russian) },
                            label = { Text(s.russian) },
                        )
                        FilterChip(
                            selected = state.language == AppLanguage.English,
                            onClick = { onLanguageSelected(AppLanguage.English) },
                            label = { Text(s.english) },
                        )
                    }
                }

                SettingsSection(s.focusTracking, s.focusTrackingSub) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(s.focusTrackingToggle, color = Color.White)
                        Switch(state.focusTrackingEnabled, onFocusTrackingChanged)
                    }
                    Text(
                        s.focusTrackingHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                SettingsSection(s.pcConnection, s.pcConnectionSub) {
                    StreamPanelTextField(
                        value = state.serverHostDraft,
                        onValueChange = onServerHostChanged,
                        label = s.serverHost,
                        placeholder = "192.168.1.76",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    StreamPanelTextField(
                        value = state.serverPortDraft,
                        onValueChange = onServerPortChanged,
                        label = s.port,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    StreamPanelTextField(
                        value = state.serverPinDraft,
                        onValueChange = onServerPinChanged,
                        label = s.pinCode,
                        placeholder = "",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = s.pinHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(s.autoConnect, color = Color.White)
                        Switch(state.server.autoConnect, onAutoConnectChanged)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onSaveConnection, modifier = Modifier.weight(1f)) {
                            Text(s.save, color = Color.White)
                        }
                        Button(onClick = onSaveAndConnect, modifier = Modifier.weight(1f)) {
                            Text(s.connect)
                        }
                    }
                }

                SettingsSection(s.layoutCustomization, s.layoutCustomizationSub) {
                    Button(onClick = onOpenLayoutCustomizer, modifier = Modifier.fillMaxWidth()) {
                        Text(s.customizeLayout)
                    }
                }

                SettingsSection(s.streamChatSettings, s.streamChatSettingsSub) {
                    StreamPanelTextField(
                        value = state.streamChatSettings.twitchChannel,
                        onValueChange = { onStreamChatSettingsChanged(state.streamChatSettings.copy(twitchChannel = it)) },
                        label = s.twitchChannel,
                        placeholder = "your_channel",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    StreamPanelTextField(
                        value = state.streamChatSettings.youtubeVideoId,
                        onValueChange = { onStreamChatSettingsChanged(state.streamChatSettings.copy(youtubeVideoId = it)) },
                        label = s.youtubeVideoId,
                        placeholder = "jfKfPfyJRdk",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SettingsSwitchRow(
                        label = s.embedChatInDashboard,
                        checked = state.streamChatSettings.embedChatInDashboard,
                        onCheckedChange = { onStreamChatSettingsChanged(state.streamChatSettings.copy(embedChatInDashboard = it)) },
                    )
                    SettingsSwitchRow(
                        label = s.showTwitchChat,
                        checked = state.streamChatSettings.showTwitchChat,
                        onCheckedChange = { onStreamChatSettingsChanged(state.streamChatSettings.copy(showTwitchChat = it)) },
                    )
                    SettingsSwitchRow(
                        label = s.showYoutubeChat,
                        checked = state.streamChatSettings.showYoutubeChat,
                        onCheckedChange = { onStreamChatSettingsChanged(state.streamChatSettings.copy(showYoutubeChat = it)) },
                    )
                }

                SettingsSection(s.gameOverlaySettings, s.gameOverlaySettingsSub) {
                    var gamePatternsDraft by rememberSaveable(state.gameOverlaySettings.autoShowProcessPatterns) {
                        mutableStateOf(state.gameOverlaySettings.autoShowProcessPatterns)
                    }
                    SettingsSwitchRow(
                        label = s.gameOverlaySettings,
                        checked = state.gameOverlaySettings.enabled,
                        onCheckedChange = { onGameOverlaySettingsChanged(state.gameOverlaySettings.copy(enabled = it)) },
                    )
                    StreamPanelTextField(
                        value = gamePatternsDraft,
                        onValueChange = { gamePatternsDraft = it },
                        label = s.gameAutoProcesses,
                        placeholder = "cs2.exe;Counter-Strike;valorant.exe;dota2.exe",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                    )
                    Button(
                        onClick = {
                            onGameOverlaySettingsChanged(state.gameOverlaySettings.copy(autoShowProcessPatterns = gamePatternsDraft))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(s.apply)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.gameOverlaySettings.showHealth,
                            onClick = { onGameOverlaySettingsChanged(state.gameOverlaySettings.copy(showHealth = !state.gameOverlaySettings.showHealth)) },
                            label = { Text(s.showHealth) },
                        )
                        FilterChip(
                            selected = state.gameOverlaySettings.showAmmo,
                            onClick = { onGameOverlaySettingsChanged(state.gameOverlaySettings.copy(showAmmo = !state.gameOverlaySettings.showAmmo)) },
                            label = { Text(s.showAmmo) },
                        )
                        FilterChip(
                            selected = state.gameOverlaySettings.showMap,
                            onClick = { onGameOverlaySettingsChanged(state.gameOverlaySettings.copy(showMap = !state.gameOverlaySettings.showMap)) },
                            label = { Text(s.showMap) },
                        )
                        FilterChip(
                            selected = state.gameOverlaySettings.showScore,
                            onClick = { onGameOverlaySettingsChanged(state.gameOverlaySettings.copy(showScore = !state.gameOverlaySettings.showScore)) },
                            label = { Text(s.showScore) },
                        )
                    }
                    Text(
                        s.gameStatusHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                SettingsSection(s.appearance, s.themesSub) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = state.appearance.themeMode == mode,
                                onClick = { onThemeSelected(mode) },
                                label = { Text(s.themeLabel(mode)) },
                            )
                        }
                    }
                    StreamPanelTextField(
                        value = state.appearance.accentColor,
                        onValueChange = onAccentChanged,
                        label = s.accentColor,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(s.glassPanels, color = Color.White)
                        Switch(state.appearance.enableGlass, onGlassChanged)
                    }
                    StreamPanelTextField(
                        value = state.appearance.cornerRadius.toString(),
                        onValueChange = { onCornerRadiusChanged(it.toIntOrNull() ?: state.appearance.cornerRadius) },
                        label = s.cornerRadius,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(s.keepScreenOn, color = Color.White)
                        Switch(state.appearance.keepScreenOn, onKeepScreenOnChanged)
                    }
                    StreamPanelTextField(
                        value = state.appearance.backgroundImageUrl,
                        onValueChange = onBackgroundImageChanged,
                        label = s.backgroundUrl,
                        placeholder = "https://example.com/wallpaper.jpg",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(
                        onClick = {
                            galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    ) {
                        Text(s.pickFromGallery, color = Color.White)
                    }
                }

                SettingsSection(s.gridLayout, s.gridLayoutSub) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(2, 3, 4, 5, 6).forEach { preset ->
                            AssistChip(
                                onClick = { onRowsChanged(preset); onColumnsChanged(preset) },
                                label = { Text("${preset}×$preset") },
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StreamPanelTextField(
                            value = state.grid.rows.toString(),
                            onValueChange = { onRowsChanged(it.toIntOrNull() ?: state.grid.rows) },
                            label = s.rows,
                            modifier = Modifier.weight(1f),
                        )
                        StreamPanelTextField(
                            value = state.grid.columns.toString(),
                            onValueChange = { onColumnsChanged(it.toIntOrNull() ?: state.grid.columns) },
                            label = s.columns,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                SettingsSection(s.quickActions, s.quickActionsSub) {
                    state.quickActions.sortedWith(compareBy({ it.row }, { it.sortOrder })).forEach { action ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(quickActionLabel(action, s), color = Color.White)
                                Text("${s.rows} ${action.row}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = action.enabled,
                                onCheckedChange = { onQuickActionEnabledChanged(action.id, it) },
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (0..5).forEach { row ->
                                FilterChip(
                                    selected = action.row == row,
                                    onClick = { onQuickActionRowChanged(action.id, row) },
                                    label = { Text("$row") },
                                )
                            }
                        }
                    }
                    OutlinedButton(onClick = onRestoreQuickActionDefaults) {
                        Text(s.restoreDefaults, color = Color.White)
                    }
                }

                SettingsSection(s.quickLaunch, s.quickLaunchSub) {
                    state.quickLaunchApps.forEach { app ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(s.showOnPanel, color = Color.White)
                                Switch(app.enabled, { onQuickLaunchEnabledChanged(app.id, it) })
                            }
                            EditableItem(
                                name = app.name,
                                path = app.path,
                                onNameChanged = { onQuickLaunchNameChanged(app.id, it) },
                                onPathChanged = { onQuickLaunchPathChanged(app.id, it) },
                                onRemove = { onRemoveQuickLaunch(app.id) },
                                canRemove = state.quickLaunchApps.size > 1,
                                nameLabel = s.name,
                                pathLabel = s.pathOrCommand,
                                pathPlaceholder = "chrome.exe",
                            )
                        }
                    }
                    SettingsAddRestoreRow(onAdd = onAddQuickLaunch, onRestore = onRestoreQuickLaunchDefaults, addLabel = s.addProgram, restoreLabel = s.restoreDefaults)
                }

                SettingsSection(s.timeTrackerSettings, s.timeTrackerSettingsSub) {
                    Text(
                        s.timeTrackerStorageHint,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    StreamPanelTextField(
                        value = state.pomodoroFocusMinutes.toString(),
                        onValueChange = { onPomodoroFocusMinutesChanged(it.toIntOrNull() ?: state.pomodoroFocusMinutes) },
                        label = s.pomodoroMinutes,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.timeTrackerProjects.forEach { project ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StreamPanelTextField(
                                value = project.name,
                                onValueChange = { onTimeTrackerNameChanged(project.id, it) },
                                label = s.project,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(project.enabled, { onTimeTrackerEnabledChanged(project.id, it) })
                            if (state.timeTrackerProjects.size > 1) {
                                IconButton(onClick = { onRemoveTimeTrackerProject(project.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.White)
                                }
                            }
                        }
                        StreamPanelTextField(
                            value = project.color,
                            onValueChange = { onTimeTrackerColorChanged(project.id, it) },
                            label = s.accentColor,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onAddTimeTrackerProject) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Text(s.addProject, modifier = Modifier.padding(start = 8.dp))
                        }
                        OutlinedButton(onClick = onExportTimeLog) {
                            Text(s.exportCsv, color = Color.White)
                        }
                        OutlinedButton(onClick = onRestoreTimeTrackerDefaults) {
                            Text(s.restoreDefaults, color = Color.White)
                        }
                    }
                    if (state.timeLogEntries.isNotEmpty()) {
                        Text(
                            s.entriesLogged(state.timeLogEntries.size),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                SettingsSection(s.navigationFolders, s.navigationFoldersSub) {
                    state.quickOpenFolders.forEach { folder ->
                        EditableItem(
                            name = folder.name,
                            path = folder.path,
                            onNameChanged = { onFolderNameChanged(folder.id, it) },
                            onPathChanged = { onFolderPathChanged(folder.id, it) },
                            onRemove = { onRemoveFolder(folder.id) },
                            canRemove = state.quickOpenFolders.size > 1,
                            nameLabel = s.name,
                            pathLabel = s.folderPath,
                            pathPlaceholder = "%USERPROFILE%\\Downloads",
                        )
                    }
                    SettingsAddRestoreRow(onAdd = onAddFolder, onRestore = onRestoreFolderDefaults, addLabel = s.addProgram, restoreLabel = s.restoreDefaults)
                }

                SettingsSection(s.urlGroups, s.urlGroupsSub) {
                    state.quickOpenUrlGroups.forEach { group ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                StreamPanelTextField(
                                    value = group.name,
                                    onValueChange = { onUrlGroupNameChanged(group.id, it) },
                                    label = s.groupName,
                                    modifier = Modifier.weight(1f),
                                )
                                if (state.quickOpenUrlGroups.size > 1) {
                                    IconButton(onClick = { onRemoveUrlGroup(group.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.White)
                                    }
                                }
                            }
                            StreamPanelTextField(
                                value = group.urls,
                                onValueChange = { onUrlGroupUrlsChanged(group.id, it) },
                                label = s.urls,
                                placeholder = "https://a.com|https://b.com",
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = false,
                            )
                        }
                    }
                    SettingsAddRestoreRow(onAdd = onAddUrlGroup, onRestore = onRestoreUrlGroupDefaults, addLabel = s.addProgram, restoreLabel = s.restoreDefaults)
                }

                SettingsSection(s.importExport, s.importExportSub) {
                    if (!state.statusMessage.isNullOrBlank()) {
                        Text(state.statusMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onExportDeck) { Text(s.exportDeck) }
                    }
                    state.exportJson?.let { json ->
                        StreamPanelTextField(
                            value = json,
                            onValueChange = {},
                            label = s.exportedJson,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                        )
                    }
                    var importDraft by remember { mutableStateOf("") }
                    StreamPanelTextField(
                        value = importDraft,
                        onValueChange = { importDraft = it },
                        label = s.pasteJsonImport,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                    )
                    Button(onClick = { onImportDeck(importDraft) }, enabled = importDraft.isNotBlank()) {
                        Text(s.importDeck)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsOverviewCard(state: SettingsUiState) {
    val s = strings()
    val languageLabel = when (state.language) {
        AppLanguage.English -> s.english
        AppLanguage.Russian -> s.russian
    }

    GlassSurface(Modifier.fillMaxWidth(), elevated = true) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader(s.settings, s.settingsOverviewSub)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("${s.language}: $languageLabel") })
                AssistChip(onClick = {}, label = { Text("${s.themes}: ${s.themeLabel(state.appearance.themeMode)}") })
                AssistChip(onClick = {}, label = { Text("${s.grid}: ${state.grid.columns}×${state.grid.rows}") })
                AssistChip(onClick = {}, label = { Text("${s.pcServer}: ${state.serverHostDraft}:${state.serverPortDraft}") })
                AssistChip(onClick = {}, label = { Text("${s.pomodoroTitle}: ${state.pomodoroFocusMinutes}") })
            }
            Text(
                s.settingsOverviewHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White)
        Switch(checked, onCheckedChange)
    }
}

@Composable
private fun SettingsSection(title: String, subtitle: String, content: @Composable () -> Unit) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    SectionHeader(title, subtitle)
                }
                Text(
                    if (expanded) "−" else "+",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (expanded) {
                content()
            }
        }
    }
}

@Composable
private fun EditableItem(
    name: String,
    path: String,
    onNameChanged: (String) -> Unit,
    onPathChanged: (String) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean,
    nameLabel: String,
    pathLabel: String,
    pathPlaceholder: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StreamPanelTextField(
                value = name,
                onValueChange = onNameChanged,
                label = nameLabel,
                modifier = Modifier.weight(1f),
            )
            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                }
            }
        }
        StreamPanelTextField(
            value = path,
            onValueChange = onPathChanged,
            label = pathLabel,
            placeholder = pathPlaceholder,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun quickActionLabel(action: QuickActionItem, s: com.streampanel.core.designsystem.AppStrings): String = when (action.actionKey) {
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

@Composable
private fun SettingsAddRestoreRow(onAdd: () -> Unit, onRestore: () -> Unit, addLabel: String, restoreLabel: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            Text(addLabel, modifier = Modifier.padding(start = 8.dp))
        }
        OutlinedButton(onClick = onRestore) {
            Text(restoreLabel, color = Color.White)
        }
    }
}
