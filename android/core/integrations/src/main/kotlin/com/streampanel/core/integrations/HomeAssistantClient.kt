package com.streampanel.core.integrations

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeAssistantClient @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun callService(payload: Map<String, String>): IntegrationResult = runCatching {
        val baseUrl = payload.required("baseUrl").trimEnd('/')
        val token = payload.required("token")
        val domain = payload.required("domain")
        val service = payload.required("service")
        val body = payload["body"].orEmpty().ifBlank { "{}" }

        val response = httpClient.post("$baseUrl/api/services/$domain/$service") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.bodyAsText()

        IntegrationResult.ok("Home Assistant service called: ${response.take(160)}")
    }.getOrElse { IntegrationResult.error(it.message ?: "Home Assistant call failed") }
}
