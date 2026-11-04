package com.prismpixelvertex.palatelog.core.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

object MediaUtils {

    /**
     * Creates a temporary file in app internal storage and returns a FileProvider content URI for Camera capture.
     */
    fun createTempCameraUri(context: Context): Pair<File, Uri> {
        val photosDir = File(context.cacheDir, "temp_photos").apply { if (!exists()) mkdirs() }
        val tempFile = File(photosDir, "photo_${System.currentTimeMillis()}.jpg")
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, tempFile)
        return Pair(tempFile, contentUri)
    }

    /**
     * Copies any image URI (from PhotoPicker or Camera) to app internal storage
     * so it persists permanently across device reboots and app launches.
     */
    fun copyUriToInternalStorage(context: Context, sourceUri: Uri): String {
        return try {
            val photosDir = File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }
            val destFile = File(photosDir, "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            sourceUri.toString()
        }
    }
}

