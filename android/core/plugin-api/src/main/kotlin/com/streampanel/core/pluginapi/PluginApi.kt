package com.streampanel.core.pluginapi

import com.streampanel.core.model.ControlAction
import com.streampanel.core.model.DashboardButton
import kotlinx.coroutines.flow.Flow

interface StreamPanelPlugin {
    val id: String
    val displayName: String
    val version: String
    fun actionProviders(): List<ActionProvider> = emptyList()
    fun buttonProviders(): List<ButtonProvider> = emptyList()
    fun settingsProviders(): List<SettingsProvider> = emptyList()
}

interface ActionProvider {
    val type: String
    val displayName: String
    suspend fun execute(action: ControlAction, context: PluginExecutionContext): PluginResult
}

interface ButtonProvider {
    val type: String
    fun observeState(button: DashboardButton): Flow<PluginButtonState>
}

interface SettingsProvider {
    val route: String
    val title: String
}

interface PluginExecutionContext {
    suspend fun sendPcCommand(type: String, payload: Map<String, String>): PluginResult
    suspend fun sendHttpRequest(payload: Map<String, String>): PluginResult
}

data class PluginButtonState(
    val label: String,
    val active: Boolean,
    val metadata: Map<String, String> = emptyMap(),
)

data class PluginResult(
    val ok: Boolean,
    val message: String,
)
