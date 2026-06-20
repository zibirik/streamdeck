package com.streampanel.core.integrations

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HueClient @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun execute(payload: Map<String, String>): IntegrationResult = runCatching {
        val bridgeUrl = payload.required("bridgeUrl").trimEnd('/')
        val appKey = payload.required("appKey")
        val resource = payload.required("resource")
        val resourceId = payload.required("resourceId")
        val body = payload["body"].orEmpty().ifBlank { "{}" }

        val response = httpClient.put("$bridgeUrl/clip/v2/resource/$resource/$resourceId") {
            header("hue-application-key", appKey)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.bodyAsText()

        IntegrationResult.ok("Hue command sent: ${response.take(160)}")
    }.getOrElse { IntegrationResult.error(it.message ?: "Hue command failed") }
}
