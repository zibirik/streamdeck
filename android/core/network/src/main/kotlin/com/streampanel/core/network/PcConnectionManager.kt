package com.streampanel.core.network

import com.streampanel.core.datastore.PreferencesDataSource
import com.streampanel.core.model.ServerConnectionSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PcConnectionManager @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource,
    private val pcConnectionClient: PcConnectionClient,
) {
    private var autoConnectStarted = false

    fun startAutoConnect(scope: CoroutineScope) {
        if (autoConnectStarted) return
        autoConnectStarted = true
        scope.launch(Dispatchers.IO) {
            runCatching {
                preferencesDataSource.serverConnection
                    .distinctUntilChanged()
                    .collect { settings ->
                        if (settings.autoConnect) {
                            runCatching { connect(settings) }
                        }
                    }
            }
        }
    }

    suspend fun connect(settings: ServerConnectionSettings? = null) = withContext(Dispatchers.IO) {
        val resolved = settings ?: preferencesDataSource.serverConnection.first()
        pcConnectionClient.connect(resolved.websocketUrl)
        if (resolved.pin.isNotBlank() && pcConnectionClient.status.value is ConnectionStatus.Connected) {
            val auth = pcConnectionClient.send(
                PcCommand(
                    id = UUID.randomUUID().toString(),
                    type = PcCommandType.CUSTOM,
                    payload = mapOf("action" to "auth", "pin" to resolved.pin),
                    createdAtEpochMs = System.currentTimeMillis(),
                ),
            )
            if (!auth.ok) {
                pcConnectionClient.disconnect()
            }
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        pcConnectionClient.disconnect()
    }
}
