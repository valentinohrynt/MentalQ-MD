package com.c242_ps246.mentalq.data.repository

import org.json.JSONObject
import retrofit2.Response

internal fun Response<*>.errorMessage(fallback: String = "Request failed"): String {
    val rawBody = runCatching { errorBody()?.string() }.getOrNull()
    if (rawBody.isNullOrBlank()) return "$fallback (${code()})"

    return runCatching {
        JSONObject(rawBody).optString("message").ifBlank { "$fallback (${code()})" }
    }.getOrDefault("$fallback (${code()})")
}

internal fun Throwable.toUserMessage(prefix: String): String =
    message?.takeIf { it.isNotBlank() }?.let { "$prefix: $it" } ?: prefix
