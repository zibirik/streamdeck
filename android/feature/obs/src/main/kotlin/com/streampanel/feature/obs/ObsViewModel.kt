package com.streampanel.feature.obs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streampanel.core.datastore.ObsConnectionSettings
import com.streampanel.core.datastore.PreferencesDataSource
import com.streampanel.core.integrations.ObsWebSocketClient
import com.streampanel.core.model.StreamChatSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject

@HiltViewModel
class ObsViewModel @Inject constructor(
    private val obsWebSocketClient: ObsWebSocketClient,
    private val preferencesDataSource: PreferencesDataSource,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ObsUiState())
    val uiState: StateFlow<ObsUiState> = _uiState.asStateFlow()
    private var statusPollingJob: Job? = null

    init {
        viewModelScope.launch {
            preferencesDataSource.streamChatSettings.collect { settings ->
                patch { copy(streamChatSettings = settings) }
            }
        }
        viewModelScope.launch {
            val saved = preferencesDataSource.obsSettings.first()
            _uiState.update { it.copy(url = saved.url, password = saved.password) }
            refreshAll()
        }
    }

    fun updateUrl(value: String) {
        _uiState.update { it.copy(url = value) }
        persist()
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value) }
        persist()
    }

    fun startStatusPolling() {
        statusPollingJob?.cancel()
        statusPollingJob = viewModelScope.launch {
            while (isActive) {
                delay(2_500)
                if (_uiState.value.working) continue
                runCatching {
                    loadStatuses()
                    loadStudioMode()
                }
            }
        }
    }

    fun stopStatusPolling() {
        statusPollingJob?.cancel()
        statusPollingJob = null
    }

    fun refreshAll() = viewModelScope.launch {
        _uiState.update { it.copy(working = true, message = "Connecting…") }
        runCatching {
            loadStudioMode()
            loadScenes()
            loadInputs()
            refreshOutputStatuses()
            loadStats()
            loadStreamServiceSettings()
            patch { copy(message = "Connected") }
        }.onFailure { error ->
            patch { copy(message = error.message ?: "OBS connection failed") }
        }
        patch { copy(working = false) }
    }

    fun switchScene(sceneName: String) = runCommand("SetCurrentProgramScene", mapOf("sceneName" to sceneName)) {
        loadScenes()
    }

    fun toggleStream() = runCommand("ToggleStream") { refreshOutputStatuses() }
    fun startStream() = runCommand("StartStream") {
        refreshOutputStatuses()
        loadStreamServiceSettings()
    }
    fun stopStream() = runCommand("StopStream") { refreshOutputStatuses() }
    fun toggleRecord() = runCommand("ToggleRecord") { refreshOutputStatuses() }
    fun startRecord() = runCommand("StartRecord") { refreshOutputStatuses() }
    fun stopRecord() = runCommand("StopRecord") { refreshOutputStatuses() }
    fun pauseRecord() {
        val command = if (_uiState.value.recordingPaused) "ResumeRecord" else "PauseRecord"
        runCommand(command) { refreshOutputStatuses() }
    }
    fun toggleReplayBuffer() = runCommand("ToggleReplayBuffer")
    fun saveReplayBuffer() = runCommand("SaveReplayBuffer")
    fun toggleVirtualCam() = runCommand("ToggleVirtualCam")
    fun toggleStudioMode() = runCommand("ToggleStudioMode") {
        loadStudioMode()
        loadScenes()
    }
    fun previewScene(sceneName: String) = runCommand("SetCurrentPreviewScene", mapOf("sceneName" to sceneName)) {
        patch { copy(currentPreviewScene = sceneName) }
    }
    fun transitionStudio() = runCommand("TriggerStudioModeTransition") {
        loadScenes()
        loadStatuses()
    }
    fun toggleInputMute(inputName: String) = runCommand("ToggleInputMute", mapOf("inputName" to inputName)) {
        viewModelScope.launch { loadInputs() }
    }

    private fun runCommand(
        command: String,
        extra: Map<String, String> = emptyMap(),
        onSuccess: (suspend () -> Unit)? = null,
    ) {
        viewModelScope.launch {
            patch { copy(working = true) }
            val state = _uiState.value
            val payload = buildMap {
                put("url", state.url)
                put("password", state.password)
                put("command", command)
                putAll(extra)
            }
            val result = obsWebSocketClient.execute(payload)
            patch { copy(message = result.message) }
            if (result.ok) onSuccess?.invoke()
            patch { copy(working = false) }
        }
    }

    private suspend fun loadScenes() {
        val state = _uiState.value
        val response = obsWebSocketClient.request(state.url, state.password, "GetSceneList")
        val data = response["responseData"]?.jsonObject
        val sceneNames = data?.get("scenes")?.jsonArray?.mapNotNull {
            it.jsonObject["sceneName"]?.jsonPrimitive?.contentOrNull
        }.orEmpty()
        val current = data?.get("currentProgramSceneName")?.jsonPrimitive?.contentOrNull ?: sceneNames.firstOrNull()
        val preview = data?.get("currentPreviewSceneName")?.jsonPrimitive?.contentOrNull
        patch {
            copy(
                scenes = sceneNames.map { ObsSceneUi(name = it) },
                currentScene = current,
                currentPreviewScene = preview,
            )
        }
        loadScenePreviews(sceneNames)
    }

    private suspend fun loadInputs() {
        val state = _uiState.value
        val response = obsWebSocketClient.request(state.url, state.password, "GetInputList")
        val raw = response["responseData"]?.jsonObject?.get("inputs")?.jsonArray.orEmpty()
        val list = mutableListOf<ObsInputUi>()
        for (element in raw) {
            val obj = element.jsonObject
            val name = obj["inputName"]?.jsonPrimitive?.contentOrNull ?: continue
            val kind = obj["inputKind"]?.jsonPrimitive?.contentOrNull ?: "input"
            val muted = runCatching { queryInputMuted(name) }.getOrDefault(false)
            list += ObsInputUi(name, kind, muted)
        }
        patch { copy(inputs = list) }
    }

    private suspend fun queryInputMuted(inputName: String): Boolean {
        val state = _uiState.value
        val response = obsWebSocketClient.request(
            state.url,
            state.password,
            "GetInputMute",
            buildJsonObject { put("inputName", inputName) },
        )
        return response["responseData"]?.jsonObject?.get("inputMuted")?.jsonPrimitive?.booleanOrNull ?: false
    }

    private suspend fun refreshOutputStatuses() {
        loadStatuses()
        delay(400)
        loadStatuses()
    }

    private suspend fun loadStatuses() {
        val state = _uiState.value
        val stream = obsWebSocketClient.request(state.url, state.password, "GetStreamStatus")
        val recording = obsWebSocketClient.request(state.url, state.password, "GetRecordStatus")
        val streamData = stream["responseData"]?.jsonObject
        val recordData = recording["responseData"]?.jsonObject
        val streaming = streamData.readBool("outputActive")
        val rec = recordData.readBool("outputActive")
        val paused = recordData.readBool("outputPaused")
        val timecode = streamData?.get("outputTimecode")?.jsonPrimitive?.contentOrNull
        val recordTimecode = recordData?.get("outputTimecode")?.jsonPrimitive?.contentOrNull
        patch {
            copy(
                streaming = streaming,
                recording = rec,
                recordingPaused = paused && rec,
                streamTimecode = timecode,
                recordTimecode = recordTimecode,
            )
        }
    }

    private suspend fun loadStats() {
        val state = _uiState.value
        val stats = obsWebSocketClient.request(state.url, state.password, "GetStats")
        val data = stats["responseData"]?.jsonObject ?: return
        patch {
            copy(
                activeFps = data["activeFps"]?.jsonPrimitive?.doubleOrNull,
                obsCpuUsage = data["cpuUsage"]?.jsonPrimitive?.doubleOrNull,
                obsMemoryMb = data["memoryUsage"]?.jsonPrimitive?.doubleOrNull,
                outputSkippedFrames = data["outputSkippedFrames"]?.jsonPrimitive?.intOrNull,
                outputTotalFrames = data["outputTotalFrames"]?.jsonPrimitive?.intOrNull,
            )
        }
    }

    private suspend fun loadStreamServiceSettings() {
        val state = _uiState.value
        val response = obsWebSocketClient.request(state.url, state.password, "GetStreamServiceSettings")
        val data = response["responseData"]?.jsonObject ?: return
        val serviceType = data["streamServiceType"]?.jsonPrimitive?.contentOrNull
        val settings = data["streamServiceSettings"]?.jsonObject
        val serviceName = settings?.get("service")?.jsonPrimitive?.contentOrNull
        val server = settings?.get("server")?.jsonPrimitive?.contentOrNull
        val key = settings?.get("key")?.jsonPrimitive?.contentOrNull
        patch {
            copy(
                streamServiceType = serviceType,
                streamServiceName = serviceName,
                streamServer = server,
                streamKeyPreview = key?.let(::maskKey),
            )
        }
    }

    private suspend fun loadStudioMode() {
        val state = _uiState.value
        val response = obsWebSocketClient.request(state.url, state.password, "GetStudioModeEnabled")
        val enabled = response["responseData"]?.jsonObject?.get("studioModeEnabled")?.jsonPrimitive?.booleanOrNull ?: false
        patch { copy(studioModeEnabled = enabled) }
    }

    private suspend fun loadScenePreviews(sceneNames: List<String>) {
        val state = _uiState.value
        val previews = sceneNames.take(12).associateWith { sceneName ->
            runCatching {
                obsWebSocketClient.request(
                    state.url,
                    state.password,
                    "GetSourceScreenshot",
                    buildJsonObject {
                        put("sourceName", sceneName)
                        put("imageFormat", "jpg")
                        put("imageWidth", 640)
                        put("imageHeight", 360)
                        put("imageCompressionQuality", 72)
                    },
                )["responseData"]?.jsonObject?.get("imageData")?.jsonPrimitive?.contentOrNull
            }.getOrNull()
        }
        patch {
            copy(
                scenes = scenes.map { scene ->
                    scene.copy(previewImageData = previews[scene.name] ?: scene.previewImageData)
                },
            )
        }
    }

    private fun maskKey(value: String): String {
        if (value.isBlank()) return "empty"
        if (value.length <= 8) return "••••"
        return "${value.take(4)}••••${value.takeLast(4)}"
    }

    private fun persist() {
        viewModelScope.launch {
            val state = _uiState.value
            preferencesDataSource.setObsSettings(ObsConnectionSettings(state.url, state.password))
        }
    }

    private fun patch(block: ObsUiState.() -> ObsUiState) {
        _uiState.update(block)
    }
}

