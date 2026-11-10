package com.prismpixelvertex.palatelog.core.sync

import java.io.File

/**
 * Security validation utilities for cross-device cloud synchronization.
 * Neutralizes path traversal (CWE-22) and resource exhaustion attacks.
 */
object SyncSecurityUtils {

    val ALLOWED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

    /**
     * Sanitizes a remote photo file name. Returns null if the file name contains path traversal
     * characters, directory separators, null bytes, or an invalid file extension.
     */
    fun sanitizePhotoFileName(fileName: String?): String? {
        if (fileName.isNullOrBlank()) return null
        val cleanName = File(fileName).name
        if (cleanName != fileName ||
            fileName.contains("..") ||
            fileName.contains("/") ||
            fileName.contains("\\") ||
            fileName.contains("\u0000")
        ) {
            return null
        }
        val ext = cleanName.substringAfterLast('.', "").lowercase()
        if (ext !in ALLOWED_IMAGE_EXTENSIONS) {
            return null
        }
        return cleanName
    }

    /**
     * Verifies that the canonical path of [targetFile] resides strictly underneath [baseDir].
     */
    fun isPathWithinDirectory(targetFile: File, baseDir: File): Boolean {
        val canonicalTarget = targetFile.canonicalFile
        val canonicalBase = baseDir.canonicalFile
        return canonicalTarget.path.startsWith(canonicalBase.path + File.separator)
    }
}
