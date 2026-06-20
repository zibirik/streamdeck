package com.streampanel.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
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
    val topProcesses: List<PcProcessInfo> = emptyList(),
)

@Singleton
class PcStatusClient @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    suspend fun fetch(host: String, port: Int): PcServerStatus? = runCatching {
        val body = httpClient.get("http://$host:$port/status").bodyAsText()
        json.decodeFromString<PcServerStatus>(body)
    }.getOrNull()
}
