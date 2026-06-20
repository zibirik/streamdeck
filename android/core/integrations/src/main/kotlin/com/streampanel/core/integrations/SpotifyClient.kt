package com.streampanel.core.integrations

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
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
        val query = deviceId?.let { "?device_id=$it" }.orEmpty()

        val response = when (command) {
            "play" -> httpClient.put("https://api.spotify.com/v1/me/player/play$query") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                payload["body"]?.takeIf(String::isNotBlank)?.let { setBody(it) }
            }.bodyAsText()
            "pause" -> httpClient.put("https://api.spotify.com/v1/me/player/pause$query") {
                bearerAuth(token)
            }.bodyAsText()
            "next" -> httpClient.post("https://api.spotify.com/v1/me/player/next$query") {
                bearerAuth(token)
            }.bodyAsText()
            "previous" -> httpClient.post("https://api.spotify.com/v1/me/player/previous$query") {
                bearerAuth(token)
            }.bodyAsText()
            "volume" -> {
                val percent = payload.required("volumePercent")
                httpClient.put("https://api.spotify.com/v1/me/player/volume?volume_percent=$percent") {
                    bearerAuth(token)
                }.bodyAsText()
            }
            else -> throw IllegalArgumentException("Unsupported Spotify command: $command")
        }

        IntegrationResult.ok("Spotify command executed: ${response.take(120)}")
    }.getOrElse { IntegrationResult.error(it.message ?: "Spotify command failed") }
}
