package com.group8.comp2300.core.network

import com.group8.comp2300.data.remote.apiBaseUrl

internal fun resolveRemoteAssetUrl(url: String?): String? {
    val value = url?.trim().orEmpty()
    if (value.isEmpty()) return null
    if (value.startsWith("http://") || value.startsWith("https://")) return value
    return "${apiBaseUrl().trimEnd('/')}/${value.trimStart('/')}"
}