private fun JsonObject?.readBool(key: String): Boolean {
    val primitive = this?.get(key)?.jsonPrimitive ?: return false
    return primitive.booleanOrNull
        ?: when (primitive.contentOrNull?.lowercase()) {
            "true", "1" -> true
            "false", "0" -> false
            else -> primitive.intOrNull == 1
        }
}

data class ObsInputUi(
    val name: String,
    val kind: String,
    val muted: Boolean,
)

data class ObsSceneUi(
    val name: String,
    val previewImageData: String? = null,
)

data class ObsUiState(
    val url: String = "ws://127.0.0.1:4455",
    val password: String = "",
    val scenes: List<ObsSceneUi> = emptyList(),
    val inputs: List<ObsInputUi> = emptyList(),
    val streaming: Boolean = false,
    val recording: Boolean = false,
    val recordingPaused: Boolean = false,
    val currentScene: String? = null,
    val currentPreviewScene: String? = null,
    val streamTimecode: String? = null,
    val recordTimecode: String? = null,
    val studioModeEnabled: Boolean = false,
    val streamServiceType: String? = null,
    val streamServiceName: String? = null,
    val streamServer: String? = null,
    val streamKeyPreview: String? = null,
    val activeFps: Double? = null,
    val obsCpuUsage: Double? = null,
    val obsMemoryMb: Double? = null,
    val outputSkippedFrames: Int? = null,
    val outputTotalFrames: Int? = null,
    val streamChatSettings: StreamChatSettings = StreamChatSettings(),
    val working: Boolean = false,
    val message: String = "Ready",
)
