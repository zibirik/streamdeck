package com.streampanel.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import com.streampanel.core.model.Cs2IntegrationInfo
import com.streampanel.core.model.GameTelemetryInfo
import com.streampanel.core.model.PcProcessInfo
import com.streampanel.core.model.PcStorageDrive
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class PcServerStatus(
    val volumePercent: Int? = null,
    val muted: Boolean? = null,
    val micMuted: Boolean? = null,
    val nowPlayingTitle: String? = null,
    val nowPlayingArtist: String? = null,
    val foregroundProcess: String? = null,
    val foregroundTitle: String? = null,
    val cpuPercent: Double? = null,
    val ramPercent: Double? = null,
    val clipboardPreview: String? = null,
    val activeClients: Int? = null,
    val mediaPlaying: Boolean? = null,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val networkInterface: String? = null,
    val diskFreePercent: Double? = null,
    val storageDrives: List<PcStorageDrive> = emptyList(),
    val gameInfo: GameTelemetryInfo = GameTelemetryInfo(),
    val cs2: Cs2IntegrationInfo = Cs2IntegrationInfo(),
    val topProcesses: List<PcProcessInfo> = emptyList(),
)

@Singleton
class PcStatusClient @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    /** Asks the PC to put the Counter-Strike 2 config file where the game reads it. */
    suspend fun installCs2(host: String, port: Int, pin: String = ""): Cs2InstallOutcome = runCatching {
        val response = httpClient.post("http://$host:$port/integrations/cs2/install") {
            if (pin.isNotBlank()) header("X-StreamPanel-Pin", pin)
        }
        val body = response.bodyAsText()
        val code = Regex("\"code\"\\s*:\\s*\"([a-z_]+)\"").find(body)?.groupValues?.getOrNull(1)
        when {
            response.status.isSuccess() -> Cs2InstallOutcome.Installed
            code == "cs2_not_found" -> Cs2InstallOutcome.GameNotFound
            response.status.value == 401 -> Cs2InstallOutcome.WrongPin
            else -> Cs2InstallOutcome.Failed(body.take(200))
        }
    }.getOrElse { Cs2InstallOutcome.Failed(it.message.orEmpty()) }
    /** [pin] is sent when the PC server has a PIN configured; it is ignored otherwise. */
    suspend fun fetch(host: String, port: Int, pin: String = ""): PcServerStatus? = runCatching {
        val response = httpClient.get("http://$host:$port/status") {
            if (pin.isNotBlank()) header("X-StreamPanel-Pin", pin)
        }
        if (!response.status.isSuccess()) return@runCatching null
        json.decodeFromString<PcServerStatus>(response.bodyAsText())
    }.getOrNull()
}

/** What came of asking the PC to set up Counter-Strike. */
sealed interface Cs2InstallOutcome {
    data object Installed : Cs2InstallOutcome
    data object GameNotFound : Cs2InstallOutcome
    data object WrongPin : Cs2InstallOutcome
    data class Failed(val reason: String) : Cs2InstallOutcome
}