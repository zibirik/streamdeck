package com.streampanel.core.integrations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MqttPublisher @Inject constructor() {
    suspend fun publish(payload: Map<String, String>): IntegrationResult = withContext(Dispatchers.IO) {
        runCatching {
            val host = payload.required("host")
            val port = payload["port"]?.toIntOrNull() ?: 1883
            val clientId = payload["clientId"].orEmpty().ifBlank { "StreamPanel-${System.currentTimeMillis()}" }
            val topic = payload.required("topic")
            val message = payload.required("message")
            val username = payload["username"]?.takeIf(String::isNotBlank)
            val password = payload["password"]?.takeIf(String::isNotBlank)
            val retain = payload["retain"].equals("true", ignoreCase = true)

            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
                socket.soTimeout = 5_000
                val output = socket.getOutputStream()
                val input = DataInputStream(socket.getInputStream())

                output.write(MqttPackets.connect(clientId, username, password))
                output.flush()

                // CONNACK is always four bytes: type, length, session flag, return code.
                val connAck = ByteArray(4)
                input.readFully(connAck)
                MqttPackets.connectFailure(connAck)?.let { throw IllegalStateException(it) }

                output.write(MqttPackets.publish(topic, message, retain))
                output.write(MqttPackets.DISCONNECT)
                output.flush()
            }

            IntegrationResult.ok("MQTT message published to $topic.")
        }.getOrElse { IntegrationResult.error(it.message ?: "MQTT publish failed") }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 5_000
    }
}

/** The few MQTT 3.1.1 packets needed to say one thing to a broker. Kept apart so they can be checked byte by byte. */
internal object MqttPackets {
    val DISCONNECT = byteArrayOf(0xE0.toByte(), 0x00)

    private const val FLAG_CLEAN_SESSION = 0x02
    private const val FLAG_PASSWORD = 0x40
    private const val FLAG_USERNAME = 0x80

    fun connect(clientId: String, username: String? = null, password: String? = null): ByteArray {
        // A password cannot exist without a user name in MQTT 3.1.1, so a lone password is ignored.
        val sendPassword = username != null && password != null
        var flags = FLAG_CLEAN_SESSION
        if (username != null) flags = flags or FLAG_USERNAME
        if (sendPassword) flags = flags or FLAG_PASSWORD

        val variableHeader = ByteArrayOutputStream().apply {
            writeUtf("MQTT")
            write(4) // protocol level 3.1.1
            write(flags)
            write(byteArrayOf(0x00, 0x3C)) // keep alive: 60 s
        }.toByteArray()

        val payload = ByteArrayOutputStream().apply {
            writeUtf(clientId)
            if (username != null) writeUtf(username)
            if (sendPassword) writeUtf(password!!)
        }.toByteArray()

        return fixedHeader(0x10, variableHeader.size + payload.size) + variableHeader + payload
    }

    fun publish(topic: String, message: String, retain: Boolean = false): ByteArray {
        val variableHeader = ByteArrayOutputStream().apply { writeUtf(topic) }.toByteArray()
        val messageBytes = message.toByteArray(StandardCharsets.UTF_8)
        val type = if (retain) 0x31 else 0x30
        return fixedHeader(type, variableHeader.size + messageBytes.size) + variableHeader + messageBytes
    }

    /** Null when the broker accepted us; otherwise the reason, in words. */
    fun connectFailure(connAck: ByteArray): String? {
        if (connAck.size < 4 || connAck[0] != 0x20.toByte()) return "The broker did not answer like an MQTT broker."
        return when (connAck[3].toInt()) {
            0 -> null
            1 -> "The broker does not speak this MQTT version."
            2 -> "The broker refused the client id."
            3 -> "The MQTT broker is not available."
            4 -> "Wrong MQTT user name or password."
            5 -> "The MQTT user is not allowed to connect. Check the user name and password."
            else -> "The broker refused the connection (code ${connAck[3]})."
        }
    }

    private fun fixedHeader(type: Int, remainingLength: Int): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(type)
        var value = remainingLength
        do {
            var encodedByte = value % 128
            value /= 128
            if (value > 0) encodedByte = encodedByte or 128
            out.write(encodedByte)
        } while (value > 0)
        return out.toByteArray()
    }

    private fun ByteArrayOutputStream.writeUtf(value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        write((bytes.size shr 8) and 0xFF)
        write(bytes.size and 0xFF)
        write(bytes)
    }
}