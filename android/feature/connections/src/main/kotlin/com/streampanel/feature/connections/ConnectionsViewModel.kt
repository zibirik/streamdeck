package com.streampanel.feature.connections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streampanel.core.datastore.PreferencesDataSource
import com.streampanel.core.model.ServerConnectionSettings
import com.streampanel.core.network.ConnectionStatus
import com.streampanel.core.network.DiscoveredServer
import com.streampanel.core.network.LanServerDiscovery
import com.streampanel.core.network.PcConnectionClient
import com.streampanel.core.network.PcConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectionsViewModel @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource,
    private val pcConnectionClient: PcConnectionClient,
    private val connectionManager: PcConnectionManager,
    private val lanServerDiscovery: LanServerDiscovery,
) : ViewModel() {
    private val hostDraft = MutableStateFlow("")
    private val portDraft = MutableStateFlow("")
    private val pinDraft = MutableStateFlow("")
    private val discovered = MutableStateFlow<List<DiscoveredServer>>(emptyList())
    private val scanning = MutableStateFlow(false)

    val uiState = combine(
        combine(
            preferencesDataSource.serverConnection,
            pcConnectionClient.status,
            hostDraft,
            portDraft,
            pinDraft,
        ) { settings, status, host, port, pin ->
            Quintuple(settings, status, host, port, pin)
        },
        discovered,
        scanning,
    ) { quintuple, servers, isScanning ->
        val settings = quintuple.first
        val status = quintuple.second
        val host = quintuple.third
        val port = quintuple.fourth
        val pin = quintuple.fifth
        ConnectionsUiState(
            settings = settings,
            status = status,
            hostDraft = host.ifBlank { settings.host },
            portDraft = port.ifBlank { settings.port.toString() },
            pinDraft = pin.ifBlank { settings.pin },
            discoveredServers = servers,
            scanning = isScanning,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ConnectionsUiState(),
    )

    fun updateHost(value: String) { hostDraft.value = value }
    fun updatePort(value: String) { portDraft.value = value.filter(Char::isDigit) }
    fun updatePin(value: String) { pinDraft.value = value }

    fun saveAndConnect() = viewModelScope.launch {
        val settings = buildSettings(autoConnect = true)
        preferencesDataSource.setServerConnection(settings)
        connectionManager.connect(settings)
    }

    fun connectWithoutSave() = viewModelScope.launch {
        connectionManager.connect(buildSettings())
    }

    fun disconnect() = viewModelScope.launch {
        connectionManager.disconnect()
    }

    fun scanNetwork() = viewModelScope.launch {
        scanning.value = true
        discovered.value = lanServerDiscovery.discover(uiState.value.portDraft.toIntOrNull() ?: 17820)
        scanning.value = false
    }

    fun selectDiscovered(server: DiscoveredServer) {
        hostDraft.value = server.host
        portDraft.value = server.port.toString()
    }

    private fun buildSettings(autoConnect: Boolean? = null) = ServerConnectionSettings(
        host = uiState.value.hostDraft,
        port = uiState.value.portDraft.toIntOrNull() ?: 17820,
        autoConnect = autoConnect ?: uiState.value.settings.autoConnect,
        pin = uiState.value.pinDraft,
    )
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
)

data class ConnectionsUiState(
    val settings: ServerConnectionSettings = ServerConnectionSettings(),
    val status: ConnectionStatus = ConnectionStatus.Disconnected,
    val hostDraft: String = "",
    val portDraft: String = "17820",
    val pinDraft: String = "",
    val discoveredServers: List<DiscoveredServer> = emptyList(),
    val scanning: Boolean = false,
)
