package com.streampanel.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkHelpersTest {
    @Test
    fun `an unauthorized answer says what to check and keeps the service's own words`() {
        val message = describeHttpFailure("Spotify", 401, """{"error":{"status":401,"message":"The access token expired"}}""")

        assertTrue(message.startsWith("Spotify answered HTTP 401"))
        assertTrue("token" in message)
        assertTrue("The access token expired" in message)
    }

    @Test
    fun `server errors blame the service not the user`() {
        assertTrue("on its side" in describeHttpFailure("Home Assistant", 503, ""))
    }

    @Test
    fun `an empty body adds no empty brackets`() {
        assertFalse("()" in describeHttpFailure("Discord", 404, "  "))
    }

    @Test
    fun `a long body is cut down`() {
        val message = describeHttpFailure("Hue", 500, "x".repeat(2000))

        assertTrue(message.length < 300)
    }

    @Test
    fun `only home network addresses are private`() {
        listOf("192.168.1.10", "10.0.0.5", "172.16.0.1", "172.31.255.255", "169.254.1.1", "philips-hue.local").forEach {
            assertTrue("$it should be private", isPrivateNetworkHost(it))
        }
        listOf("8.8.8.8", "172.32.0.1", "172.15.0.1", "192.169.1.1", "example.com", "1.2.3", "999.1.1.1", "").forEach {
            assertFalse("$it should NOT be private", isPrivateNetworkHost(it))
        }
    }
}