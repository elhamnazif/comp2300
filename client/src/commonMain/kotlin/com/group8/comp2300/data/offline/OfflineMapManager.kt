package com.group8.comp2300.data.offline

import co.touchlab.kermit.Logger
import com.group8.comp2300.data.local.OfflineMapSettingsDataSource
import org.maplibre.compose.offline.OfflineManager
import org.maplibre.compose.offline.OfflinePackDefinition.TilePyramid
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position

class OfflineMapManager(
    private val offlineManager: OfflineManager,
    private val settingsDataSource: OfflineMapSettingsDataSource,
) {
    suspend fun ensureOfflineMapDownload() {
        if (settingsDataSource.isDownloadInitiated()) return

        try {
            val existingPack = offlineManager.packs
                .firstOrNull { it.metadata?.decodeToString() == PackMetadata }

            if (existingPack == null) {
                val definition = TilePyramid(
                    styleUrl = StyleUrl,
                    bounds = MalaysiaBounds,
                    minZoom = MinOfflineZoom,
                    maxZoom = MaxOfflineZoom,
                )

                val pack = offlineManager.create(
                    definition = definition,
                    metadata = PackMetadata.encodeToByteArray(),
                )
                offlineManager.resume(pack)
                Logger.i { "Offline map download initiated for Malaysia" }
            }

            settingsDataSource.markDownloadInitiated()
        } catch (e: Exception) {
            Logger.e(e) { "Failed to initiate offline map download" }
        }
    }

    companion object {
        val MalaysiaBounds = BoundingBox(
            southwest = Position(longitude = 99.5, latitude = 0.85),
            northeast = Position(longitude = 119.5, latitude = 7.5),
        )

        const val StyleUrl = "https://tiles.openfreemap.org/styles/liberty"
        const val MinOfflineZoom = 10
        const val MaxOfflineZoom = 16
        const val MinCameraZoom = 7f
        private const val PackMetadata = "vita-malaysia-offline"
    }
}
