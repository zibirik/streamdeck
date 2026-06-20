package com.streampanel.core.integrations

data class IntegrationResult(
    val ok: Boolean,
    val message: String,
) {
    companion object {
        fun ok(message: String) = IntegrationResult(true, message)
        fun error(message: String) = IntegrationResult(false, message)
    }
}

internal fun Map<String, String>.required(key: String): String =
    this[key]?.takeIf(String::isNotBlank)
        ?: throw IllegalArgumentException("Missing payload key: $key")
