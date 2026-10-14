package com.streampanel.core.integrations

import com.streampanel.core.network.describeHttpFailure
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
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
            timeout { requestTimeoutMillis = 10_000 }
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val text = response.bodyAsText()
        if (response.status.isSuccess()) {
            IntegrationResult.ok("Home Assistant: $domain.$service")
        } else {
            IntegrationResult.error(describeHttpFailure("Home Assistant", response.status.value, text))
        }
    }.getOrElse { IntegrationResult.error(it.message ?: "Home Assistant call failed") }
}