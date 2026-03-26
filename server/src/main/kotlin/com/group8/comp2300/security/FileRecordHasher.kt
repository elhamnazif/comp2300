package com.group8.comp2300.security

import org.slf4j.LoggerFactory
import java.io.File
import java.security.MessageDigest

object FileIntegrityHasher {
    private val logger = LoggerFactory.getLogger(FileIntegrityHasher::class.java)

    /**
     * Generates a deterministic SHA-256 fingerprint of a file's content.
     */
    fun calculateFileHash(file: File): String? = try {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = file.readBytes()
        val hashBytes = digest.digest(bytes)
        // Convert bytes to a Hex string
        hashBytes.joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        logger.error("Failed to calculate hash for file: ${file.name}", e)
        null
    }

    /**
     * Verifies if the physical file matches the hash stored in the database.
     */
    fun verifyIntegrity(file: File, storedHash: String): Boolean {
        val currentHash = calculateFileHash(file)
        val isValid = currentHash == storedHash

        if (!isValid) {
            logger.warn("INTEGRITY BREACH: File ${file.name} does not match stored hash. Possible corruption or tampering.")
        }

        return isValid
    }
}


/**
 * implement in MedicalRecordService
 */
/*
fun getFileForDownload(id: String, userId: String): File? {
    // 1. Get the metadata from the database
    val record = repository.getRecordById(id, userId) ?: return null
    val physicalFile = File(record.storagePath)

    if (!physicalFile.exists()) {
        logger.error("File record exists in DB but physical file is missing: ${record.storagePath}")
        return null
    }

    // 2. Run the Integrity Check
    val currentHash = FileIntegrityHasher.calculateFileHash(physicalFile)
    val storedHash = record.fileHash

    if (currentHash != storedHash) {
        // THIS IS WHERE YOUR WARNING LIVES
        logger.warn("INTEGRITY CHECK FAILED for file ID: $id. Expected $storedHash but got $currentHash. Download aborted for security.")
        return null // Block the download because the data is untrustworthy
    }

    // 3. If everything matches, return the file to the Route to be streamed
    return physicalFile
}
*/
