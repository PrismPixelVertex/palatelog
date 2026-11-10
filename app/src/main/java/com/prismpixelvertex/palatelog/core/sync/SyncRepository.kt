package com.prismpixelvertex.palatelog.core.sync

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.core.content.edit
import com.prismpixelvertex.palatelog.core.database.DishFeedbackDao
import com.prismpixelvertex.palatelog.core.database.DishFeedbackEntity
import com.prismpixelvertex.palatelog.core.database.RestaurantDao
import com.prismpixelvertex.palatelog.core.database.RestaurantEntity
import com.prismpixelvertex.palatelog.core.database.VisitDao
import com.prismpixelvertex.palatelog.core.database.VisitEntryEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import com.prismpixelvertex.palatelog.R
import com.prismpixelvertex.palatelog.core.notification.PalateNotificationManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SyncStatus {
    object Idle : SyncStatus
    object Syncing : SyncStatus
    data class Success(val message: String, val timestamp: Long) : SyncStatus
    data class Error(val error: String) : SyncStatus
}

@Singleton
class SyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val restaurantDao: RestaurantDao,
    private val visitDao: VisitDao,
    private val dishDao: DishFeedbackDao,
    private val googleDriveSyncClient: GoogleDriveSyncClient,
    private val notificationManager: PalateNotificationManager
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val prefs = context.getSharedPreferences("palatelog_sync_prefs", Context.MODE_PRIVATE)

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    @SuppressLint("HardwareIds")
    private fun getHardwareDeviceId(): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "device_unknown"
        } catch (_: Exception) {
            "device_unknown"
        }
    }

    private val deviceId: String by lazy { getHardwareDeviceId() }

    fun getGoogleAccountEmail(): String? {
        return prefs.getString("google_account_email", null)
    }

    fun setGoogleAccountEmail(email: String?) {
        if (email.isNullOrBlank()) {
            prefs.edit { remove("google_account_email") }
        } else {
            prefs.edit { putString("google_account_email", email) }
        }
    }

    fun getSyncMode(): String {
        return prefs.getString("sync_mode", "SHARED_ACCOUNT") ?: "SHARED_ACCOUNT"
    }

    fun setSyncMode(mode: String) {
        prefs.edit { putString("sync_mode", mode) }
    }

    fun getTargetFolderId(): String? {
        return prefs.getString("target_folder_id", null)
    }

    fun setTargetFolderId(folderId: String?) {
        if (folderId.isNullOrBlank()) {
            prefs.edit { remove("target_folder_id") }
        } else {
            prefs.edit { putString("target_folder_id", folderId) }
        }
    }

    fun getLastSyncTimestamp(): Long {
        return prefs.getLong("last_synced_at", 0L)
    }

    private fun setLastSyncTimestamp(timestamp: Long) {
        prefs.edit { putLong("last_synced_at", timestamp) }
    }

    private fun extractPhotoFileName(uriString: String?): String? {
        if (uriString.isNullOrBlank()) return null
        val lastSegment = uriString.substringAfterLast("/")
        return SyncSecurityUtils.sanitizePhotoFileName(lastSegment)
    }

    private fun getLocalPhotoUri(fileName: String): String? {
        val file = getLocalPhotoFile(fileName) ?: return null
        return if (file.exists()) android.net.Uri.fromFile(file).toString() else null
    }

    private fun getLocalPhotoFile(fileName: String): File? {
        val cleanName = SyncSecurityUtils.sanitizePhotoFileName(fileName)
        if (cleanName == null) {
            Log.w("PalateLog_Sync", "Unsafe or invalid photo file name rejected: $fileName")
            return null
        }
        val photosDir = File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }
        val targetFile = File(photosDir, cleanName)
        if (!SyncSecurityUtils.isPathWithinDirectory(targetFile, photosDir)) {
            Log.w("PalateLog_Sync", "Path traversal detected for photo name: $fileName")
            return null
        }
        return targetFile
    }

    /**
     * Grants permission to an invited family member via Google Drive API.
     */
    suspend fun inviteFamilyMember(email: String): Boolean = withContext(Dispatchers.IO) {
        val accountEmail = getGoogleAccountEmail() ?: throw IllegalStateException("Not connected to Google Account.")
        val accessToken = googleDriveSyncClient.getAccessToken(accountEmail)
        var folderId = getTargetFolderId()
        if (folderId.isNullOrBlank()) {
            folderId = googleDriveSyncClient.getOrCreateFamilyFolder(accessToken)
            setTargetFolderId(folderId)
        }

        try {
            googleDriveSyncClient.grantFolderPermission(accessToken, folderId, email)
        } catch (e: Exception) {
            if (e.message?.contains("appNotAuthorizedToChild") == true) {
                folderId = googleDriveSyncClient.createNewFamilyFolder(accessToken)
                setTargetFolderId(folderId)
                googleDriveSyncClient.grantFolderPermission(accessToken, folderId, email)
            } else {
                throw e
            }
        }
    }

    /**
     * Lists all family members currently holding permissions on the family sync folder.
     */
    suspend fun listInvitedMembers(): List<DrivePermissionItem> = withContext(Dispatchers.IO) {
        val accountEmail = getGoogleAccountEmail() ?: return@withContext emptyList()
        val accessToken = googleDriveSyncClient.getAccessToken(accountEmail)
        val folderId = getTargetFolderId() ?: googleDriveSyncClient.getOrCreateFamilyFolder(accessToken)
        googleDriveSyncClient.listFolderPermissions(accessToken, folderId)
    }

    /**
     * Verifies whether the connected account has access to the target sync folder.
     */
    suspend fun verifyTargetFolderAccess(): FolderAccessResult = withContext(Dispatchers.IO) {
        val accountEmail = getGoogleAccountEmail() ?: return@withContext FolderAccessResult.Error("No account connected")
        val accessToken = googleDriveSyncClient.getAccessToken(accountEmail)
        val folderId = getTargetFolderId() ?: googleDriveSyncClient.getOrCreateFamilyFolder(accessToken)
        googleDriveSyncClient.verifyFolderAccess(accessToken, folderId)
    }

    /**
     * Executes 2-way Google Shared Drive Sync including photo media files.
     */
    suspend fun performGoogleDriveSync(forceFullExport: Boolean = false): Unit = withContext(Dispatchers.IO) {
        val accountEmail = getGoogleAccountEmail()
        if (accountEmail.isNullOrBlank()) {
            _syncStatus.value = SyncStatus.Error("No Google account connected.")
            return@withContext
        }

        Log.d("PalateLog_Sync", "Starting Google Drive Sync for account: $accountEmail, device: $deviceId")
        _syncStatus.value = SyncStatus.Syncing
        try {
            val accessToken = googleDriveSyncClient.getAccessToken(accountEmail)

            var folderId = getTargetFolderId()
            if (!folderId.isNullOrBlank()) {
                val accessResult = googleDriveSyncClient.verifyFolderAccess(accessToken, folderId)
                if (accessResult is FolderAccessResult.AccessDenied) {
                    val errMsg = "Access Pending: Please ask host to add $accountEmail in Family Sync Settings."
                    Log.e("PalateLog_Sync", "Folder access denied for folderId=$folderId")
                    _syncStatus.value = SyncStatus.Error(errMsg)
                    throw IOException(errMsg)
                }
            } else {
                folderId = googleDriveSyncClient.getOrCreateFamilyFolder(accessToken)
                setTargetFolderId(folderId)
            }

            val photosFolderId = try {
                googleDriveSyncClient.getOrCreatePhotosFolder(accessToken, folderId)
            } catch (e: Exception) {
                Log.e("PalateLog_Sync", "Failed to access/create Photos folder", e)
                null
            }

            val remotePhotoFiles = if (photosFolderId != null) {
                googleDriveSyncClient.listRemotePhotoFiles(accessToken, photosFolderId)
            } else emptyList()
            val remotePhotoMap = remotePhotoFiles.associateBy { it.name }
            Log.d("PalateLog_Sync", "Found ${remotePhotoMap.size} remote photo files in Google Drive")

            var mergedRemoteCount = 0
            var photosDownloadedCount = 0
            var photosUploadedCount = 0
            val remotePayloads = mutableListOf<SyncPayload>()

            // 1. Download and merge all remote delta files from other family members
            val remoteFiles = googleDriveSyncClient.listRemoteSyncFiles(accessToken, folderId)
            Log.d("PalateLog_Sync", "Found ${remoteFiles.size} remote JSON delta files in Google Drive")

            for (file in remoteFiles) {
                if (file.name == "changes_$deviceId.json" || file.name.startsWith("changes_${deviceId}_")) continue

                try {
                    val remoteJson = googleDriveSyncClient.downloadSyncPayload(accessToken, file.id)
                    val remotePayload = json.decodeFromString<SyncPayload>(remoteJson)

                    if (photosFolderId != null) {
                        for (visit in remotePayload.visits) {
                            val visitPhotoNames = if (visit.photoNames.isNotEmpty()) visit.photoNames else listOfNotNull(visit.billReceiptPhotoName)
                            for (photoName in visitPhotoNames) {
                                val localFile = getLocalPhotoFile(photoName) ?: continue
                                if (!localFile.exists()) {
                                    remotePhotoMap[photoName]?.let { driveFile ->
                                        val success = googleDriveSyncClient.downloadPhotoFile(accessToken, driveFile.id, localFile)
                                        if (success) {
                                            photosDownloadedCount++
                                            Log.d("PalateLog_Sync", "Downloaded photo: $photoName")
                                        } else {
                                            Log.e("PalateLog_Sync", "Failed to download photo: $photoName")
                                        }
                                    }
                                }
                            }
                        }
                        for (dish in remotePayload.dishes) {
                            for (photoName in dish.photoNames) {
                                val localFile = getLocalPhotoFile(photoName) ?: continue
                                if (!localFile.exists()) {
                                    remotePhotoMap[photoName]?.let { driveFile ->
                                        val success = googleDriveSyncClient.downloadPhotoFile(accessToken, driveFile.id, localFile)
                                        if (success) {
                                            photosDownloadedCount++
                                            Log.d("PalateLog_Sync", "Downloaded photo: $photoName")
                                        } else {
                                            Log.e("PalateLog_Sync", "Failed to download photo: $photoName")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    importPayload(remotePayload)
                    remotePayloads.add(remotePayload)
                    mergedRemoteCount++
                } catch (e: Exception) {
                    Log.e("PalateLog_Sync", "Error downloading or importing remote payload ${file.name}", e)
                }
            }

            // 2. Export local payload and upload missing local photo files to Google Drive
            val localPayload = exportSharedPayload(0L)

            if (photosFolderId != null) {
                val localPhotoNames = mutableSetOf<String>()
                localPayload.visits.forEach { v ->
                    localPhotoNames.addAll(v.photoNames)
                    v.billReceiptPhotoName?.let { localPhotoNames.add(it) }
                }
                localPayload.dishes.forEach { d -> localPhotoNames.addAll(d.photoNames) }

                for (photoName in localPhotoNames) {
                    if (!remotePhotoMap.containsKey(photoName)) {
                        val localFile = getLocalPhotoFile(photoName) ?: continue
                        if (localFile.exists()) {
                            try {
                                val bytes = localFile.readBytes()
                                val success = googleDriveSyncClient.uploadPhotoFile(accessToken, photosFolderId, photoName, bytes)
                                if (success) {
                                    photosUploadedCount++
                                    Log.d("PalateLog_Sync", "Uploaded photo to Google Drive: $photoName (${bytes.size} bytes)")
                                }
                            } catch (e: Exception) {
                                Log.e("PalateLog_Sync", "Failed to upload photo $photoName", e)
                            }
                        }
                    }
                }
            }

            if (localPayload.restaurants.isNotEmpty() || localPayload.visits.isNotEmpty() || localPayload.dishes.isNotEmpty()) {
                val jsonString = json.encodeToString(localPayload)
                googleDriveSyncClient.uploadOrUpdateSyncPayload(accessToken, folderId, deviceId, jsonString)
            }

            val now = System.currentTimeMillis()
            setLastSyncTimestamp(now)

            // Clear any previously active sync auth alert since sync was successful
            notificationManager.clearSyncAuthAlert()

            // If remote family updates arrived, dispatch a batched family sync digest
            val allNewRestaurants = remotePayloads.flatMap { it.restaurants }.filter { !it.isDeleted }
            val allNewVisits = remotePayloads.flatMap { it.visits }.filter { !it.isDeleted }
            val allNewDishes = remotePayloads.flatMap { it.dishes }.filter { !it.isDeleted }

            val totalUpdatesCount = allNewRestaurants.size + allNewVisits.size
            if (totalUpdatesCount > 0) {
                val memberNames = allNewVisits.mapNotNull { it.createdByMemberName.takeIf { name -> name.isNotBlank() } }.distinct()
                val placeNames = allNewRestaurants.map { it.name }.distinct()

                val authorName = when {
                    memberNames.size == 1 -> memberNames.first()
                    else -> null
                }

                val summary = if (authorName != null) {
                    if (totalUpdatesCount == 1) {
                        context.getString(R.string.notification_family_sync_single, authorName, totalUpdatesCount)
                    } else {
                        context.getString(R.string.notification_family_sync_plural, authorName, totalUpdatesCount)
                    }
                } else {
                    if (totalUpdatesCount == 1) {
                        context.getString(R.string.notification_family_sync_generic_single, totalUpdatesCount)
                    } else {
                        context.getString(R.string.notification_family_sync_generic_plural, totalUpdatesCount)
                    }
                }

                val details = mutableListOf<String>()
                for (place in placeNames.take(3)) {
                    details.add("📍 $place")
                }
                for (dish in allNewDishes.take(3)) {
                    val verdictIcon = when (dish.verdict) {
                        "GO" -> "🟢"
                        "NO_GO" -> "🔴"
                        else -> "🟡"
                    }
                    details.add("$verdictIcon ${dish.dishName}")
                }

                notificationManager.postFamilySyncDigest(summary, details)
            }

            val photoDetails = when {
                photosUploadedCount > 0 && photosDownloadedCount > 0 -> ", $photosUploadedCount photos uploaded, $photosDownloadedCount downloaded"
                photosUploadedCount > 0 -> ", $photosUploadedCount photos uploaded"
                photosDownloadedCount > 0 -> ", $photosDownloadedCount photos downloaded"
                else -> ""
            }

            val successMsg = "Synced with Google Drive ($mergedRemoteCount updates merged$photoDetails)"
            Log.d("PalateLog_Sync", "Sync SUCCESS: $successMsg")
            _syncStatus.value = SyncStatus.Success(successMsg, now)
        } catch (e: GoogleAuthException) {
            Log.e("PalateLog_Sync", "GoogleAuthException during sync: ${e.message}", e)
            _syncStatus.value = SyncStatus.Error(e.message ?: "Google Auth required")
            notificationManager.postSyncAuthAlert(e.message ?: "Google Auth required", isFolderPermissionDenied = false)
            throw e
        } catch (e: Exception) {
            Log.e("PalateLog_Sync", "Exception during sync: ${e.localizedMessage}", e)
            _syncStatus.value = SyncStatus.Error(e.localizedMessage ?: "Drive Sync failed")

            val msg = e.message.orEmpty()
            val isAuthOrPermissionError = msg.contains("401") || msg.contains("403") ||
                msg.contains("AccessDenied", ignoreCase = true) ||
                msg.contains("appNotAuthorizedToChild", ignoreCase = true) ||
                msg.contains("insufficientFilePermissions", ignoreCase = true)

            if (isAuthOrPermissionError) {
                val isFolderDenied = msg.contains("403") || msg.contains("AccessDenied", ignoreCase = true) || msg.contains("appNotAuthorizedToChild", ignoreCase = true)
                notificationManager.postSyncAuthAlert(msg, isFolderPermissionDenied = isFolderDenied)
            }
            throw e
        }
    }

    /**
     * Exports all local shared records modified since the given timestamp.
     */
    suspend fun exportSharedPayload(sinceTimestamp: Long = 0L): SyncPayload = withContext(Dispatchers.IO) {
        val modifiedRestaurants = restaurantDao.getModifiedSince(sinceTimestamp).filter { it.isShared }
        val modifiedVisits = visitDao.getModifiedSince(sinceTimestamp).filter { it.isShared }
        val modifiedDishes = dishDao.getModifiedSince(sinceTimestamp).filter { it.isShared }

        SyncPayload(
            deviceId = deviceId,
            timestamp = System.currentTimeMillis(),
            restaurants = modifiedRestaurants.map {
                SyncRestaurant(
                    id = it.id,
                    name = it.name,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    address = it.address,
                    city = it.city,
                    highwayOrRoute = it.highwayOrRoute,
                    cuisine = it.cuisine,
                    restaurantType = it.restaurantType,
                    overallVerdict = it.overallVerdict,
                    tags = it.tags,
                    isShared = it.isShared,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted
                )
            },
            visits = modifiedVisits.map {
                val uris = if (it.billReceiptPhotoUri.isNullOrBlank()) emptyList() else it.billReceiptPhotoUri.split(",").filter { s -> s.isNotBlank() }
                val names = uris.mapNotNull { uri -> extractPhotoFileName(uri) }
                SyncVisit(
                    id = it.id,
                    restaurantId = it.restaurantId,
                    visitDate = it.visitDate,
                    channel = it.channel,
                    visitVerdict = it.visitVerdict,
                    billAmount = it.billAmount,
                    billReceiptPhotoName = names.firstOrNull(),
                    photoNames = names,
                    generalNotes = it.generalNotes,
                    createdByMemberId = it.createdByMemberId,
                    createdByMemberName = it.createdByMemberName,
                    isShared = it.isShared,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted
                )
            },
            dishes = modifiedDishes.map {
                SyncDish(
                    id = it.id,
                    visitId = it.visitId,
                    restaurantId = it.restaurantId,
                    dishName = it.dishName,
                    category = it.category,
                    verdict = it.verdict,
                    tags = it.tags,
                    memberPreferences = it.memberPreferences,
                    price = it.price,
                    photoNames = it.photoUris.mapNotNull { uri -> extractPhotoFileName(uri) },
                    notes = it.notes,
                    isShared = it.isShared,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted
                )
            }
        )
    }

    /**
     * Ingests a remote SyncPayload into the local database using Last-Write-Wins (LWW).
     */
    suspend fun importPayload(payload: SyncPayload): Unit = withContext(Dispatchers.IO) {
        _syncStatus.value = SyncStatus.Syncing
        try {
            // 1. Merge Restaurants
            val restaurantEntities = payload.restaurants.map {
                RestaurantEntity(
                    id = it.id,
                    name = it.name,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    address = it.address,
                    city = it.city,
                    highwayOrRoute = it.highwayOrRoute,
                    cuisine = it.cuisine,
                    restaurantType = it.restaurantType,
                    overallVerdict = it.overallVerdict,
                    tags = it.tags,
                    isShared = it.isShared,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted
                )
            }
            for (r in restaurantEntities) {
                val existing = restaurantDao.getRestaurantByIdSync(r.id)
                if (existing == null || r.updatedAt > existing.updatedAt) {
                    restaurantDao.insert(r)
                }
            }

            // 2. Merge Visits
            val visitEntities = payload.visits.map {
                val names = if (it.photoNames.isNotEmpty()) it.photoNames else listOfNotNull(it.billReceiptPhotoName)
                val uris = names.mapNotNull { name -> getLocalPhotoUri(name) }
                val visitPhotosString = if (uris.isEmpty()) null else uris.joinToString(",")

                VisitEntryEntity(
                    id = it.id,
                    restaurantId = it.restaurantId,
                    visitDate = it.visitDate,
                    channel = it.channel,
                    visitVerdict = it.visitVerdict,
                    billAmount = it.billAmount,
                    billReceiptPhotoUri = visitPhotosString,
                    generalNotes = it.generalNotes,
                    createdByMemberId = it.createdByMemberId,
                    createdByMemberName = it.createdByMemberName,
                    isShared = it.isShared,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted
                )
            }
            visitDao.insertAll(visitEntities)

            // 3. Merge Dishes
            val dishEntities = payload.dishes.map {
                val photoUris = it.photoNames.mapNotNull { name -> getLocalPhotoUri(name) }
                DishFeedbackEntity(
                    id = it.id,
                    visitId = it.visitId,
                    restaurantId = it.restaurantId,
                    dishName = it.dishName,
                    category = it.category,
                    verdict = it.verdict,
                    tags = it.tags,
                    memberPreferences = it.memberPreferences,
                    price = it.price,
                    photoUris = photoUris,
                    notes = it.notes,
                    isShared = it.isShared,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted
                )
            }
            dishDao.insertAll(dishEntities)

            val now = System.currentTimeMillis()
            setLastSyncTimestamp(now)
            _syncStatus.value = SyncStatus.Success("Synced ${payload.restaurants.size} places & ${payload.dishes.size} dishes", now)
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error(e.localizedMessage ?: "Sync failed")
        }
    }

    /**
     * Exports full database to a JSON file in app cache or storage for manual backup/sharing.
     */
    suspend fun exportToJsonFile(): File = withContext(Dispatchers.IO) {
        val payload = exportSharedPayload(0L)
        val jsonString = json.encodeToString(payload)
        val exportFile = File(context.cacheDir, "palatelog_backup_${System.currentTimeMillis()}.json")
        exportFile.writeText(jsonString)
        exportFile
    }

    /**
     * Imports from a JSON string.
     */
    suspend fun importFromJsonString(jsonString: String) {
        val payload = json.decodeFromString<SyncPayload>(jsonString)
        importPayload(payload)
    }
}
