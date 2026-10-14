package com.streampanel.core.integrations

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MqttPacketsTest {
    @Test
    fun `a plain connect packet matches the MQTT 3_1_1 layout byte for byte`() {
        val packet = MqttPackets.connect("sp")

        val expected = byteArrayOf(
            0x10, 0x0E, // CONNECT, 14 bytes follow
            0x00, 0x04, 'M'.code.toByte(), 'Q'.code.toByte(), 'T'.code.toByte(), 'T'.code.toByte(),
            0x04, // protocol level
            0x02, // clean session
            0x00, 0x3C, // keep alive
            0x00, 0x02, 's'.code.toByte(), 'p'.code.toByte(),
        )
        assertArrayEquals(expected, packet)
    }

    @Test
    fun `user name and password turn on their flags and travel after the client id`() {
        val packet = MqttPackets.connect("c", "user", "pass")

        assertEquals(0xC2.toByte(), packet[9]) // username + password + clean session
        val text = String(packet, Charsets.ISO_8859_1)
        assertTrue(text.indexOf("c") < text.indexOf("user"))
        assertTrue(text.indexOf("user") < text.indexOf("pass"))
    }

    @Test
    fun `a password without a user name is not sent because the protocol forbids it`() {
        val packet = MqttPackets.connect("c", null, "secret")

        assertEquals(0x02.toByte(), packet[9])
        assertTrue("secret" !in String(packet, Charsets.ISO_8859_1))
    }

    @Test
    fun `publish packets carry the retain flag when asked`() {
        assertEquals(0x30.toByte(), MqttPackets.publish("t", "m")[0])
        assertEquals(0x31.toByte(), MqttPackets.publish("t", "m", retain = true)[0])
    }

    @Test
    fun `long messages use more than one length byte`() {
        val packet = MqttPackets.publish("t", "x".repeat(300))

        // 2 + 1 + 300 = 303 = 0x12F -> 0xAF 0x02
        assertEquals(0xAF.toByte(), packet[1])
        assertEquals(0x02.toByte(), packet[2])
    }

    @Test
    fun `broker answers are explained in words`() {
        assertNull(MqttPackets.connectFailure(byteArrayOf(0x20, 0x02, 0x00, 0x00)))
        assertTrue("password" in MqttPackets.connectFailure(byteArrayOf(0x20, 0x02, 0x00, 0x04))!!)
        assertTrue("not allowed" in MqttPackets.connectFailure(byteArrayOf(0x20, 0x02, 0x00, 0x05))!!)
        assertNotNull(MqttPackets.connectFailure(byteArrayOf(0x00, 0x00, 0x00, 0x00)))
    }
}