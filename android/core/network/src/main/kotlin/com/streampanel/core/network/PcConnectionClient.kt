package com.streampanel.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
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
    /** Closes the connection. A non-null [failureMessage] is shown as a connection error instead of plain "disconnected". */
    suspend fun disconnect(failureMessage: String? = null)
    suspend fun send(command: PcCommand, timeoutMs: Long = DEFAULT_COMMAND_TIMEOUT_MS): PcCommandResponse
}

/** Most commands finish at once; programs the PC runs for us (git, docker) pass a longer one. */
const val DEFAULT_COMMAND_TIMEOUT_MS = 8_000L

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

    override suspend fun disconnect(failureMessage: String?) = mutex.withLock {
        session?.close()
        session = null
        currentUrl = null
        _status.value = failureMessage?.let { ConnectionStatus.Failed(it) } ?: ConnectionStatus.Disconnected
    }

    override suspend fun send(command: PcCommand, timeoutMs: Long): PcCommandResponse = mutex.withLock {
        val activeSession = session
            ?: return@withLock PcCommandResponse(
                id = command.id,
                ok = false,
                message = "PC server is not connected",
                completedAtEpochMs = System.currentTimeMillis(),
            )

        return@withLock try {
            activeSession.send(Frame.Text(json.encodeToString(command)))
            withTimeout(timeoutMs) { receiveResponseFor(activeSession, command.id) }
        } catch (timeout: TimeoutCancellationException) {
            // A slow command is not a broken connection. Its late answer is skipped by id next time.
            PcCommandResponse(
                id = command.id,
                ok = false,
                message = "The PC did not answer in time",
                completedAtEpochMs = System.currentTimeMillis(),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
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

    /** Reads answers until the one for [id]; answers to earlier commands that timed out are thrown away. */
    private suspend fun receiveResponseFor(session: WebSocketSession, id: String): PcCommandResponse {
        while (true) {
            val text = (session.incoming.receive() as? Frame.Text)?.readText() ?: continue
            val response = json.decodeFromString<PcCommandResponse>(text)
            if (response.id == id) return response
        }
    }
}