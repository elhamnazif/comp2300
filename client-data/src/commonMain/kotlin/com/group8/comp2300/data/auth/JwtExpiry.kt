package com.group8.comp2300.data.auth

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalEncodingApi::class)
internal fun extractJwtExpiration(accessToken: String): Long? {
    val payloadSegment = accessToken.split('.').getOrNull(1) ?: return null
    val normalizedPayload = payloadSegment.padEnd((payloadSegment.length + 3) / 4 * 4, '=')
    val payloadJson = runCatching { Base64.UrlSafe.decode(normalizedPayload).decodeToString() }.getOrNull() ?: return null
    val expiresAtSeconds = runCatching {
        Json.parseToJsonElement(payloadJson)
            .jsonObject["exp"]
            ?.jsonPrimitive
            ?.content
            ?.toLong()
    }.getOrNull() ?: return null
    return expiresAtSeconds * 1000
}
