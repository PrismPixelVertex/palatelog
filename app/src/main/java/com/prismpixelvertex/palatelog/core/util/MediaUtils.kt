package com.prismpixelvertex.palatelog.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * Custom [ActivityResultContracts.TakePicture] contract that explicitly passes
 * intent extras targeting the device's rear (back) camera across OEM camera implementations.
 */
class TakePictureWithRearCamera : ActivityResultContracts.TakePicture() {
    override fun createIntent(context: Context, input: Uri): Intent {
        return super.createIntent(context, input).apply {
            // Camera1 API extra (0 = CAMERA_FACING_BACK)
            putExtra("android.intent.extras.CAMERA_FACING", 0)
            putExtra("android.intent.extra.CAMERA_FACING", 0)

            // Camera2 API extra (1 = LENS_FACING_BACK)
            putExtra("android.intent.extras.LENS_FACING_BACK", 1)
            putExtra("android.intent.extra.LENS_FACING_BACK", 1)

            // Boolean front-camera overrides
            putExtra("android.intent.extra.USE_FRONT_CAMERA", false)
            putExtra("android.intent.extras.USE_FRONT_CAMERA", false)

            // Vendor-specific string/int hints (Samsung, MIUI, etc.)
            putExtra("camerafacing", "back")
            putExtra("previous_mode", 0)
        }
    }
}

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

