package com.streampanel.core.integrations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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

            Socket(host, port).use { socket ->
                socket.soTimeout = 5_000
                val output = socket.getOutputStream()
                val input = socket.getInputStream()
                output.write(connectPacket(clientId))
                output.flush()

                val connAck = ByteArray(4)
                val read = input.read(connAck)
                if (read < 4 || connAck[0] != 0x20.toByte() || connAck[3] != 0.toByte()) {
                    throw IllegalStateException("MQTT broker rejected connection.")
                }

                output.write(publishPacket(topic, message))
                output.write(byteArrayOf(0xE0.toByte(), 0x00))
                output.flush()
            }

            IntegrationResult.ok("MQTT message published to $topic.")
        }.getOrElse { IntegrationResult.error(it.message ?: "MQTT publish failed") }
    }

    private fun connectPacket(clientId: String): ByteArray {
        val variableHeader = ByteArrayOutputStream().apply {
            writeUtf("MQTT")
            write(4)
            write(0x02)
            write(byteArrayOf(0x00, 0x3C))
        }.toByteArray()

        val payload = ByteArrayOutputStream().apply {
            writeUtf(clientId)
        }.toByteArray()

        return fixedHeader(0x10, variableHeader.size + payload.size) + variableHeader + payload
    }

    private fun publishPacket(topic: String, message: String): ByteArray {
        val variableHeader = ByteArrayOutputStream().apply {
            writeUtf(topic)
        }.toByteArray()
        val messageBytes = message.toByteArray(StandardCharsets.UTF_8)
        return fixedHeader(0x30, variableHeader.size + messageBytes.size) + variableHeader + messageBytes
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
