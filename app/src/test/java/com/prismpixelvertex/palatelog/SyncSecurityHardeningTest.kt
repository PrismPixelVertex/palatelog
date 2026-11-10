package com.prismpixelvertex.palatelog

import com.prismpixelvertex.palatelog.core.sync.GoogleDriveSyncClient
import com.prismpixelvertex.palatelog.core.sync.SyncSecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SyncSecurityHardeningTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun sanitizePhotoFileName_validNames_accepted() {
        assertEquals("photo.jpg", SyncSecurityUtils.sanitizePhotoFileName("photo.jpg"))
        assertEquals("image_123.jpeg", SyncSecurityUtils.sanitizePhotoFileName("image_123.jpeg"))
        assertEquals("dish.png", SyncSecurityUtils.sanitizePhotoFileName("dish.png"))
        assertEquals("receipt.webp", SyncSecurityUtils.sanitizePhotoFileName("receipt.webp"))
        assertEquals("RECEIPT.JPG", SyncSecurityUtils.sanitizePhotoFileName("RECEIPT.JPG"))
    }

    @Test
    fun sanitizePhotoFileName_pathTraversalAttempts_rejected() {
        assertNull(SyncSecurityUtils.sanitizePhotoFileName("../secret.jpg"))
        assertNull(SyncSecurityUtils.sanitizePhotoFileName("../../shared_prefs/prefs.xml"))
        assertNull(SyncSecurityUtils.sanitizePhotoFileName("..\\..\\databases\\palatelog.db"))
        assertNull(SyncSecurityUtils.sanitizePhotoFileName("/etc/passwd"))
        assertNull(SyncSecurityUtils.sanitizePhotoFileName("folder/photo.jpg"))
        assertNull(SyncSecurityUtils.sanitizePhotoFileName("photo\u0000.jpg"))
        assertNull(SyncSecurityUtils.sanitizePhotoFileName(".."))
        assertNull(SyncSecurityUtils.sanitizePhotoFileName("."))
    }

    @Test
    fun sanitizePhotoFileName_invalidExtensions_rejected() {
        assertNull(SyncSecurityUtils.sanitizePhotoFileName("malicious.exe"))
        assertNull(SyncSecurityUtils.sanitizePhotoFileName("script.sh"))
        assertNull(SyncSecurityUtils.sanitizePhotoFileName("data.json"))
        assertNull(SyncSecurityUtils.sanitizePhotoFileName("archive.zip"))
        assertNull(SyncSecurityUtils.sanitizePhotoFileName("photo.txt"))
        assertNull(SyncSecurityUtils.sanitizePhotoFileName(""))
        assertNull(SyncSecurityUtils.sanitizePhotoFileName(null))
    }

    @Test
    fun isPathWithinDirectory_strictlyEnforcesParentDirectory() {
        val baseDir = tempFolder.newFolder("photos")
        val legitimateFile = File(baseDir, "img_123.jpg")
        legitimateFile.createNewFile()

        assertTrue(SyncSecurityUtils.isPathWithinDirectory(legitimateFile, baseDir))

        val outsideFile = File(tempFolder.root, "outside.jpg")
        outsideFile.createNewFile()

        assertFalse(SyncSecurityUtils.isPathWithinDirectory(outsideFile, baseDir))
    }

    @Test
    fun syncResourceLimits_areConfiguredSafely() {
        assertEquals(15L * 1024 * 1024, GoogleDriveSyncClient.MAX_PHOTO_SIZE_BYTES)
        assertEquals(10L * 1024 * 1024, GoogleDriveSyncClient.MAX_PAYLOAD_SIZE_BYTES)
    }
}
