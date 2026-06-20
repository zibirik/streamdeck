package com.streampanel.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpActionClient @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun execute(payload: Map<String, String>): HttpActionResult {
        val url = payload["url"].orEmpty()
        if (url.isBlank()) return HttpActionResult(false, "Missing URL")

        val method = payload["method"]?.uppercase().orEmpty().ifBlank { "GET" }
        val body = payload["body"].orEmpty()
        val headers = payload
            .filterKeys { it.startsWith("header.") }
            .mapKeys { it.key.removePrefix("header.") }

        val responseText = when (method) {
            "POST" -> httpClient.post(url) {
                headers.forEach { (key, value) -> header(key, value) }
                if (body.isNotBlank()) setBody(body)
            }.bodyAsText()
            "PUT" -> httpClient.put(url) {
                headers.forEach { (key, value) -> header(key, value) }
                if (body.isNotBlank()) setBody(body)
            }.bodyAsText()
            "PATCH" -> httpClient.patch(url) {
                headers.forEach { (key, value) -> header(key, value) }
                if (body.isNotBlank()) setBody(body)
            }.bodyAsText()
            "DELETE" -> httpClient.delete(url) {
                headers.forEach { (key, value) -> header(key, value) }
            }.bodyAsText()
            else -> httpClient.get(url) {
                headers.forEach { (key, value) -> header(key, value) }
            }.bodyAsText()
        }

        return HttpActionResult(true, responseText.take(240))
    }
}

data class HttpActionResult(
    val ok: Boolean,
    val message: String,
)
