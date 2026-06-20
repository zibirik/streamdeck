package com.streampanel.core.pluginapi

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class PluginRegistry {
    private val _plugins = MutableStateFlow<List<StreamPanelPlugin>>(emptyList())
    val plugins: StateFlow<List<StreamPanelPlugin>> = _plugins

    val actionProviders: List<ActionProvider>
        get() = plugins.value.flatMap { it.actionProviders() }

    val buttonProviders: List<ButtonProvider>
        get() = plugins.value.flatMap { it.buttonProviders() }

    val settingsProviders: List<SettingsProvider>
        get() = plugins.value.flatMap { it.settingsProviders() }

    fun register(plugin: StreamPanelPlugin) {
        _plugins.update { current ->
            if (current.any { it.id == plugin.id }) current else current + plugin
        }
    }

    fun unregister(pluginId: String) {
        _plugins.update { current -> current.filterNot { it.id == pluginId } }
    }

    fun findActionProvider(type: String): ActionProvider? =
        actionProviders.firstOrNull { it.type == type }
}
