package com.streampanel.core.network

import kotlinx.serialization.Serializable

const val CURRENT_PROTOCOL_VERSION = 1

@Serializable
data class PcCommand(
    val id: String,
    val protocolVersion: Int = CURRENT_PROTOCOL_VERSION,
    val type: PcCommandType,
    val payload: Map<String, String>,
    val createdAtEpochMs: Long,
)

@Serializable
enum class PcCommandType {
    OPEN_URL,
    LAUNCH_PROCESS,
    SEND_TEXT,
    HOTKEY,
    MOUSE_COMMAND,
    MEDIA_COMMAND,
    VOLUME_COMMAND,
    WINDOW_COMMAND,
    OPEN_FOLDER,
    OPEN_URLS,
    SYSTEM_COMMAND,
    CUSTOM,
}

@Serializable
data class PcCommandResponse(
    val id: String,
    val ok: Boolean,
    val message: String,
    val completedAtEpochMs: Long,
)

sealed interface ConnectionStatus {
    data object Disconnected : ConnectionStatus
    data object Connecting : ConnectionStatus
    data class Connected(val url: String) : ConnectionStatus
    data class Failed(val reason: String) : ConnectionStatus
}
