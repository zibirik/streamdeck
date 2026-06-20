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
class StreamlabsClient @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun execute(payload: Map<String, String>): IntegrationResult = runCatching {
        val token = payload.required("token")
        val endpoint = payload.required("endpoint").trimStart('/')
        val body = payload["body"].orEmpty().ifBlank { "{}" }

        val response = httpClient.post("https://streamlabs.com/api/v2.0/$endpoint") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.bodyAsText()

        IntegrationResult.ok("Streamlabs command sent: ${response.take(160)}")
    }.getOrElse { IntegrationResult.error(it.message ?: "Streamlabs command failed") }
}
