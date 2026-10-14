package com.streampanel.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RawSocketClient @Inject constructor() {
    suspend fun sendTcp(payload: Map<String, String>): RawSocketResult = withContext(Dispatchers.IO) {
        runCatching {
            val host = payload.required("host")
            val port = payload.required("port").toPort()
            val message = payload.required("message")
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
                socket.getOutputStream().write(message.toByteArray())
                socket.getOutputStream().flush()
            }
            RawSocketResult(true, "TCP packet sent to $host:$port")
        }.getOrElse { RawSocketResult(false, it.message ?: "TCP send failed") }
    }

    suspend fun sendUdp(payload: Map<String, String>): RawSocketResult = withContext(Dispatchers.IO) {
        runCatching {
            val host = payload.required("host")
            val port = payload.required("port").toPort()
            val message = payload.required("message").toByteArray()
            DatagramSocket().use { socket ->
                socket.send(DatagramPacket(message, message.size, InetAddress.getByName(host), port))
            }
            RawSocketResult(true, "UDP packet sent to $host:$port")
        }.getOrElse { RawSocketResult(false, it.message ?: "UDP send failed") }
    }

    private fun String.toPort(): Int =
        toIntOrNull()?.takeIf { it in 1..65535 } ?: throw IllegalArgumentException("Port must be a number from 1 to 65535, got '$this'")

    private fun Map<String, String>.required(key: String): String =
        this[key]?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Missing payload key: $key")

    private companion object {
        const val CONNECT_TIMEOUT_MS = 5_000
    }
}

data class RawSocketResult(
    val ok: Boolean,
    val message: String,
)