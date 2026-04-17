package com.group8.comp2300.platform.files

import io.github.vinceglb.filekit.*
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication

class MedicalRecordFileOpener {
    suspend fun open(fileName: String, fileBytes: ByteArray): Result<Unit> = runCatching {
        val cacheDirectory = PlatformFile(FileKit.cacheDir, "medical-records").also { it.createDirectories() }
        val cachedFile = PlatformFile(cacheDirectory, fileName.sanitizedFileName())
        cachedFile.write(fileBytes)
        FileKit.openFileWithDefaultApplication(cachedFile)
    }
}

private fun String.sanitizedFileName(): String = buildString(length) {
    for (character in this@sanitizedFileName) {
        append(
            when (character) {
                '/', '\\', ':', '*', '?', '"', '<', '>', '|' -> '_'
                else -> character
            },
        )
    }
}.ifBlank { "medical-record" }
