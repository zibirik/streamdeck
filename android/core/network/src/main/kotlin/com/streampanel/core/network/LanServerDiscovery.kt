package com.streampanel.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

data class DiscoveredServer(
    val host: String,
    val port: Int,
    val machineName: String,
)

@Singleton
class LanServerDiscovery @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    suspend fun discover(port: Int = 17820): List<DiscoveredServer> = withContext(Dispatchers.IO) {
        val prefix = localSubnetPrefix() ?: return@withContext emptyList()
        (1..254).map { hostSuffix ->
            async {
                val host = "$prefix$hostSuffix"
                probe(host, port)
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun probe(host: String, port: Int): DiscoveredServer? =
        withTimeoutOrNull(350) {
            runCatching {
                val body = httpClient.get("http://$host:$port/status").bodyAsText()
                val root = json.parseToJsonElement(body).jsonObject
                val name = root["name"]?.jsonPrimitive?.content
                if (name != "StreamPanel Server") return@runCatching null
                DiscoveredServer(
                    host = host,
                    port = port,
                    machineName = root["machineName"]?.jsonPrimitive?.content ?: host,
                )
            }.getOrNull()
        }

    private fun localSubnetPrefix(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
        for (iface in interfaces) {
            if (!iface.isUp || iface.isLoopback) continue
            for (addr in iface.inetAddresses) {
                if (addr is Inet4Address && !addr.isLoopbackAddress) {
                    val parts = addr.hostAddress?.split('.') ?: continue
                    if (parts.size == 4) return "${parts[0]}.${parts[1]}.${parts[2]}."
                }
            }
        }
        return null
    }
}
