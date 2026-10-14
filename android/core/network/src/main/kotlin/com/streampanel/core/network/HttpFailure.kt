package com.streampanel.core.network

/**
 * A web service said no. "HTTP 401" alone means little to a person: say what it usually means and keep the
 * service's own words, because those often name the real problem (Spotify: NO_ACTIVE_DEVICE).
 */
fun describeHttpFailure(service: String, code: Int, body: String): String {
    val reason = when (code) {
        400 -> "the request was not understood — check the values you entered"
        401 -> "access denied — check the token, key or password"
        403 -> "not allowed — the account or token lacks permission"
        404 -> "not found — check the address and the ids"
        408 -> "the service took too long"
        429 -> "too many requests — wait a little and try again"
        in 500..599 -> "the service has a problem on its side"
        else -> ""
    }
    val detail = body.trim().replace(Regex("\\s+"), " ").take(160)
    return buildString {
        append(service).append(" answered HTTP ").append(code)
        if (reason.isNotEmpty()) append(": ").append(reason)
        if (detail.isNotEmpty()) append(" (").append(detail).append(")")
    }
}