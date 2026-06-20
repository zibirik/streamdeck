package com.streampanel.core.integrations

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObsWebSocketClient @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    suspend fun execute(payload: Map<String, String>): IntegrationResult = runCatching {
        val url = payload.required("url")
        val password = payload["password"].orEmpty()
        val command = payload.required("command")
        val requestData = payload.toObsRequestData(command)

        val response = request(url, password, command, requestData)
        val status = response["requestStatus"]?.jsonObject
        val success = status?.get("result")?.jsonPrimitive?.boolean ?: false
        val comment = status?.get("comment")?.jsonPrimitive?.contentOrNull

        if (success) {
            IntegrationResult.ok("OBS command executed: $command")
        } else {
            IntegrationResult.error(obsErrorMessage(command, comment ?: "OBS rejected command: $command"))
        }
    }.getOrElse { throwable ->
        IntegrationResult.error(obsErrorMessage(command = payload["command"].orEmpty(), raw = throwable.message ?: "OBS command failed"))
    }

    suspend fun request(
        url: String,
        password: String,
        requestType: String,
        requestData: JsonObject = buildJsonObject {},
    ): JsonObject {
        val session = httpClient.webSocketSession(url)
        try {
            val hello = receiveJson(session) { it["op"]?.jsonPrimitive?.int == 0 }
            val helloData = hello["d"]?.jsonObject ?: buildJsonObject {}
            val authentication = helloData["authentication"]?.jsonObject
            val identifyData = buildJsonObject {
                put("rpcVersion", 1)
                if (authentication != null) {
                    put(
                        "authentication",
                        createAuthentication(
                            password = password,
                            salt = authentication["salt"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                            challenge = authentication["challenge"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        ),
                    )
                }
            }
            session.send(Frame.Text(json.encodeToString(JsonObject.serializer(), envelope(1, identifyData))))
            receiveJson(session) { it["op"]?.jsonPrimitive?.int == 2 }

            val requestId = UUID.randomUUID().toString()
            session.send(
                Frame.Text(
                    json.encodeToString(
                        JsonObject.serializer(),
                        envelope(
                            op = 6,
                            d = buildJsonObject {
                                put("requestType", requestType)
                                put("requestId", requestId)
                                put("requestData", requestData)
                            },
                        ),
                    ),
                ),
            )

            val response = receiveJson(session) {
                it["op"]?.jsonPrimitive?.int == 7 &&
                    it["d"]?.jsonObject?.get("requestId")?.jsonPrimitive?.contentOrNull == requestId
            }
            return response["d"]?.jsonObject ?: buildJsonObject {}
        } finally {
            session.close()
        }
    }

    private suspend fun receiveJson(
        session: io.ktor.websocket.WebSocketSession,
        predicate: (JsonObject) -> Boolean,
    ): JsonObject = withTimeout(8_000) {
        while (true) {
            val frame = session.incoming.receive()
            val text = (frame as? Frame.Text)?.readText() ?: continue
            val parsed = json.parseToJsonElement(text).jsonObject
            if (predicate(parsed)) return@withTimeout parsed
        }
        error("Unreachable")
    }

    private fun envelope(op: Int, d: JsonObject): JsonObject =
        buildJsonObject {
            put("op", op)
            put("d", d)
        }

    private fun createAuthentication(password: String, salt: String, challenge: String): String {
        val secret = sha256Base64(password + salt)
        return sha256Base64(secret + challenge)
    }

    private fun sha256Base64(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return Base64.getEncoder().encodeToString(digest)
    }
}

private fun Map<String, String>.toObsRequestData(command: String): JsonObject =
    when (command) {
        "SetCurrentProgramScene" -> buildJsonObject {
            put("sceneName", required("sceneName"))
        }
        "SetCurrentPreviewScene" -> buildJsonObject {
            put("sceneName", required("sceneName"))
        }
        "SetStreamServiceSettings" -> buildJsonObject {
            put("streamServiceType", required("streamServiceType"))
            put("streamServiceSettings", required("streamServiceSettings"))
        }
        "GetSourceScreenshot" -> buildJsonObject {
            put("sourceName", required("sourceName"))
            put("imageFormat", this@toObsRequestData["imageFormat"] ?: "jpg")
            put("imageWidth", this@toObsRequestData["imageWidth"]?.toIntOrNull() ?: 420)
            put("imageHeight", this@toObsRequestData["imageHeight"]?.toIntOrNull() ?: 236)
            put("imageCompressionQuality", this@toObsRequestData["imageCompressionQuality"]?.toIntOrNull() ?: 65)
        }
        "SetSceneItemEnabled" -> buildJsonObject {
            put("sceneName", required("sceneName"))
            put("sceneItemId", required("sceneItemId").toInt())
            put("sceneItemEnabled", required("sceneItemEnabled").toBoolean())
        }
        "SetInputVolume" -> buildJsonObject {
            put("inputName", required("inputName"))
            put("inputVolumeMul", required("inputVolumeMul").toDouble())
        }
        "GetInputMute",
        "ToggleInputMute" -> buildJsonObject {
            put("inputName", required("inputName"))
        }
        "ToggleStream",
        "StartStream",
        "StopStream",
        "ToggleRecord",
        "StartRecord",
        "StopRecord",
        "PauseRecord",
        "ResumeRecord",
        "ToggleReplayBuffer",
        "StartReplayBuffer",
        "StopReplayBuffer",
        "SaveReplayBuffer",
        "ToggleVirtualCam",
        "StartVirtualCam",
        "StopVirtualCam",
        "ToggleStudioMode",
        "GetStudioModeEnabled",
        "TriggerStudioModeTransition",
        "GetStreamStatus",
        "GetRecordStatus",
        "GetStreamServiceSettings",
        "GetSceneList",
        "GetStats",
        "GetInputList" -> buildJsonObject {}
        else -> buildJsonObject {
            this@toObsRequestData
                .filterKeys { it.startsWith("data.") }
                .forEach { (key, value) -> put(key.removePrefix("data."), value) }
        }
    }

private fun obsErrorMessage(command: String, raw: String): String {
    val text = raw.ifBlank { "OBS command failed" }
    val lower = text.lowercase()
    if ("channel was closed" in lower || "closedreceivechannel" in lower) {
        return "OBS закрыл WebSocket-соединение. Чаще всего это неверный пароль WebSocket или OBS отклонил авторизацию. Проверь пароль в Сервис -> Настройки сервера WebSocket."
    }
    if (command in setOf("StartStream", "ToggleStream") &&
        listOf("key", "stream", "channel", "server", "rtmp", "access", "доступ", "ключ", "канал").any { it in lower }
    ) {
        return "StreamPanel подключился к OBS, но сам OBS не смог начать трансляцию: $text. Проверь в OBS: Настройки -> Трансляция -> сервис, аккаунт/ключ трансляции. Трансляцию начинать вручную не нужно."
    }
    return text
}
