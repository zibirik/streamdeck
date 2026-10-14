package com.streampanel.core.integrations

import com.streampanel.core.network.LAN_DEVICE_CLIENT
import com.streampanel.core.network.describeHttpFailure
import com.streampanel.core.network.isPrivateNetworkHost
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class HueClient @Inject constructor(
    private val httpClient: HttpClient,
    @Named(LAN_DEVICE_CLIENT) private val lanDeviceClient: HttpClient,
) {
    suspend fun execute(payload: Map<String, String>): IntegrationResult = runCatching {
        val bridgeUrl = payload.required("bridgeUrl").trimEnd('/').let {
            if (it.startsWith("http://", true) || it.startsWith("https://", true)) it else "https://$it"
        }
        val appKey = payload.required("appKey")
        val resource = payload.required("resource")
        val resourceId = payload.required("resourceId")
        val body = payload["body"].orEmpty().ifBlank { "{}" }

        // The bridge's HTTPS certificate is self-signed. Skipping the check is only acceptable for a device in the home.
        val client = if (isPrivateNetworkHost(Url(bridgeUrl).host)) lanDeviceClient else httpClient

        val response = client.put("$bridgeUrl/clip/v2/resource/$resource/$resourceId") {
            timeout { requestTimeoutMillis = 10_000 }
            header("hue-application-key", appKey)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val text = response.bodyAsText()
        if (response.status.isSuccess()) {
            IntegrationResult.ok("Hue: done")
        } else {
            IntegrationResult.error(describeHttpFailure("The Hue bridge", response.status.value, text))
        }
    }.getOrElse { IntegrationResult.error(it.message ?: "Hue command failed") }
}