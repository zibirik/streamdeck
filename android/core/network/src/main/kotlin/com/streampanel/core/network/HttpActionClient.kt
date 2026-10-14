package com.streampanel.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpActionClient @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun execute(payload: Map<String, String>): HttpActionResult {
        val url = payload["url"].orEmpty()
        if (url.isBlank()) return HttpActionResult(false, "Missing URL")
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            return HttpActionResult(false, "The address must start with http:// or https://")
        }

        val method = payload["method"]?.uppercase().orEmpty().ifBlank { "GET" }
        val body = payload["body"].orEmpty()
        val headers = payload
            .filterKeys { it.startsWith("header.") }
            .mapKeys { it.key.removePrefix("header.") }

        return try {
            val response: HttpResponse = when (method) {
                "POST" -> httpClient.post(url) {
                    timeout { requestTimeoutMillis = REQUEST_TIMEOUT_MS }
                    headers.forEach { (key, value) -> header(key, value) }
                    if (body.isNotBlank()) setBody(body)
                }
                "PUT" -> httpClient.put(url) {
                    timeout { requestTimeoutMillis = REQUEST_TIMEOUT_MS }
                    headers.forEach { (key, value) -> header(key, value) }
                    if (body.isNotBlank()) setBody(body)
                }
                "PATCH" -> httpClient.patch(url) {
                    timeout { requestTimeoutMillis = REQUEST_TIMEOUT_MS }
                    headers.forEach { (key, value) -> header(key, value) }
                    if (body.isNotBlank()) setBody(body)
                }
                "DELETE" -> httpClient.delete(url) {
                    timeout { requestTimeoutMillis = REQUEST_TIMEOUT_MS }
                    headers.forEach { (key, value) -> header(key, value) }
                }
                else -> httpClient.get(url) {
                    timeout { requestTimeoutMillis = REQUEST_TIMEOUT_MS }
                    headers.forEach { (key, value) -> header(key, value) }
                }
            }
            val text = response.bodyAsText()
            if (response.status.isSuccess()) {
                HttpActionResult(true, if (text.isBlank()) "HTTP ${response.status.value}" else "HTTP ${response.status.value}: ${text.take(240)}")
            } else {
                HttpActionResult(false, describeHttpFailure("The server", response.status.value, text))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (throwable: Throwable) {
            HttpActionResult(false, throwable.message ?: "The request failed")
        }
    }

    private companion object {
        const val REQUEST_TIMEOUT_MS = 15_000L
    }
}

data class HttpActionResult(
    val ok: Boolean,
    val message: String,
)