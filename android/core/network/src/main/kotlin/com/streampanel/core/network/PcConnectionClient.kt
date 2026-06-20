package com.streampanel.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface PcConnectionClient {
    val status: StateFlow<ConnectionStatus>
    suspend fun connect(url: String)
    suspend fun disconnect()
    suspend fun send(command: PcCommand): PcCommandResponse
}

@Singleton
class KtorPcConnectionClient @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
) : PcConnectionClient {
    private val mutex = Mutex()
    private var session: WebSocketSession? = null
    private var currentUrl: String? = null
    private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)

    override val status: StateFlow<ConnectionStatus> = _status

    override suspend fun connect(url: String) = mutex.withLock {
        if (session?.isActive == true && currentUrl == url) return@withLock
        _status.value = ConnectionStatus.Connecting
        try {
            session?.close()
            session = httpClient.webSocketSession(url)
            currentUrl = url
            _status.value = ConnectionStatus.Connected(url)
        } catch (throwable: Throwable) {
            session = null
            currentUrl = null
            _status.value = ConnectionStatus.Failed(throwable.message ?: "Connection failed")
        }
    }

    override suspend fun disconnect() = mutex.withLock {
        session?.close()
        session = null
        currentUrl = null
        _status.value = ConnectionStatus.Disconnected
    }

    override suspend fun send(command: PcCommand): PcCommandResponse = mutex.withLock {
        val activeSession = session
            ?: return@withLock PcCommandResponse(
                id = command.id,
                ok = false,
                message = "PC server is not connected",
                completedAtEpochMs = System.currentTimeMillis(),
            )

        return@withLock try {
            activeSession.send(Frame.Text(json.encodeToString(command)))
            val responseFrame = withTimeout(5_000) { activeSession.incoming.receive() }
            val responseText = (responseFrame as? Frame.Text)?.readText().orEmpty()
            json.decodeFromString<PcCommandResponse>(responseText)
        } catch (throwable: Throwable) {
            _status.value = ConnectionStatus.Failed(throwable.message ?: "Command failed")
            PcCommandResponse(
                id = command.id,
                ok = false,
                message = throwable.message ?: "Command failed",
                completedAtEpochMs = System.currentTimeMillis(),
            )
        }
    }
}
