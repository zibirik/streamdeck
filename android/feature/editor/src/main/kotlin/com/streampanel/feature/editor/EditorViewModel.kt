package com.streampanel.feature.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streampanel.core.database.ControlPanelRepository
import com.streampanel.core.model.ActionType
import com.streampanel.core.model.ActionWhen
import com.streampanel.core.model.ButtonState
import com.streampanel.core.model.ControlAction
import com.streampanel.core.model.DashboardButton
import com.streampanel.core.model.ControlPage
import com.streampanel.core.model.MacroProgram
import com.streampanel.core.model.MacroStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ControlPanelRepository,
    private val json: Json,
) : ViewModel() {
    private val buttonId: String = checkNotNull(savedStateHandle["buttonId"])
    private val draft = MutableStateFlow(EditorDraft())
    private val actionDrafts = MutableStateFlow<List<ActionDraft>>(emptyList())
    private val saved = MutableStateFlow(false)
    private val button = repository.observeButton(buttonId).filterNotNull()
    private val actions = repository.observeActions(buttonId)
    private val pages = repository.observeActiveProfile().flatMapLatest { profile ->
        if (profile == null) flowOf(emptyList()) else repository.observePages(profile.id)
    }

    private val editorCore = combine(button, actions, pages, draft, actionDrafts) { button, actions, pages, draft, actionDrafts ->
        val effectiveDraft = if (draft.initialized) draft else EditorDraft.from(button)
        val effectiveActions = if (actionDrafts.isNotEmpty()) {
            actionDrafts
        } else {
            actions.map { ActionDraft.from(it) }.ifEmpty { listOf(ActionDraft()) }
        }
        EditorCoreSnapshot(button, actions, pages, effectiveDraft, effectiveActions)
    }

    val uiState = combine(editorCore, saved) { core, saved ->
        EditorUiState(
            button = core.button,
            actions = core.actions,
            pages = core.pages,
            draft = core.draft,
            actionDrafts = core.actionDrafts,
            saved = saved,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        EditorUiState(),
    )

    fun updateTitle(value: String) = update { it.copy(title = value, initialized = true) }
    fun updateSubtitle(value: String) = update { it.copy(subtitle = value, initialized = true) }
    fun updateIcon(value: String) = update { it.copy(iconName = value, initialized = true) }
    fun updateImageUri(value: String) = update { it.copy(imageUri = value, initialized = true) }
    fun updateGifUri(value: String) = update { it.copy(gifUri = value, initialized = true) }
    fun updateStartColor(value: String) = update { it.copy(backgroundColor = value, initialized = true) }
    fun updateEndColor(value: String) = update { it.copy(gradientEndColor = value, initialized = true) }
    fun updateRowSpan(value: Int) = update { it.copy(rowSpan = value.coerceIn(1, 6), initialized = true) }
    fun updateColumnSpan(value: Int) = update { it.copy(columnSpan = value.coerceIn(1, 6), initialized = true) }
    fun updateToggle(enabled: Boolean) = update { it.copy(isToggle = enabled, initialized = true) }
    fun updateActiveIcon(value: String) = update { it.copy(activeIconName = value, initialized = true) }
    fun updateActiveColor(value: String) = update { it.copy(activeBackgroundColor = value, initialized = true) }

    fun addAction() {
        actionDrafts.value = uiState.value.actionDrafts + ActionDraft()
        saved.value = false
    }

    fun applyTemplate(template: MacroTemplate) {
        val current = uiState.value.draft
        val (nextDraft, actions) = template.build(current, json)
        draft.value = nextDraft.copy(initialized = true)
        actionDrafts.value = actions
        saved.value = false
    }

    fun removeAction(index: Int) {
        val list = uiState.value.actionDrafts.toMutableList()
        if (list.size <= 1) return
        list.removeAt(index)
        actionDrafts.value = list
        saved.value = false
    }

    fun updateAction(index: Int, block: (ActionDraft) -> ActionDraft) {
        val list = uiState.value.actionDrafts.toMutableList()
        if (index !in list.indices) return
        list[index] = block(list[index])
        actionDrafts.value = list
        saved.value = false
    }

    fun deleteButton(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteButton(buttonId)
            onDeleted()
        }
    }

    fun save() {
        val state = uiState.value
        val sourceButton = state.button ?: return
        val draft = state.draft
        viewModelScope.launch {
            repository.upsertButton(
                sourceButton.copy(
                    title = draft.title,
                    subtitle = draft.subtitle.ifBlank { null },
                    iconName = draft.iconName.ifBlank { null },
                    imageUri = draft.imageUri.ifBlank { null },
                    gifUri = draft.gifUri.ifBlank { null },
                    backgroundColor = draft.backgroundColor,
                    gradientEndColor = draft.gradientEndColor.ifBlank { null },
                    rowSpan = draft.rowSpan,
                    columnSpan = draft.columnSpan,
                    isToggle = draft.isToggle,
                    activeIconName = draft.activeIconName.ifBlank { null },
                    activeBackgroundColor = draft.activeBackgroundColor.ifBlank { null },
                    state = if (draft.isToggle) ButtonState.Idle else sourceButton.state,
                ),
            )
            val savedActions = state.actionDrafts.mapIndexed { index, actionDraft ->
                ControlAction(
                    id = actionDraft.id,
                    buttonId = sourceButton.id,
                    type = actionDraft.actionType,
                    label = actionDraft.label.ifBlank { actionDraft.actionType.name },
                    payload = if (actionDraft.payloadKey.isBlank()) {
                        emptyMap()
                    } else {
                        mapOf(actionDraft.payloadKey to actionDraft.payloadValue)
                    },
                    sortOrder = index,
                    whenState = actionDraft.whenState,
                )
            }
            repository.replaceActions(sourceButton.id, savedActions)
            saved.value = true
        }
    }

    private fun update(block: (EditorDraft) -> EditorDraft) {
        draft.value = block(uiState.value.draft)
        saved.value = false
    }
}

