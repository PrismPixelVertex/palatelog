package com.prismpixelvertex.palatelog

import com.prismpixelvertex.palatelog.core.notification.PalateNotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PalateNotificationManagerTest {

    @Test
    fun channelIds_areDistinctAndValid() {
        val channels = listOf(
            PalateNotificationManager.CHANNEL_SYNC_HEALTH,
            PalateNotificationManager.CHANNEL_FAMILY_SYNC,
            PalateNotificationManager.CHANNEL_RADAR_SERVICE,
            PalateNotificationManager.CHANNEL_RADAR_ALERTS
        )
        assertEquals(4, channels.distinct().size)
        assertEquals("channel_radar_service", PalateNotificationManager.CHANNEL_RADAR_SERVICE)
        assertEquals("channel_radar_alerts", PalateNotificationManager.CHANNEL_RADAR_ALERTS)
    }

    @Test
    fun notificationIds_areUnique() {
        val ids = listOf(
            PalateNotificationManager.NOTIFICATION_ID_SYNC_HEALTH,
            PalateNotificationManager.NOTIFICATION_ID_FAMILY_SYNC,
            PalateNotificationManager.NOTIFICATION_ID_RADAR_SERVICE,
            PalateNotificationManager.NOTIFICATION_ID_RADAR_ALERT
        )
        assertEquals(4, ids.distinct().size)
        assertEquals(2001, PalateNotificationManager.NOTIFICATION_ID_RADAR_SERVICE)
        assertEquals(2100, PalateNotificationManager.NOTIFICATION_ID_RADAR_ALERT)
    }

    @Test
    fun navigationTargets_matchExpectedRoutes() {
        assertEquals("settings", PalateNotificationManager.NAV_TARGET_SETTINGS)
        assertEquals("dashboard", PalateNotificationManager.NAV_TARGET_DASHBOARD)
        assertEquals("restaurant_detail", PalateNotificationManager.NAV_TARGET_RESTAURANT_DETAIL)
        assertEquals("extra_restaurant_id", PalateNotificationManager.EXTRA_RESTAURANT_ID)
    }
}
