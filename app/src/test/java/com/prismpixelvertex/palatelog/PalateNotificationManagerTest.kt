package com.prismpixelvertex.palatelog

import com.prismpixelvertex.palatelog.core.notification.PalateNotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PalateNotificationManagerTest {

    @Test
    fun channelIds_areDistinctAndValid() {
        assertNotEquals(
            PalateNotificationManager.CHANNEL_SYNC_HEALTH,
            PalateNotificationManager.CHANNEL_FAMILY_SYNC
        )
        assertEquals("channel_sync_health", PalateNotificationManager.CHANNEL_SYNC_HEALTH)
        assertEquals("channel_family_sync", PalateNotificationManager.CHANNEL_FAMILY_SYNC)
    }

    @Test
    fun notificationIds_areUnique() {
        assertNotEquals(
            PalateNotificationManager.NOTIFICATION_ID_SYNC_HEALTH,
            PalateNotificationManager.NOTIFICATION_ID_FAMILY_SYNC
        )
        assertEquals(1001, PalateNotificationManager.NOTIFICATION_ID_SYNC_HEALTH)
        assertEquals(1002, PalateNotificationManager.NOTIFICATION_ID_FAMILY_SYNC)
    }

    @Test
    fun navigationTargets_matchExpectedRoutes() {
        assertEquals("settings", PalateNotificationManager.NAV_TARGET_SETTINGS)
        assertEquals("dashboard", PalateNotificationManager.NAV_TARGET_DASHBOARD)
    }
}