enum class MacroTemplate(val key: String, val programmable: Boolean = false) {
    OpenBrowser("browser"),
    OpenWebsite("website"),
    OpenApp("app"),
    ObsToggleStream("obs_stream"),
    ObsSaveReplay("obs_replay"),
    Screenshot("screenshot"),
    DiscordMute("discord_mute"),
    MicMute("mic_mute"),
    FocusMode("focus_mode"),
    MeetingMute("meeting_mute"),
    OpenStudy("study_pack"),
    TwitchChat("twitch_chat"),
    YoutubeStudio("youtube_studio"),
    PcConfigurator("pc_configurator"),
    Cs2GsiHelp("cs2_gsi_help"),
    WindowLayout("window_layout"),
    MoveToMonitor2("move_monitor_2"),
    GameMode("game_mode"),
    StreamStartPack("stream_start_pack"),
    WorkFocusPack("work_focus_pack"),
    ProgramSequence("program_sequence", programmable = true),
    ProgramLoop("program_loop", programmable = true),
    ProgramTimer("program_timer", programmable = true),
    ProgramCondition("program_condition", programmable = true);

    fun build(current: EditorDraft, json: Json): Pair<EditorDraft, List<ActionDraft>> = when (this) {
        OpenBrowser -> current.copy(
            title = "Browser",
            subtitle = "Open default browser",
            iconName = "public",
            backgroundColor = "#4285F4",
        ) to listOf(ActionDraft(actionType = ActionType.LaunchProcess, payloadKey = "path", payloadValue = "chrome.exe"))
        OpenWebsite -> current.copy(
            title = "Website",
            subtitle = "Open URL",
            iconName = "public",
            backgroundColor = "#38BDF8",
        ) to listOf(ActionDraft(actionType = ActionType.OpenUrl, payloadKey = "url", payloadValue = "https://google.com"))
        OpenApp -> current.copy(
            title = "App",
            subtitle = "Launch program",
            iconName = "apps",
            backgroundColor = "#8B5CF6",
        ) to listOf(ActionDraft(actionType = ActionType.LaunchProcess, payloadKey = "path", payloadValue = "notepad.exe"))
        ObsToggleStream -> current.copy(
            title = "OBS Stream",
            subtitle = "Toggle stream",
            iconName = "live_tv",
            backgroundColor = "#EF4444",
        ) to listOf(ActionDraft(actionType = ActionType.ObsCommand, payloadKey = "command", payloadValue = "ToggleStream"))
        ObsSaveReplay -> current.copy(
            title = "Save Replay",
            subtitle = "OBS replay buffer",
            iconName = "fiber_manual_record",
            backgroundColor = "#F59E0B",
        ) to listOf(ActionDraft(actionType = ActionType.ObsCommand, payloadKey = "command", payloadValue = "SaveReplayBuffer"))
        Screenshot -> current.copy(
            title = "Screenshot",
            subtitle = "Win+Shift+S",
            iconName = "photo_camera",
            backgroundColor = "#38BDF8",
        ) to listOf(ActionDraft(actionType = ActionType.SystemCommand, payloadKey = "name", payloadValue = "screenshot"))
        DiscordMute -> current.copy(
            title = "Discord Mute",
            subtitle = "Focus Discord + mute",
            iconName = "mic_off",
            backgroundColor = "#5865F2",
        ) to listOf(ActionDraft(actionType = ActionType.SystemCommand, payloadKey = "name", payloadValue = "discord_mute"))
        MicMute -> current.copy(
            title = "Mic Mute",
            subtitle = "System mic",
            iconName = "mic_off",
            backgroundColor = "#8B5CF6",
        ) to listOf(ActionDraft(actionType = ActionType.VolumeCommand, payloadKey = "action", payloadValue = "mic_mute_toggle"))
        FocusMode -> current.copy(
            title = "Focus",
            subtitle = "Notifications off",
            iconName = "timer",
            backgroundColor = "#22C55E",
            isToggle = true,
            activeBackgroundColor = "#EF4444",
        ) to listOf(
            ActionDraft(actionType = ActionType.SystemCommand, payloadKey = "name", payloadValue = "focus_mode_on", whenState = ActionWhen.On),
            ActionDraft(actionType = ActionType.SystemCommand, payloadKey = "name", payloadValue = "focus_mode_off", whenState = ActionWhen.Off),
        )
        MeetingMute -> current.copy(
            title = "Meeting Mute",
            subtitle = "Teams/Zoom mic hotkey",
            iconName = "mic_off",
            backgroundColor = "#0EA5E9",
        ) to listOf(ActionDraft(actionType = ActionType.Hotkey, payloadKey = "keys", payloadValue = "CTRL+SHIFT+M"))
        OpenStudy -> current.copy(
            title = "Study Pack",
            subtitle = "Docs + timer",
            iconName = "school",
            backgroundColor = "#14B8A6",
        ) to listOf(
            ActionDraft(actionType = ActionType.OpenUrlGroup, payloadKey = "urls", payloadValue = "https://calendar.google.com|https://docs.google.com|https://chat.openai.com"),
            ActionDraft(actionType = ActionType.SystemCommand, payloadKey = "name", payloadValue = "focus_mode_on"),
        )
        TwitchChat -> current.copy(
            title = "Twitch Chat",
            subtitle = "Open popout chat",
            iconName = "chat",
            backgroundColor = "#9146FF",
        ) to listOf(ActionDraft(actionType = ActionType.OpenUrl, payloadKey = "url", payloadValue = "https://www.twitch.tv/popout/YOUR_CHANNEL/chat?popout="))
        YoutubeStudio -> current.copy(
            title = "YouTube Live",
            subtitle = "Studio + live control",
            iconName = "live_tv",
            backgroundColor = "#EF4444",
        ) to listOf(ActionDraft(actionType = ActionType.OpenUrlGroup, payloadKey = "urls", payloadValue = "https://studio.youtube.com|https://www.youtube.com/live_dashboard"))
        PcConfigurator -> current.copy(
            title = "PC Config",
            subtitle = "Open StreamPanel web",
            iconName = "settings",
            backgroundColor = "#38BDF8",
        ) to listOf(ActionDraft(actionType = ActionType.OpenUrl, payloadKey = "url", payloadValue = "http://127.0.0.1:17820"))
        Cs2GsiHelp -> current.copy(
            title = "CS2 HUD",
            subtitle = "Open telemetry setup",
            iconName = "sports_esports",
            backgroundColor = "#F97316",
        ) to listOf(ActionDraft(actionType = ActionType.OpenUrl, payloadKey = "url", payloadValue = "http://127.0.0.1:17820"))
        WindowLayout -> current.copy(
            title = "Window Layout",
            subtitle = "Snap active window left",
            iconName = "dashboard",
            backgroundColor = "#6366F1",
        ) to listOf(
            ActionDraft(actionType = ActionType.WindowCommand, payloadKey = "action", payloadValue = "snap_left"),
            ActionDraft(actionType = ActionType.Delay, payloadKey = "durationMs", payloadValue = "250"),
            ActionDraft(actionType = ActionType.Hotkey, payloadKey = "keys", payloadValue = "ALT+TAB"),
            ActionDraft(actionType = ActionType.WindowCommand, payloadKey = "action", payloadValue = "snap_right"),
        )
        MoveToMonitor2 -> current.copy(
            title = "Monitor 2",
            subtitle = "Move active window",
            iconName = "desktop_windows",
            backgroundColor = "#0EA5E9",
        ) to listOf(ActionDraft(actionType = ActionType.WindowCommand, payloadKey = "action", payloadValue = "move_monitor"))
        GameMode -> current.copy(
            title = "Game Mode",
            subtitle = "Focus + Discord mute + OBS replay",
            iconName = "sports_esports",
            backgroundColor = "#F97316",
            gradientEndColor = "#8B5CF6",
        ) to listOf(
            ActionDraft(actionType = ActionType.SystemCommand, payloadKey = "name", payloadValue = "focus_mode_on"),
            ActionDraft(actionType = ActionType.SystemCommand, payloadKey = "name", payloadValue = "discord_deafen"),
            ActionDraft(actionType = ActionType.ObsCommand, payloadKey = "command", payloadValue = "StartReplayBuffer"),
        )
        StreamStartPack -> current.copy(
            title = "Stream Start",
            subtitle = "OBS + dashboards + Discord",
            iconName = "live_tv",
            backgroundColor = "#EF4444",
            gradientEndColor = "#9146FF",
        ) to listOf(
            ActionDraft(actionType = ActionType.LaunchProcess, payloadKey = "path", payloadValue = "obs64.exe"),
            ActionDraft(actionType = ActionType.OpenUrlGroup, payloadKey = "urls", payloadValue = "https://dashboard.twitch.tv|https://studio.youtube.com"),
            ActionDraft(actionType = ActionType.LaunchProcess, payloadKey = "path", payloadValue = "%LOCALAPPDATA%\\Discord\\Update.exe --processStart Discord.exe"),
        )
        WorkFocusPack -> current.copy(
            title = "Work Focus",
            subtitle = "Focus + browser + notes",
            iconName = "work",
            backgroundColor = "#14B8A6",
            gradientEndColor = "#38BDF8",
        ) to listOf(
            ActionDraft(actionType = ActionType.SystemCommand, payloadKey = "name", payloadValue = "focus_mode_on"),
            ActionDraft(actionType = ActionType.LaunchProcess, payloadKey = "path", payloadValue = "chrome.exe"),
            ActionDraft(actionType = ActionType.LaunchProcess, payloadKey = "path", payloadValue = "notepad.exe"),
        )
        ProgramSequence -> current.programTemplate(
            title = "Program",
            subtitle = "Focus + wait + browser",
            color = "#14B8A6",
            program = MacroProgram(
                id = UUID.randomUUID().toString(),
                name = "Focus sequence",
                steps = listOf(
                    MacroStep.RunAction(programAction(ActionType.SystemCommand, "name", "focus_mode_on")),
                    MacroStep.Delay(1_000),
                    MacroStep.RunAction(programAction(ActionType.LaunchProcess, "path", "chrome.exe")),
                ),
            ),
            json = json,
        )
        ProgramLoop -> current.programTemplate(
            title = "Loop",
            subtitle = "Repeat hotkey 3x",
            color = "#F59E0B",
            program = MacroProgram(
                id = UUID.randomUUID().toString(),
                name = "Repeat hotkey",
                steps = listOf(
                    MacroStep.Loop(
                        count = 3,
                        steps = listOf(
                            MacroStep.RunAction(programAction(ActionType.Hotkey, "keys", "CTRL+S")),
                            MacroStep.Delay(500),
                        ),
                    ),
                ),
            ),
            json = json,
        )
        ProgramTimer -> current.programTemplate(
            title = "Timer",
            subtitle = "Run after delay",
            color = "#EF4444",
            program = MacroProgram(
                id = UUID.randomUUID().toString(),
                name = "Delayed action",
                steps = listOf(
                    MacroStep.Timer(
                        delayMs = 5_000,
                        steps = listOf(programRun(ActionType.SystemCommand, "name", "screenshot")),
                    ),
                ),
            ),
            json = json,
        )
        ProgramCondition -> current.programTemplate(
            title = "Condition",
            subtitle = "If variable then action",
            color = "#6366F1",
            program = MacroProgram(
                id = UUID.randomUUID().toString(),
                name = "Conditional action",
                variables = mapOf("mode" to "study"),
                steps = listOf(
                    MacroStep.Condition(
                        variable = "mode",
                        equals = "study",
                        thenSteps = listOf(programRun(ActionType.OpenUrl, "url", "https://docs.google.com")),
                        elseSteps = listOf(programRun(ActionType.LaunchProcess, "path", "notepad.exe")),
                    ),
                ),
            ),
            json = json,
        )
    }

