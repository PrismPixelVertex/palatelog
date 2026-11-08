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
        const val CHANNEL_RADAR_SERVICE = "channel_radar_service"
        const val CHANNEL_RADAR_ALERTS = "channel_radar_alerts"

        const val NOTIFICATION_ID_SYNC_HEALTH = 1001
        const val NOTIFICATION_ID_FAMILY_SYNC = 1002
        const val NOTIFICATION_ID_RADAR_SERVICE = 2001
        const val NOTIFICATION_ID_RADAR_ALERT = 2100

        const val EXTRA_NAV_TARGET = "extra_nav_target"
        const val NAV_TARGET_SETTINGS = "settings"
        const val NAV_TARGET_DASHBOARD = "dashboard"
        const val NAV_TARGET_RESTAURANT_DETAIL = "restaurant_detail"
        const val EXTRA_RESTAURANT_ID = "extra_restaurant_id"
        const val ACTION_STOP_RADAR = "com.prismpixelvertex.palatelog.ACTION_STOP_RADAR"
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

        val radarServiceChannel = NotificationChannel(
            CHANNEL_RADAR_SERVICE,
            context.getString(R.string.notification_channel_radar_service_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_radar_service_desc)
            enableVibration(false)
        }

        val radarAlertsChannel = NotificationChannel(
            CHANNEL_RADAR_ALERTS,
            context.getString(R.string.notification_channel_radar_alerts_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_radar_alerts_desc)
            enableVibration(true)
            enableLights(true)
        }

        notificationManager.createNotificationChannel(syncHealthChannel)
        notificationManager.createNotificationChannel(familySyncChannel)
        notificationManager.createNotificationChannel(radarServiceChannel)
        notificationManager.createNotificationChannel(radarAlertsChannel)
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

    /**
     * Builds the persistent foreground notification for the running Palate Radar trip service.
     * Includes a 1-tap "Turn Off" action to immediately stop tracking and deregister geofences.
     */
    fun buildRadarServiceNotification(stopCount: Int): android.app.Notification {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV_TARGET, NAV_TARGET_DASHBOARD)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_RADAR_SERVICE,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_STOP_RADAR
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val stopPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_RADAR_SERVICE + 1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = when {
            stopCount == 0 -> context.getString(R.string.radar_service_notification_scanning_none)
            stopCount == 1 -> context.getString(R.string.radar_service_notification_scanning_single, stopCount)
            else -> context.getString(R.string.radar_service_notification_scanning_plural, stopCount)
        }

        return NotificationCompat.Builder(context, CHANNEL_RADAR_SERVICE)
            .setSmallIcon(R.drawable.ic_notification_restaurant)
            .setColor(0x0284C7.toInt()) // Radar Blue
            .setContentTitle(context.getString(R.string.radar_service_notification_title))
            .setContentText(statusText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(statusText))
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.radar_service_action_stop),
                stopPendingIntent
            )
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Dispatches a high-priority heads-up proximity alert when entering a 500m geofence.
     * Uses a single dedicated notification ID (NOTIFICATION_ID_RADAR_ALERT) to prevent
     * status bar flooding, updating smoothly to the most immediate upcoming spot.
     *
     * Tapping opens the RestaurantDetailScreen for that spot.
     */
    fun postRadarProximityAlert(
        restaurantId: String,
        restaurantName: String,
        overallVerdict: String,
        summaryText: String,
        additionalNearbyCount: Int = 0,
    ) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.w(TAG, "Notifications disabled; skipping proximity alert for $restaurantName")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV_TARGET, NAV_TARGET_RESTAURANT_DETAIL)
            putExtra(EXTRA_RESTAURANT_ID, restaurantId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_RADAR_ALERT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, color) = when (overallVerdict) {
            "NO_GO" -> Pair(context.getString(R.string.radar_alert_no_go_title), 0xEF4444.toInt()) // Crimson
            "GO" -> Pair(context.getString(R.string.radar_alert_go_title), 0x10B981.toInt()) // Emerald
            else -> Pair(context.getString(R.string.radar_alert_caution_title), 0xF59E0B.toInt()) // Amber
        }

        val details = summaryText.ifBlank { context.getString(R.string.radar_alert_no_details) }
        val additionalNote = if (additionalNearbyCount > 0) {
            context.getString(
                if (additionalNearbyCount == 1) R.string.radar_alert_additional_spots
                else R.string.radar_alert_additional_spots_plural,
                additionalNearbyCount
            )
        } else {
            ""
        }
        val content = context.getString(R.string.radar_alert_body_format, restaurantName, details) + additionalNote

        val notification = NotificationCompat.Builder(context, CHANNEL_RADAR_ALERTS)
            .setSmallIcon(R.drawable.ic_notification_restaurant)
            .setColor(color)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID_RADAR_ALERT, notification)
            Log.i(TAG, "Dispatched radar proximity alert for $restaurantName ($overallVerdict)")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException posting radar alert", e)
        }
    }

    /**
     * Clears any active proximity alerts.
     */
    fun clearRadarProximityAlert() {
        try {
            notificationManager.cancel(NOTIFICATION_ID_RADAR_ALERT)
            Log.d(TAG, "Cleared radar proximity alert")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing radar proximity alert", e)
        }
    }

    /**
     * Clears all radar notifications (both the ongoing trip service notification
     * and any active proximity alert heads-up cards).
     */
    fun clearAllRadarNotifications() {
        try {
            notificationManager.cancel(NOTIFICATION_ID_RADAR_SERVICE)
            notificationManager.cancel(NOTIFICATION_ID_RADAR_ALERT)
            Log.d(TAG, "Cleared all radar notifications")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all radar notifications", e)
        }
    }
}
