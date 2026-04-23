package com.group8.comp2300.service.auth

import java.io.File
import java.util.UUID

class ProfileImageStorage(private val uploadDir: String = "profile-images") {
    init {
        val dir = File(uploadDir)
        if (!dir.exists()) dir.mkdirs()
    }

    fun save(userId: String, originalFileName: String, fileBytes: ByteArray): Result<String> = runCatching {
        val extension = originalFileName.substringAfterLast('.', "").lowercase()
        require(extension in AllowedExtensions) { INVALID_TYPE_MESSAGE }
        require(fileBytes.size <= MAX_FILE_SIZE_BYTES) { FILE_TOO_LARGE_MESSAGE }

        val fileName = "$userId-${UUID.randomUUID()}.$extension"
        File(uploadDir, fileName).writeBytes(fileBytes)
        "/images/profile/$fileName"
    }.recoverCatching { error ->
        if (error is IllegalArgumentException) throw error
        throw IllegalStateException("Failed to store profile photo", error)
    }

    fun clear(userId: String) {
        val userPrefix = "$userId-"
        File(uploadDir)
            .listFiles()
            ?.filter { it.isFile && it.name.startsWith(userPrefix) }
            ?.forEach { file -> file.delete() }
    }

    fun deleteByUrl(imageUrl: String?) {
        val fileName = imageUrl
            ?.takeIf { it.startsWith(PUBLIC_URL_PREFIX) }
            ?.substringAfterLast('/')
            ?.takeIf(String::isNotBlank)
            ?: return

        File(uploadDir, fileName).takeIf(File::isFile)?.delete()
    }

    companion object {
        internal const val FILE_TOO_LARGE_MESSAGE = "Profile photo must be 5 MB or smaller"
        internal const val INVALID_TYPE_MESSAGE = "Profile photo must be JPG, PNG, or WebP"
        internal const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024
        private const val PUBLIC_URL_PREFIX = "/images/profile/"
        private val AllowedExtensions = setOf("jpg", "jpeg", "png", "webp")
    }
}
