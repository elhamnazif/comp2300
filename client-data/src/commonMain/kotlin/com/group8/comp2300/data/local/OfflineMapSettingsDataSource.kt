package com.group8.comp2300.data.local

import com.russhwolf.settings.Settings

class OfflineMapSettingsDataSource(private val settings: Settings) {
    fun isDownloadInitiated(): Boolean = settings.getBoolean(KeyDownloadInitiated, false)

    fun markDownloadInitiated() {
        settings.putBoolean(KeyDownloadInitiated, true)
    }

    private companion object {
        const val KeyDownloadInitiated = "offline_map.download_initiated"
    }
}
