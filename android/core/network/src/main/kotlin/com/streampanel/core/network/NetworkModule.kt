package com.streampanel.core.network

import dagger.Module
import dagger.Binds
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.security.cert.X509Certificate
import javax.inject.Named
import javax.inject.Singleton
import javax.net.ssl.X509TrustManager

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindingModule {
    @Binds
    @Singleton
    abstract fun bindPcConnectionClient(client: KtorPcConnectionClient): PcConnectionClient
}

/** Name of the client that accepts the self-signed certificates of devices on the home network (Philips Hue). */
const val LAN_DEVICE_CLIENT = "lan-device"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient = HttpClient(CIO) {
        install(WebSockets)
        install(ContentNegotiation) {
            json(json)
        }
        // Nobody should wait forever for a PC that is off. Per-request limits are set where they matter.
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
        }
    }

    /**
     * Home devices (a Hue bridge) serve HTTPS with a certificate nobody can verify. This client skips that check, so it
     * must only ever be used for addresses on the local network (see [isPrivateNetworkHost]).
     */
    @Provides
    @Singleton
    @Named(LAN_DEVICE_CLIENT)
    fun provideLanDeviceClient(): HttpClient = HttpClient(CIO) {
        engine {
            https {
                trustManager = object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) = Unit
                    override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) = Unit
                    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
                }
            }
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
        }
    }
}

/** True for addresses that never leave the home: 10.x, 172.16-31.x, 192.168.x, 169.254.x and `.local` names. */
fun isPrivateNetworkHost(host: String): Boolean {
    val name = host.trim().lowercase()
    if (name.endsWith(".local")) return true
    val parts = name.split('.')
    if (parts.size != 4) return false
    val octets = parts.map { it.toIntOrNull() ?: return false }
    if (octets.any { it !in 0..255 }) return false
    return when {
        octets[0] == 10 -> true
        octets[0] == 172 && octets[1] in 16..31 -> true
        octets[0] == 192 && octets[1] == 168 -> true
        octets[0] == 169 && octets[1] == 254 -> true
        else -> false
    }
}