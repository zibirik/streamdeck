package com.streampanel.core.execution

import com.streampanel.core.model.ActionType
import com.streampanel.core.model.ControlAction
import com.streampanel.core.model.MacroProgram
import com.streampanel.core.model.MacroStep
import com.streampanel.core.datastore.PreferencesDataSource
import com.streampanel.core.integrations.DiscordWebhookClient
import com.streampanel.core.integrations.HomeAssistantClient
import com.streampanel.core.integrations.HueClient
import com.streampanel.core.integrations.IntegrationResult
import com.streampanel.core.integrations.MqttPublisher
import com.streampanel.core.integrations.ObsWebSocketClient
import com.streampanel.core.integrations.SpotifyClient
import com.streampanel.core.integrations.StreamlabsClient
import com.streampanel.core.network.HttpActionClient
import com.streampanel.core.network.PcCommand
import com.streampanel.core.network.PcCommandType
import com.streampanel.core.network.PcConnectionClient
import com.streampanel.core.network.RawSocketClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionExecutor @Inject constructor(
    private val pcConnectionClient: PcConnectionClient,
    private val httpActionClient: HttpActionClient,
    private val rawSocketClient: RawSocketClient,
    private val obsWebSocketClient: ObsWebSocketClient,
    private val homeAssistantClient: HomeAssistantClient,
    private val hueClient: HueClient,
    private val mqttPublisher: MqttPublisher,
    private val discordWebhookClient: DiscordWebhookClient,
    private val spotifyClient: SpotifyClient,
    private val streamlabsClient: StreamlabsClient,
    private val preferencesDataSource: PreferencesDataSource,
    private val json: Json,
) {
    suspend fun execute(actions: List<ControlAction>): ExecutionReport {
        val results = actions.sortedBy { it.sortOrder }.map { executeSingle(it) }
        return ExecutionReport(results = results)
    }

    private suspend fun executeSingle(action: ControlAction): ActionExecutionResult =
        when (action.type) {
            ActionType.OpenUrl -> sendPcCommand(action, PcCommandType.OPEN_URL, "url")
            ActionType.LaunchProcess -> sendPcCommand(action, PcCommandType.LAUNCH_PROCESS, "path")
            ActionType.SendText -> sendPcCommand(action, PcCommandType.SEND_TEXT, "text")
            ActionType.Hotkey -> sendPcCommand(action, PcCommandType.HOTKEY, "keys")
            ActionType.MouseCommand -> sendPcCommand(action, PcCommandType.MOUSE_COMMAND, "command")
            ActionType.MediaCommand -> sendPcCommand(action, PcCommandType.MEDIA_COMMAND, "action")
            ActionType.VolumeCommand -> sendPcCommand(action, PcCommandType.VOLUME_COMMAND, "action")
            ActionType.WindowCommand -> sendPcCommand(action, PcCommandType.WINDOW_COMMAND, "action")
            ActionType.SystemCommand -> sendPcCommand(action, PcCommandType.SYSTEM_COMMAND, "name")
            ActionType.OpenFolder -> sendPcCommand(action, PcCommandType.OPEN_FOLDER, "path")
            ActionType.OpenUrlGroup -> sendPcCommand(action, PcCommandType.OPEN_URLS, "urls")
            ActionType.Delay -> runDelay(action)
            ActionType.HttpRequest -> runHttp(action)
            ActionType.TcpPacket -> rawSocketClient.sendTcp(action.payload).let {
                ActionExecutionResult(action.id, it.ok, it.message)
            }
            ActionType.UdpPacket -> rawSocketClient.sendUdp(action.payload).let {
                ActionExecutionResult(action.id, it.ok, it.message)
            }
            ActionType.ObsCommand -> runObs(action)
            ActionType.HomeAssistant -> homeAssistantClient.callService(action.payload).asActionResult(action.id)
            ActionType.HueCommand -> hueClient.execute(action.payload).asActionResult(action.id)
            ActionType.Mqtt -> mqttPublisher.publish(action.payload).asActionResult(action.id)
            ActionType.DiscordWebhook -> discordWebhookClient.send(action.payload).asActionResult(action.id)
            ActionType.SpotifyCommand -> spotifyClient.execute(action.payload).asActionResult(action.id)
            ActionType.StreamlabsCommand -> streamlabsClient.execute(action.payload).asActionResult(action.id)
            ActionType.Sequence -> runMacro(action)
            ActionType.NavigatePage -> ActionExecutionResult(
                action.id,
                !action.payload["pageId"].isNullOrBlank(),
                if (action.payload["pageId"].isNullOrBlank()) "Missing pageId" else "Navigate to layer",
            )
            ActionType.WebSocketCommand,
            ActionType.Custom -> sendPcCommand(action, PcCommandType.CUSTOM, null)
        }

    private suspend fun sendPcCommand(
        action: ControlAction,
        commandType: PcCommandType,
        requiredKey: String?,
    ): ActionExecutionResult {
        if (requiredKey != null && action.payload[requiredKey].isNullOrBlank()) {
            return ActionExecutionResult(action.id, false, "Missing payload key: $requiredKey")
        }

        val response = pcConnectionClient.send(
            PcCommand(
                id = UUID.randomUUID().toString(),
                type = commandType,
                payload = action.payload,
                createdAtEpochMs = System.currentTimeMillis(),
            ),
        )
        return ActionExecutionResult(action.id, response.ok, response.message)
    }

    private suspend fun runDelay(action: ControlAction): ActionExecutionResult {
        val duration = action.payload["durationMs"]?.toLongOrNull()?.coerceIn(0, 60_000) ?: 250L
        delay(duration)
        return ActionExecutionResult(action.id, true, "Waited ${duration}ms")
    }

    private suspend fun runHttp(action: ControlAction): ActionExecutionResult {
        val result = httpActionClient.execute(action.payload)
        return ActionExecutionResult(action.id, result.ok, result.message)
    }

    private suspend fun runObs(action: ControlAction): ActionExecutionResult {
        val settings = preferencesDataSource.obsSettings.first()
        val payload = buildMap {
            put("url", settings.url)
            put("password", settings.password)
            putAll(action.payload)
        }
        return obsWebSocketClient.execute(payload).asActionResult(action.id)
    }

    private suspend fun runMacro(action: ControlAction): ActionExecutionResult {
        val programJson = action.payload["program"]
            ?: return ActionExecutionResult(action.id, false, "Missing macro program JSON")
        val program = runCatching { json.decodeFromString<MacroProgram>(programJson) }
            .getOrElse { return ActionExecutionResult(action.id, false, it.message ?: "Invalid macro program") }
        val context = MacroContext(program.variables.toMutableMap())
        val results = executeMacroSteps(program.steps, context)
        val report = ExecutionReport(results)
        return ActionExecutionResult(action.id, report.ok, report.message.ifBlank { "Macro ${program.name} executed" })
    }

    private suspend fun executeMacroSteps(
        steps: List<MacroStep>,
        context: MacroContext,
    ): List<ActionExecutionResult> {
        val results = mutableListOf<ActionExecutionResult>()
        for (step in steps) {
            when (step) {
                is MacroStep.Delay -> {
                    delay(step.durationMs.coerceIn(0, 60_000))
                    results += ActionExecutionResult("macro-delay", true, "Waited ${step.durationMs}ms")
                }
                is MacroStep.SetVariable -> {
                    context.variables[step.name] = step.value.resolveVariables(context.variables)
                    results += ActionExecutionResult("macro-variable-${step.name}", true, "Variable ${step.name} set")
                }
                is MacroStep.RunAction -> {
                    results += executeSingle(step.action.resolveVariables(context.variables))
                }
                is MacroStep.Condition -> {
                    val branch = if (context.variables[step.variable] == step.equals.resolveVariables(context.variables)) {
                        step.thenSteps
                    } else {
                        step.elseSteps
                    }
                    results += executeMacroSteps(branch, context)
                }
                is MacroStep.Loop -> {
                    repeat(step.count.coerceIn(0, 1_000)) {
                        results += executeMacroSteps(step.steps, context)
                    }
                }
                is MacroStep.Timer -> {
                    delay(step.delayMs.coerceIn(0, 24 * 60 * 60 * 1_000L))
                    results += executeMacroSteps(step.steps, context)
                }
            }
        }
        return results
    }

    private fun IntegrationResult.asActionResult(actionId: String): ActionExecutionResult =
        ActionExecutionResult(actionId, ok, message)

    private fun ControlAction.resolveVariables(variables: Map<String, String>): ControlAction =
        copy(payload = payload.mapValues { it.value.resolveVariables(variables) })

    private fun String.resolveVariables(variables: Map<String, String>): String =
        variables.entries.fold(this) { acc, (key, value) -> acc.replace("\${$key}", value) }
}

private data class MacroContext(
    val variables: MutableMap<String, String>,
)

data class ExecutionReport(
    val results: List<ActionExecutionResult>,
) {
    val ok: Boolean = results.all { it.ok }
    val message: String = results.joinToString(separator = "\n") { it.message }
}

data class ActionExecutionResult(
    val actionId: String,
    val ok: Boolean,
    val message: String,
)