    private fun EditorDraft.programTemplate(
        title: String,
        subtitle: String,
        color: String,
        program: MacroProgram,
        json: Json,
    ): Pair<EditorDraft, List<ActionDraft>> =
        copy(
            title = title,
            subtitle = subtitle,
            iconName = "code",
            backgroundColor = color,
        ) to listOf(
            ActionDraft(
                actionType = ActionType.Sequence,
                payloadKey = "program",
                payloadValue = json.encodeToString(program),
            ),
        )

    private fun programRun(type: ActionType, key: String, value: String): MacroStep.RunAction =
        MacroStep.RunAction(programAction(type, key, value))

    private fun programAction(type: ActionType, key: String, value: String): ControlAction =
        ControlAction(
            id = UUID.randomUUID().toString(),
            buttonId = "program",
            type = type,
            label = type.name,
            payload = mapOf(key to value),
        )
}

private data class EditorCoreSnapshot(
    val button: DashboardButton,
    val actions: List<ControlAction>,
    val pages: List<ControlPage>,
    val draft: EditorDraft,
    val actionDrafts: List<ActionDraft>,
)

data class EditorUiState(
    val button: DashboardButton? = null,
    val actions: List<ControlAction> = emptyList(),
    val pages: List<ControlPage> = emptyList(),
    val draft: EditorDraft = EditorDraft(),
    val actionDrafts: List<ActionDraft> = listOf(ActionDraft()),
    val saved: Boolean = false,
)

