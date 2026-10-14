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
class StreamlabsClient @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun execute(payload: Map<String, String>): IntegrationResult = runCatching {
        val token = payload.required("token")
        val endpoint = payload.required("endpoint").trimStart('/')
        val body = payload["body"].orEmpty().ifBlank { "{}" }

        val response = httpClient.post("https://streamlabs.com/api/v2.0/$endpoint") {
            timeout { requestTimeoutMillis = 10_000 }
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val text = response.bodyAsText()
        if (response.status.isSuccess()) {
            IntegrationResult.ok("Streamlabs: $endpoint")
        } else {
            IntegrationResult.error(describeHttpFailure("Streamlabs", response.status.value, text))
        }
    }.getOrElse { IntegrationResult.error(it.message ?: "Streamlabs command failed") }
}