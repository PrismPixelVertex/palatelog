package com.prismpixelvertex.palatelog.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.prismpixelvertex.palatelog.MainActivity
import com.prismpixelvertex.palatelog.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PalateNotificationManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    companion object {
        private const val TAG = "PalateLog_Notify"

        const val CHANNEL_SYNC_HEALTH = "channel_sync_health"
        const val CHANNEL_FAMILY_SYNC = "channel_family_sync"

        const val NOTIFICATION_ID_SYNC_HEALTH = 1001
        const val NOTIFICATION_ID_FAMILY_SYNC = 1002

        const val EXTRA_NAV_TARGET = "extra_nav_target"
        const val NAV_TARGET_SETTINGS = "settings"
        const val NAV_TARGET_DASHBOARD = "dashboard"
    }

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * Creates and registers notification channels for API 26+.
     * Safe to call multiple times (idempotent).
     */
    fun initChannels() {
        val syncHealthChannel = NotificationChannel(
            CHANNEL_SYNC_HEALTH,
            context.getString(R.string.notification_channel_sync_health_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_sync_health_desc)
            enableVibration(false)
        }

        val familySyncChannel = NotificationChannel(
            CHANNEL_FAMILY_SYNC,
            context.getString(R.string.notification_channel_family_sync_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_family_sync_desc)
            enableVibration(false)
        }

        notificationManager.createNotificationChannel(syncHealthChannel)
        notificationManager.createNotificationChannel(familySyncChannel)
        Log.d(TAG, "Notification channels registered successfully")
    }

    /**
     * Posts an actionable sync health alert when human intervention is needed
     * (e.g. OAuth token expired or shared family folder permission denied).
     *
     * Tapping the notification takes the user directly to the Settings screen.
     */
    fun postSyncAuthAlert(errorMessage: String, isFolderPermissionDenied: Boolean = false) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.w(TAG, "Notifications disabled by user; skipping sync auth alert")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV_TARGET, NAV_TARGET_SETTINGS)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_SYNC_HEALTH,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val messageText = if (isFolderPermissionDenied) {
            context.getString(R.string.notification_sync_permission_denied_text)
        } else {
            context.getString(R.string.notification_sync_auth_expired_text)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC_HEALTH)
            .setSmallIcon(R.drawable.ic_notification_restaurant)
            .setColor(0xF59E0B.toInt()) // Amber Primary
            .setContentTitle(context.getString(R.string.notification_sync_auth_title))
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID_SYNC_HEALTH, notification)
            Log.i(TAG, "Dispatched sync auth alert notification: $errorMessage")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException posting sync auth alert", e)
        }
    }

    /**
     * Automatically dismisses the sync health alert once sync recovers successfully.
     */
    fun clearSyncAuthAlert() {
        try {
            notificationManager.cancel(NOTIFICATION_ID_SYNC_HEALTH)
            Log.d(TAG, "Cleared sync auth alert notification")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing sync auth alert", e)
        }
    }

    /**
     * Posts a low-priority, batched summary digest when background sync merges updates from family devices.
     *
     * Tapping the notification opens the Dashboard / Explore view.
     */
    fun postFamilySyncDigest(summary: String, details: List<String> = emptyList()) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.w(TAG, "Notifications disabled by user; skipping family sync digest")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV_TARGET, NAV_TARGET_DASHBOARD)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_FAMILY_SYNC,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_FAMILY_SYNC)
            .setSmallIcon(R.drawable.ic_notification_restaurant)
            .setColor(0x10B981.toInt()) // VerdictGoGreen Emerald
            .setContentTitle(context.getString(R.string.notification_family_sync_title))
            .setContentText(summary)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (details.isNotEmpty()) {
            val inboxStyle = NotificationCompat.InboxStyle().setSummaryText(summary)
            for (line in details.take(5)) {
                inboxStyle.addLine(line)
            }
            builder.setStyle(inboxStyle)
        }

        try {
            notificationManager.notify(NOTIFICATION_ID_FAMILY_SYNC, builder.build())
            Log.i(TAG, "Dispatched family sync digest notification: $summary")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException posting family sync digest", e)
        }
    }
}