data class EditorDraft(
    val title: String = "",
    val subtitle: String = "",
    val iconName: String = "",
    val imageUri: String = "",
    val gifUri: String = "",
    val backgroundColor: String = "#242836",
    val gradientEndColor: String = "",
    val rowSpan: Int = 1,
    val columnSpan: Int = 1,
    val isToggle: Boolean = false,
    val activeIconName: String = "",
    val activeBackgroundColor: String = "",
    val initialized: Boolean = false,
) {
    companion object {
        fun from(button: DashboardButton): EditorDraft = EditorDraft(
            title = button.title,
            subtitle = button.subtitle.orEmpty(),
            iconName = button.iconName.orEmpty(),
            imageUri = button.imageUri.orEmpty(),
            gifUri = button.gifUri.orEmpty(),
            backgroundColor = button.backgroundColor,
            gradientEndColor = button.gradientEndColor.orEmpty(),
            rowSpan = button.rowSpan,
            columnSpan = button.columnSpan,
            isToggle = button.isToggle,
            activeIconName = button.activeIconName.orEmpty(),
            activeBackgroundColor = button.activeBackgroundColor.orEmpty(),
            initialized = true,
        )
    }
}

data class ActionDraft(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val actionType: ActionType = ActionType.Hotkey,
    val payloadKey: String = "keys",
    val payloadValue: String = "",
    val whenState: ActionWhen = ActionWhen.Always,
) {
    companion object {
        fun from(action: ControlAction): ActionDraft {
            val firstPayload = action.payload.entries.firstOrNull()
            return ActionDraft(
                id = action.id,
                label = action.label,
                actionType = action.type,
                payloadKey = firstPayload?.key ?: defaultPayloadKey(action.type),
                payloadValue = firstPayload?.value.orEmpty(),
                whenState = action.whenState,
            )
        }

        fun defaultPayloadKey(type: ActionType): String =
            when (type) {
                ActionType.OpenUrl -> "url"
                ActionType.LaunchProcess -> "path"
                ActionType.SendText -> "text"
                ActionType.Hotkey -> "keys"
                ActionType.Delay -> "durationMs"
                ActionType.HttpRequest -> "url"
                ActionType.TcpPacket -> "host"
                ActionType.UdpPacket -> "host"
                ActionType.MouseCommand -> "command"
                ActionType.MediaCommand -> "action"
                ActionType.VolumeCommand -> "action"
                ActionType.WindowCommand -> "action"
                ActionType.SystemCommand -> "name"
                ActionType.OpenFolder -> "path"
                ActionType.OpenUrlGroup -> "urls"
                ActionType.ObsCommand -> "command"
                ActionType.StreamlabsCommand -> "endpoint"
                ActionType.SpotifyCommand -> "command"
                ActionType.DiscordWebhook -> "webhookUrl"
                ActionType.HueCommand -> "bridgeUrl"
                ActionType.HomeAssistant -> "baseUrl"
                ActionType.Mqtt -> "topic"
                ActionType.Sequence -> "program"
                ActionType.NavigatePage -> "pageId"
                else -> "command"
            }
    }
}
