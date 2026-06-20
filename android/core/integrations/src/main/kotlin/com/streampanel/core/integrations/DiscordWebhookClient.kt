package com.streampanel.core.integrations

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscordWebhookClient @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun send(payload: Map<String, String>): IntegrationResult = runCatching {
        val webhookUrl = payload.required("webhookUrl")
        val content = payload.required("content")
        val username = payload["username"]

        val response = httpClient.post(webhookUrl) {
            contentType(ContentType.Application.Json)
            setBody(DiscordWebhookBody(content = content, username = username))
        }.bodyAsText()

        IntegrationResult.ok("Discord webhook sent: ${response.take(120)}")
    }.getOrElse { IntegrationResult.error(it.message ?: "Discord webhook failed") }
}

@Serializable
private data class DiscordWebhookBody(
    val content: String,
    val username: String? = null,
)
