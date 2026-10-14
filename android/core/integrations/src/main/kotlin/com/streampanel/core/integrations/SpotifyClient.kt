package com.streampanel.core.integrations

import com.streampanel.core.network.describeHttpFailure
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyClient @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun execute(payload: Map<String, String>): IntegrationResult = runCatching {
        val token = payload.required("token")
        val command = payload.required("command").lowercase()
        val deviceId = payload["deviceId"]?.takeIf(String::isNotBlank)
        val query = deviceId?.let { "?device_id=${URLEncoder.encode(it, "UTF-8")}" }.orEmpty()

        val response: HttpResponse = when (command) {
            "play" -> httpClient.put("https://api.spotify.com/v1/me/player/play$query") {
                timeout { requestTimeoutMillis = 10_000 }
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                payload["body"]?.takeIf(String::isNotBlank)?.let { setBody(it) }
            }
            "pause" -> httpClient.put("https://api.spotify.com/v1/me/player/pause$query") {
                timeout { requestTimeoutMillis = 10_000 }
                bearerAuth(token)
            }
            "next" -> httpClient.post("https://api.spotify.com/v1/me/player/next$query") {
                timeout { requestTimeoutMillis = 10_000 }
                bearerAuth(token)
            }
            "previous" -> httpClient.post("https://api.spotify.com/v1/me/player/previous$query") {
                timeout { requestTimeoutMillis = 10_000 }
                bearerAuth(token)
            }
            "volume" -> {
                val percent = payload.required("volumePercent").toIntOrNull()?.coerceIn(0, 100)
                    ?: throw IllegalArgumentException("volumePercent must be a number from 0 to 100")
                httpClient.put("https://api.spotify.com/v1/me/player/volume?volume_percent=$percent") {
                    timeout { requestTimeoutMillis = 10_000 }
                    bearerAuth(token)
                }
            }
            else -> throw IllegalArgumentException("Unsupported Spotify command: $command")
        }

        if (response.status.isSuccess()) {
            IntegrationResult.ok("Spotify: $command")
        } else {
            val text = response.bodyAsText()
            val hint = if ("NO_ACTIVE_DEVICE" in text) " Open Spotify on any device and start playing once." else ""
            IntegrationResult.error(describeHttpFailure("Spotify", response.status.value, text) + hint)
        }
    }.getOrElse { IntegrationResult.error(it.message ?: "Spotify command failed") }
}