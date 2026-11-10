package com.prismpixelvertex.palatelog.core.sync

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class DriveFileItem(
    val id: String,
    val name: String,
    val createdTime: String? = null,
    val modifiedTime: String? = null
)

@Serializable
data class DriveFileListResponse(
    val files: List<DriveFileItem> = emptyList()
)

@Serializable
data class DrivePermissionItem(
    val id: String? = null,
    val emailAddress: String? = null,
    val role: String? = null
)

@Serializable
data class DrivePermissionListResponse(
    val permissions: List<DrivePermissionItem> = emptyList()
)

sealed interface FolderAccessResult {
    object Success : FolderAccessResult
    data class AccessDenied(val folderId: String, val statusCode: Int) : FolderAccessResult
    data class Error(val message: String) : FolderAccessResult
}

class GoogleAuthException(
    message: String,
    val userRecoverableIntent: android.content.Intent? = null
) : Exception(message)

@Singleton
class GoogleDriveSyncClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: HttpClient
) {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Gets OAuth2 access token for Google Drive API.
     */
    suspend fun getAccessToken(accountEmail: String): String = withContext(Dispatchers.IO) {
        val scope = "oauth2:https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/drive.readonly"
        val account = Account(accountEmail, "com.google")
        try {
            GoogleAuthUtil.getToken(context, account, scope)
        } catch (e: UserRecoverableAuthException) {
            throw GoogleAuthException("Google Auth permission required", e.intent)
        } catch (e: Exception) {
            throw GoogleAuthException("Failed to obtain OAuth token: ${e.localizedMessage}")
        }
    }

    /**
     * Locates or creates the 'PalateLog_Family' folder in Google Drive.
     */
    suspend fun getOrCreateFamilyFolder(accessToken: String): String = withContext(Dispatchers.IO) {
        val folderName = "PalateLog_Family"
        // 1. Search for existing folder
        val query = "name = '$folderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"

        val response = httpClient.get("https://www.googleapis.com/drive/v3/files") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            parameter("q", query)
            parameter("fields", "files(id,name)")
        }

        val responseText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IOException("Failed to search Drive folder: HTTP ${response.status.value} - $responseText")
        }

        val fileList = json.decodeFromString<DriveFileListResponse>(responseText)
        if (fileList.files.isNotEmpty()) {
            return@withContext fileList.files.first().id
        }

        // 2. Folder does not exist, create it
        createNewFamilyFolder(accessToken)
    }

    /**
     * Creates a fresh 'PalateLog_Family' folder in Google Drive.
     */
    suspend fun createNewFamilyFolder(accessToken: String): String = withContext(Dispatchers.IO) {
        val folderName = "PalateLog_Family"
        val jsonPayload = """{"name":"$folderName","mimeType":"application/vnd.google-apps.folder"}"""

        val response = httpClient.post("https://www.googleapis.com/drive/v3/files") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(jsonPayload)
        }

        val responseText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IOException("Failed to create Drive folder: HTTP ${response.status.value} - $responseText")
        }

        val createdFolder = json.decodeFromString<DriveFileItem>(responseText)
        createdFolder.id
    }

    /**
     * Uploads a JSON sync payload file to the target Google Drive folder.
     */
    suspend fun uploadSyncPayload(
        accessToken: String,
        folderId: String,
        fileName: String,
        jsonContent: String
    ): Boolean = withContext(Dispatchers.IO) {
        val metadataJson = """{"name":"$fileName","parents":["$folderId"]}"""

        val response = httpClient.submitFormWithBinaryData(
            url = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart",
            formData = formData {
                append(
                    key = "metadata",
                    value = metadataJson,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, "application/json; charset=UTF-8")
                    }
                )
                append(
                    key = "file",
                    value = jsonContent.toByteArray(Charsets.UTF_8),
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, "application/json; charset=UTF-8")
                    }
                )
            }
        ) {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }

        val responseText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IOException("Failed to upload sync payload: HTTP ${response.status.value} - $responseText")
        }

        true
    }

    /**
     * Searches for an existing canonical sync file for this device (changes_<deviceId>.json).
     */
    suspend fun findSyncFileForDevice(
        accessToken: String,
        folderId: String,
        deviceId: String
    ): DriveFileItem? = withContext(Dispatchers.IO) {
        val targetName = "changes_$deviceId.json"
        val query = "'$folderId' in parents and name = '$targetName' and trashed = false"

        val response = httpClient.get("https://www.googleapis.com/drive/v3/files") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            parameter("q", query)
            parameter("fields", "files(id,name,createdTime,modifiedTime)")
            parameter("pageSize", 1)
        }

        if (!response.status.isSuccess()) {
            return@withContext null
        }

        val fileList = json.decodeFromString<DriveFileListResponse>(response.bodyAsText())
        fileList.files.firstOrNull()
    }

    /**
     * Updates an existing Google Drive sync file content in-place via PATCH.
     */
    suspend fun updateSyncPayloadFile(
        accessToken: String,
        fileId: String,
        jsonContent: String
    ): Boolean = withContext(Dispatchers.IO) {
        val response = httpClient.patch("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json.withParameter("charset", "UTF-8"))
            setBody(jsonContent)
        }

        val responseText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IOException("Failed to update sync payload: HTTP ${response.status.value} - $responseText")
        }

        true
    }

    /**
     * Uploads or updates the single canonical sync file for this device (changes_<deviceId>.json)
     * with automatic retry for transient network timeouts.
     */
    suspend fun uploadOrUpdateSyncPayload(
        accessToken: String,
        folderId: String,
        deviceId: String,
        jsonContent: String
    ): Boolean {
        var lastException: Exception? = null
        for (attempt in 1..2) {
            try {
                val existingFile = findSyncFileForDevice(accessToken, folderId, deviceId)
                val targetName = "changes_$deviceId.json"
                return if (existingFile != null) {
                    updateSyncPayloadFile(accessToken, existingFile.id, jsonContent)
                } else {
                    uploadSyncPayload(accessToken, folderId, targetName, jsonContent)
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < 2 && (e is io.ktor.client.plugins.HttpRequestTimeoutException || e is IOException)) {
                    Log.w("GoogleDriveSyncClient", "Payload upload attempt $attempt failed with ${e.message}. Retrying in 1.5s...")
                    delay(1500)
                } else {
                    throw e
                }
            }
        }
        throw lastException ?: IOException("Failed to upload or update sync payload")
    }

    /**
     * Lists all JSON delta sync files in the family folder.
     */
    suspend fun listRemoteSyncFiles(
        accessToken: String,
        folderId: String
    ): List<DriveFileItem> = withContext(Dispatchers.IO) {
        val query = "'$folderId' in parents and mimeType = 'application/json' and trashed = false"

        val response = httpClient.get("https://www.googleapis.com/drive/v3/files") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            parameter("q", query)
            parameter("fields", "files(id,name,createdTime,modifiedTime)")
            parameter("pageSize", 100)
        }

        val responseText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IOException("Failed to list remote sync files: HTTP ${response.status.value} - $responseText")
        }

        val fileList = json.decodeFromString<DriveFileListResponse>(responseText)
        fileList.files
    }

    /**
     * Downloads content of a specific sync file by fileId.
     */
    suspend fun downloadSyncPayload(
        accessToken: String,
        fileId: String
    ): String = withContext(Dispatchers.IO) {
        val response = httpClient.get("https://www.googleapis.com/drive/v3/files/$fileId") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            parameter("alt", "media")
        }

        val responseText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IOException("Failed to download payload: HTTP ${response.status.value} - $responseText")
        }

        responseText
    }

    /**
     * Locates or creates the 'Photos' subfolder inside 'PalateLog_Family'.
     */
    suspend fun getOrCreatePhotosFolder(
        accessToken: String,
        familyFolderId: String
    ): String = withContext(Dispatchers.IO) {
        val folderName = "Photos"
        val query = "'$familyFolderId' in parents and name = '$folderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"

        val response = httpClient.get("https://www.googleapis.com/drive/v3/files") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            parameter("q", query)
            parameter("fields", "files(id,name)")
        }

        val responseText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IOException("Failed to search Photos folder: HTTP ${response.status.value} - $responseText")
        }

        val fileList = json.decodeFromString<DriveFileListResponse>(responseText)
        if (fileList.files.isNotEmpty()) {
            return@withContext fileList.files.first().id
        }

        val jsonPayload = """{"name":"$folderName","mimeType":"application/vnd.google-apps.folder","parents":["$familyFolderId"]}"""
        val createResponse = httpClient.post("https://www.googleapis.com/drive/v3/files") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(jsonPayload)
        }

        val createBody = createResponse.bodyAsText()
        if (!createResponse.status.isSuccess()) {
            throw IOException("Failed to create Photos folder: HTTP ${createResponse.status.value} - $createBody")
        }

        val createdFolder = json.decodeFromString<DriveFileItem>(createBody)
        createdFolder.id
    }

    /**
     * Lists all photo files in the 'Photos' subfolder.
     */
    suspend fun listRemotePhotoFiles(
        accessToken: String,
        photosFolderId: String
    ): List<DriveFileItem> = withContext(Dispatchers.IO) {
        val query = "'$photosFolderId' in parents and trashed = false"

        val response = httpClient.get("https://www.googleapis.com/drive/v3/files") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            parameter("q", query)
            parameter("fields", "files(id,name,createdTime,modifiedTime)")
            parameter("pageSize", 1000)
        }

        if (!response.status.isSuccess()) {
            return@withContext emptyList()
        }

        val fileList = json.decodeFromString<DriveFileListResponse>(response.bodyAsText())
        fileList.files
    }

    /**
     * Uploads a binary photo file to the 'Photos' subfolder in Google Drive.
     */
    suspend fun uploadPhotoFile(
        accessToken: String,
        photosFolderId: String,
        fileName: String,
        fileBytes: ByteArray
    ): Boolean = withContext(Dispatchers.IO) {
        val metadataJson = """{"name":"$fileName","parents":["$photosFolderId"]}"""

        val response = httpClient.submitFormWithBinaryData(
            url = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart",
            formData = formData {
                append(
                    key = "metadata",
                    value = metadataJson,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, "application/json; charset=UTF-8")
                    }
                )
                append(
                    key = "file",
                    value = fileBytes,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                    }
                )
            }
        ) {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }

        val responseText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IOException("Failed to upload photo $fileName: HTTP ${response.status.value} - $responseText")
        }

        true
    }

    /**
     * Downloads a photo file from Google Drive directly into local device file.
     */
    suspend fun downloadPhotoFile(
        accessToken: String,
        fileId: String,
        destFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val response = httpClient.get("https://www.googleapis.com/drive/v3/files/$fileId") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            parameter("alt", "media")
        }

        if (response.status.isSuccess()) {
            destFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
            val bytes: ByteArray = response.body()
            destFile.writeBytes(bytes)
            true
        } else {
            false
        }
    }

    /**
     * Verifies if the authenticated user has access to the target Google Drive folder.
     */
    suspend fun verifyFolderAccess(
        accessToken: String,
        folderId: String
    ): FolderAccessResult = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.get("https://www.googleapis.com/drive/v3/files/$folderId") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("fields", "id,name")
            }

            when (response.status) {
                HttpStatusCode.OK -> FolderAccessResult.Success
                HttpStatusCode.NotFound, HttpStatusCode.Forbidden -> {
                    FolderAccessResult.AccessDenied(folderId, response.status.value)
                }
                else -> {
                    FolderAccessResult.Error("HTTP ${response.status.value}: ${response.bodyAsText()}")
                }
            }
        } catch (e: Exception) {
            FolderAccessResult.Error(e.localizedMessage ?: "Network request failed")
        }
    }

    /**
     * Grants 'writer' (Editor) permission on the target Google Drive folder to a family member email.
     */
    suspend fun grantFolderPermission(
        accessToken: String,
        folderId: String,
        emailAddress: String
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanEmail = emailAddress.trim()
        android.util.Log.d("PalateLog_Sync", "grantFolderPermission called for email=$cleanEmail, folderId=$folderId")
        val jsonPayload = """{"role":"writer","type":"user","emailAddress":"$cleanEmail"}"""

        val response = httpClient.post("https://www.googleapis.com/drive/v3/files/$folderId/permissions") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            parameter("sendNotificationEmail", false)
            contentType(ContentType.Application.Json)
            setBody(jsonPayload)
        }

        val responseBody = response.bodyAsText()
        android.util.Log.d("PalateLog_Sync", "grantFolderPermission response code: ${response.status.value}, body: $responseBody")

        if (!response.status.isSuccess() && response.status != HttpStatusCode.Conflict) { // 409 Conflict = already granted
            android.util.Log.e("PalateLog_Sync", "grantFolderPermission failed: HTTP ${response.status.value} - $responseBody")
            throw IOException("HTTP ${response.status.value} - $responseBody")
        }

        true
    }

    /**
     * Lists current permission holders for the target Google Drive folder.
     */
    suspend fun listFolderPermissions(
        accessToken: String,
        folderId: String
    ): List<DrivePermissionItem> = withContext(Dispatchers.IO) {
        val response = httpClient.get("https://www.googleapis.com/drive/v3/files/$folderId/permissions") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            parameter("fields", "permissions(id,emailAddress,role)")
        }

        val responseBody = response.bodyAsText()
        if (!response.status.isSuccess()) {
            return@withContext emptyList()
        }

        try {
            val permList = json.decodeFromString<DrivePermissionListResponse>(responseBody)
            permList.permissions
        } catch (e: Exception) {
            emptyList()
        }
    }
}
