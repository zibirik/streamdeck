package com.streampanel.feature.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streampanel.core.designsystem.AppBackdrop
import com.streampanel.core.designsystem.ControlButtonCard
import com.streampanel.core.designsystem.GlassSurface
import com.streampanel.core.designsystem.StreamPanelTextField
import com.streampanel.core.designsystem.strings
import com.streampanel.core.model.ActionType
import com.streampanel.core.model.ActionWhen
import com.streampanel.core.model.ButtonState

@Composable
fun EditorRoute(
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    EditorScreen(
        state = state,
        onBack = onBack,
        onTitleChange = viewModel::updateTitle,
        onSubtitleChange = viewModel::updateSubtitle,
        onIconChange = viewModel::updateIcon,
        onImageUriChange = viewModel::updateImageUri,
        onGifUriChange = viewModel::updateGifUri,
        onStartColorChange = viewModel::updateStartColor,
        onEndColorChange = viewModel::updateEndColor,
        onRowSpanChange = viewModel::updateRowSpan,
        onColumnSpanChange = viewModel::updateColumnSpan,
        onToggleChange = viewModel::updateToggle,
        onActiveIconChange = viewModel::updateActiveIcon,
        onActiveColorChange = viewModel::updateActiveColor,
        onApplyTemplate = viewModel::applyTemplate,
        onAddAction = viewModel::addAction,
        onRemoveAction = viewModel::removeAction,
        onUpdateAction = viewModel::updateAction,
        onSave = viewModel::save,
        onDelete = { viewModel.deleteButton(onBack) },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    state: EditorUiState,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onSubtitleChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onImageUriChange: (String) -> Unit,
    onGifUriChange: (String) -> Unit,
    onStartColorChange: (String) -> Unit,
    onEndColorChange: (String) -> Unit,
    onRowSpanChange: (Int) -> Unit,
    onColumnSpanChange: (Int) -> Unit,
    onToggleChange: (Boolean) -> Unit,
    onActiveIconChange: (String) -> Unit,
    onActiveColorChange: (String) -> Unit,
    onApplyTemplate: (MacroTemplate) -> Unit,
    onAddAction: () -> Unit,
    onRemoveAction: (Int) -> Unit,
    onUpdateAction: (Int, (ActionDraft) -> ActionDraft) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val s = strings()
    AppBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = { Text(s.editorTitle, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = s.back, tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = s.delete, tint = Color.White)
                        }
                        Button(onClick = onSave, modifier = Modifier.padding(end = 16.dp)) {
                            Text(if (state.saved) s.saved else s.save)
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GlassSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(s.livePreview, style = MaterialTheme.typography.titleLarge, color = Color.White)
                        state.button?.let {
                            ControlButtonCard(
                                button = it.copy(
                                    title = state.draft.title,
                                    subtitle = state.draft.subtitle.ifBlank { null },
                                    iconName = state.draft.iconName.ifBlank { null },
                                    imageUri = state.draft.imageUri.ifBlank { null },
                                    gifUri = state.draft.gifUri.ifBlank { null },
                                    backgroundColor = state.draft.backgroundColor,
                                    gradientEndColor = state.draft.gradientEndColor.ifBlank { null },
                                    rowSpan = state.draft.rowSpan,
                                    columnSpan = state.draft.columnSpan,
                                    isToggle = state.draft.isToggle,
                                    activeIconName = state.draft.activeIconName.ifBlank { null },
                                    activeBackgroundColor = state.draft.activeBackgroundColor.ifBlank { null },
                                    state = if (state.draft.isToggle) ButtonState.Active else it.state,
                                ),
                                modifier = Modifier.fillMaxWidth().height(180.dp),
                                onClick = { Unit },
                                onLongClick = { Unit },
                            )
                        }
                    }
                }

                EditorSection(s.visual) {
                        StreamPanelTextField(state.draft.title, onTitleChange, s.name, Modifier.fillMaxWidth())
                        StreamPanelTextField(state.draft.subtitle, onSubtitleChange, s.subtitle, Modifier.fillMaxWidth())
                        StreamPanelTextField(state.draft.iconName, onIconChange, s.icon, Modifier.fillMaxWidth())
                        Text(s.iconPresets, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("bolt", "public", "apps", "folder", "live_tv", "mic_off", "timer", "school", "code", "sports_esports", "music_note", "photo_camera").forEach { icon ->
                                AssistChip(
                                    onClick = { onIconChange(icon) },
                                    label = { Text(icon) },
                                )
                            }
                        }
                        StreamPanelTextField(state.draft.imageUri, onImageUriChange, s.imageUrl, Modifier.fillMaxWidth())
                        StreamPanelTextField(state.draft.backgroundColor, onStartColorChange, s.accentColor, Modifier.fillMaxWidth())
                        StreamPanelTextField(state.draft.gradientEndColor, onEndColorChange, s.gradient, Modifier.fillMaxWidth())
                        Text(s.colorPresets, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("#8B5CF6", "#EF4444", "#38BDF8", "#22C55E", "#F59E0B", "#242836").forEach { hex ->
                                AssistChip(
                                    onClick = { onStartColorChange(hex) },
                                    label = { Text(hex) },
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StreamPanelTextField(
                                state.draft.rowSpan.toString(),
                                { onRowSpanChange(it.toIntOrNull() ?: state.draft.rowSpan) },
                                s.rowSpan,
                                Modifier.weight(1f),
                            )
                            StreamPanelTextField(
                                state.draft.columnSpan.toString(),
                                { onColumnSpanChange(it.toIntOrNull() ?: state.draft.columnSpan) },
                                s.columnSpan,
                                Modifier.weight(1f),
                            )
                        }
                    }

                EditorSection(s.toggleMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(s.twoStateButton, color = Color.White)
                            Switch(state.draft.isToggle, onToggleChange)
                        }
                        if (state.draft.isToggle) {
                            StreamPanelTextField(state.draft.activeIconName, onActiveIconChange, s.activeIcon, Modifier.fillMaxWidth())
                            StreamPanelTextField(state.draft.activeBackgroundColor, onActiveColorChange, s.activeColor, Modifier.fillMaxWidth())
                        }
                    }

                EditorSection(s.macroSteps(state.actionDrafts.size)) {
                        Text(s.macroTemplates, color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelLarge)
                        Text(s.simpleActions, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            MacroTemplate.entries.filterNot { it.programmable }.forEach { template ->
                                AssistChip(
                                    onClick = { onApplyTemplate(template) },
                                    label = { Text(s.macroTemplateLabel(template.key)) },
                                )
                            }
                        }
                        Text(s.advancedPrograms, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
                        Text(s.programMacroHint, color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.bodySmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            MacroTemplate.entries.filter { it.programmable }.forEach { template ->
                                AssistChip(
                                    onClick = { onApplyTemplate(template) },
                                    label = { Text(s.macroTemplateLabel(template.key)) },
                                )
                            }
                        }
                        state.actionDrafts.forEachIndexed { index, action ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(s.step(index + 1), color = Color.White)
                                    if (state.actionDrafts.size > 1) {
                                        IconButton(onClick = { onRemoveAction(index) }) {
                                            Icon(Icons.Default.Delete, contentDescription = s.delete, tint = Color.White)
                                        }
                                    }
                                }
                                ActionTypePicker(
                                    selected = action.actionType,
                                    onSelected = {
                                        onUpdateAction(index) { draft ->
                                            draft.copy(
                                                actionType = it,
                                                payloadKey = ActionDraft.defaultPayloadKey(it),
                                            )
                                        }
                                    },
                                )
                                WhenStatePicker(
                                    selected = action.whenState,
                                    enabled = state.draft.isToggle,
                                    onSelected = { onUpdateAction(index) { draft -> draft.copy(whenState = it) } },
                                )
                                StreamPanelTextField(
                                    value = action.payloadKey,
                                    onValueChange = { onUpdateAction(index) { draft -> draft.copy(payloadKey = it) } },
                                    label = s.payloadKey,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                StreamPanelTextField(
                                    value = action.payloadValue,
                                    onValueChange = { onUpdateAction(index) { draft -> draft.copy(payloadValue = it) } },
                                    label = when (action.actionType) {
                                        ActionType.Delay -> s.delayMs
                                        ActionType.NavigatePage -> s.targetLayerId
                                        ActionType.LaunchProcess -> s.pathOrCommand
                                        ActionType.Sequence -> s.advancedPrograms
                                        else -> s.payloadValue
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = action.actionType != ActionType.Sequence,
                                )
                                ActionValuePresets(
                                    s = s,
                                    actionType = action.actionType,
                                    onPreset = { key, value ->
                                        onUpdateAction(index) { draft -> draft.copy(payloadKey = key, payloadValue = value) }
                                    },
                                )
                                if (action.actionType == ActionType.LaunchProcess) {
                                    Text(
                                        s.launchAdminHint,
                                        color = Color.White.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                if (action.actionType == ActionType.NavigatePage) {
                                    Text(
                                        s.navigatePageHint,
                                        color = Color.White.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        state.pages.forEach { page ->
                                            AssistChip(
                                                onClick = {
                                                    onUpdateAction(index) { draft ->
                                                        draft.copy(payloadKey = "pageId", payloadValue = page.id)
                                                    }
                                                },
                                                label = { Text(page.title) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        OutlinedButton(onClick = onAddAction) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Text(" ${s.addStep}", color = Color.White)
                        }
                    }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionValuePresets(
    s: com.streampanel.core.designsystem.AppStrings,
    actionType: ActionType,
    onPreset: (String, String) -> Unit,
) {
    val presets = when (actionType) {
        ActionType.Hotkey -> listOf(
            s.copy to ("keys" to "CTRL+C"),
            s.paste to ("keys" to "CTRL+V"),
            s.save to ("keys" to "CTRL+S"),
            s.altTab to ("keys" to "ALT+TAB"),
            s.screenshot to ("keys" to "WIN+SHIFT+S"),
        )
        ActionType.LaunchProcess -> listOf(
            "Chrome" to ("path" to "chrome.exe"),
            "Explorer" to ("path" to "explorer.exe"),
            "Notepad" to ("path" to "notepad.exe"),
            "OBS" to ("path" to "obs64.exe"),
            "Discord" to ("path" to "%LOCALAPPDATA%\\Discord\\Update.exe --processStart Discord.exe"),
        )
        ActionType.OpenUrl -> listOf(
            "Google" to ("url" to "https://google.com"),
            "YouTube Studio" to ("url" to "https://studio.youtube.com"),
            "Twitch" to ("url" to "https://dashboard.twitch.tv"),
        )
        ActionType.SystemCommand -> listOf(
            s.screenshot to ("name" to "screenshot"),
            s.taskManager to ("name" to "task_manager"),
            "${s.startFocus}: ${s.pomodoroStart}" to ("name" to "focus_mode_on"),
            "${s.startFocus}: ${s.pomodoroStop}" to ("name" to "focus_mode_off"),
            s.discordMute to ("name" to "discord_mute"),
        )
        ActionType.ObsCommand -> listOf(
            s.obsStream to ("command" to "ToggleStream"),
            s.obsRecord to ("command" to "ToggleRecord"),
            s.obsReplay to ("command" to "SaveReplayBuffer"),
            s.obsStudioMode to ("command" to "ToggleStudioMode"),
        )
        else -> emptyList()
    }
    if (presets.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEach { (label, payload) ->
            AssistChip(onClick = { onPreset(payload.first, payload.second) }, label = { Text(label, maxLines = 1) })
        }
    }
}

@Composable
private fun EditorSection(title: String, content: @Composable () -> Unit) {
    GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White)
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionTypePicker(selected: ActionType, onSelected: (ActionType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        StreamPanelTextField(
            value = selected.name,
            onValueChange = {},
            label = strings().actionType,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ActionType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name) },
                    onClick = { expanded = false; onSelected(type) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WhenStatePicker(
    selected: ActionWhen,
    enabled: Boolean,
    onSelected: (ActionWhen) -> Unit,
) {
    if (!enabled) return
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        StreamPanelTextField(
            value = selected.name,
            onValueChange = {},
            label = strings().whenToggle,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ActionWhen.entries.forEach { whenState ->
                DropdownMenuItem(
                    text = { Text(whenState.name) },
                    onClick = { expanded = false; onSelected(whenState) },
                )
            }
        }
    }
}
