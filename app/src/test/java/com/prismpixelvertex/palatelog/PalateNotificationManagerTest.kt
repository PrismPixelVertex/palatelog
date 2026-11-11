package com.prismpixelvertex.palatelog

import com.prismpixelvertex.palatelog.core.notification.PalateNotificationManager
import org.junit.Assert.assertEquals
import org.junit.Test

class PalateNotificationManagerTest {

    @Test
    fun channelIds_areDistinctAndValid() {
        val channels = listOf(
            PalateNotificationManager.CHANNEL_SYNC_HEALTH,
            PalateNotificationManager.CHANNEL_FAMILY_SYNC,
            PalateNotificationManager.CHANNEL_RADAR_SERVICE,
            PalateNotificationManager.CHANNEL_RADAR_ALERTS,
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

    @Test
    fun formatSyncDigestDetails_formatsRestaurantsWithVerdictTagsAndDishesWithColorIcons() {
        val restaurants = listOf(
            com.prismpixelvertex.palatelog.core.sync.SyncRestaurant(
                id = "r1",
                name = "Jems Pizza",
                latitude = 12.97,
                longitude = 77.59,
                restaurantType = "DINE_IN",
                overallVerdict = "GO",
                createdAt = 1000L,
                updatedAt = 1000L
            ),
            com.prismpixelvertex.palatelog.core.sync.SyncRestaurant(
                id = "r2",
                name = "Thata Coffee",
                latitude = 12.98,
                longitude = 77.60,
                restaurantType = "DHABA",
                overallVerdict = "NO_GO",
                createdAt = 1000L,
                updatedAt = 1000L
            ),
            com.prismpixelvertex.palatelog.core.sync.SyncRestaurant(
                id = "r3",
                name = "California Burrito",
                latitude = 12.99,
                longitude = 77.61,
                restaurantType = "CAFE",
                overallVerdict = "CAUTION",
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val dishes = listOf(
            com.prismpixelvertex.palatelog.core.sync.SyncDish(
                id = "d1",
                visitId = "v1",
                restaurantId = "r1",
                dishName = "Crispy Taco",
                category = "MAIN",
                verdict = "GO",
                updatedAt = 1000L
            ),
            com.prismpixelvertex.palatelog.core.sync.SyncDish(
                id = "d2",
                visitId = "v2",
                restaurantId = "r2",
                dishName = "Big Bell Box Veg 5 in 1",
                category = "MAIN",
                verdict = "NO_GO",
                updatedAt = 1000L
            )
        )

        val details = com.prismpixelvertex.palatelog.core.sync.SyncRepository.formatSyncDigestDetails(restaurants, dishes)

        assertEquals(5, details.size)
        assertEquals("📍 Jems Pizza [GO]", details[0])
        assertEquals("📍 Thata Coffee [NO-GO]", details[1])
        assertEquals("📍 California Burrito [CAUTION]", details[2])
        assertEquals("🟢 Crispy Taco", details[3])
        assertEquals("🔴 Big Bell Box Veg 5 in 1", details[4])
    }

    @Test
    fun formatSyncDigestDetails_capsAtMaxLinesAndDeduplicates() {
        val restaurants = listOf(
            com.prismpixelvertex.palatelog.core.sync.SyncRestaurant(id = "r1", name = "Spot A", latitude = 0.0, longitude = 0.0, restaurantType = "DINE_IN", overallVerdict = "GO", createdAt = 0L, updatedAt = 0L),
            com.prismpixelvertex.palatelog.core.sync.SyncRestaurant(id = "r1", name = "Spot A Duplicate", latitude = 0.0, longitude = 0.0, restaurantType = "DINE_IN", overallVerdict = "GO", createdAt = 0L, updatedAt = 0L),
            com.prismpixelvertex.palatelog.core.sync.SyncRestaurant(id = "r2", name = "Spot B", latitude = 0.0, longitude = 0.0, restaurantType = "DINE_IN", overallVerdict = "NO_GO", createdAt = 0L, updatedAt = 0L),
            com.prismpixelvertex.palatelog.core.sync.SyncRestaurant(id = "r3", name = "Spot C", latitude = 0.0, longitude = 0.0, restaurantType = "DINE_IN", overallVerdict = "CAUTION", createdAt = 0L, updatedAt = 0L),
            com.prismpixelvertex.palatelog.core.sync.SyncRestaurant(id = "r4", name = "Spot D", latitude = 0.0, longitude = 0.0, restaurantType = "DINE_IN", overallVerdict = "GO", createdAt = 0L, updatedAt = 0L)
        )

        val dishes = listOf(
            com.prismpixelvertex.palatelog.core.sync.SyncDish(id = "d1", visitId = "v1", restaurantId = "r1", dishName = "Dish 1", category = "MAIN", verdict = "GO", updatedAt = 0L),
            com.prismpixelvertex.palatelog.core.sync.SyncDish(id = "d2", visitId = "v1", restaurantId = "r1", dishName = "Dish 2", category = "MAIN", verdict = "MEH", updatedAt = 0L),
            com.prismpixelvertex.palatelog.core.sync.SyncDish(id = "d3", visitId = "v1", restaurantId = "r1", dishName = "Dish 3", category = "MAIN", verdict = "NO_GO", updatedAt = 0L)
        )

        // Capped at 5 lines total (up to 3 restaurants + remaining slots for dishes)
        val details = com.prismpixelvertex.palatelog.core.sync.SyncRepository.formatSyncDigestDetails(restaurants, dishes, maxLines = 5)
        assertEquals(5, details.size)
        assertEquals("📍 Spot A [GO]", details[0])
        assertEquals("📍 Spot B [NO-GO]", details[1])
        assertEquals("📍 Spot C [CAUTION]", details[2])
        assertEquals("🟢 Dish 1", details[3])
        assertEquals("🟡 Dish 2", details[4])
    }

    @Test
    fun formatSyncDigestDetails_handlesEmptyInputs() {
        val details = com.prismpixelvertex.palatelog.core.sync.SyncRepository.formatSyncDigestDetails(emptyList(), emptyList())
        assertEquals(0, details.size)
    }
}
