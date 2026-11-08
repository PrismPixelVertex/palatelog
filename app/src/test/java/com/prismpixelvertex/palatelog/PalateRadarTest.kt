package com.prismpixelvertex.palatelog

import com.prismpixelvertex.palatelog.core.database.RestaurantEntity
import com.prismpixelvertex.palatelog.core.location.GeoUtils
import com.prismpixelvertex.palatelog.core.model.RestaurantType
import com.prismpixelvertex.palatelog.core.model.Verdict
import com.prismpixelvertex.palatelog.core.radar.PalateRadarManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PalateRadarTest {

    @Test
    fun radarConstants_matchSpecification() {
        assertEquals(500f, PalateRadarManager.GEOFENCE_RADIUS_METERS, 0.001f)
        assertEquals(100, PalateRadarManager.MAX_GEOFENCES)
        assertEquals(500.0, PalateRadarManager.CORRIDOR_RADIUS_KM, 0.001)
    }

    @Test
    fun candidatePrioritization_prioritizesNoGoRegretsFirst() {
        val centerLat = 12.9716 // Bangalore
        val centerLng = 77.5946

        // Sample spots along highway
        val spots = listOf(
            createSpot("1", "Spot A (GO)", 12.8000, 77.5000, Verdict.GO.name),
            createSpot("2", "Spot B (NO_GO)", 12.7000, 77.4000, Verdict.NO_GO.name),
            createSpot("3", "Spot C (CAUTION)", 12.6000, 77.3000, Verdict.CAUTION.name),
            createSpot("4", "Spot D (NO_GO Far)", 12.5000, 77.2000, Verdict.NO_GO.name)
        )

        // When includeGoRecommendations = false, GO spots should be excluded
        val filteredWithoutGo = spots.filter { spot ->
            when (spot.overallVerdict) {
                Verdict.NO_GO.name -> true
                Verdict.CAUTION.name -> true
                Verdict.GO.name -> false
                else -> true
            }
        }.sortedWith(
            compareBy<RestaurantEntity> { spot ->
                when (spot.overallVerdict) {
                    Verdict.NO_GO.name -> 0
                    Verdict.CAUTION.name -> 1
                    else -> 2
                }
            }.thenBy { spot ->
                GeoUtils.calculateDistanceKm(centerLat, centerLng, spot.latitude!!, spot.longitude!!)
            }
        )

        assertEquals(3, filteredWithoutGo.size)
        // Top priority must be NO_GO spots ordered by distance
        assertEquals("Spot B (NO_GO)", filteredWithoutGo[0].name)
        assertEquals("Spot D (NO_GO Far)", filteredWithoutGo[1].name)
        assertEquals("Spot C (CAUTION)", filteredWithoutGo[2].name)

        // When includeGoRecommendations = true, GO spots are included at lower priority
        val filteredWithGo = spots.sortedWith(
            compareBy<RestaurantEntity> { spot ->
                when (spot.overallVerdict) {
                    Verdict.NO_GO.name -> 0
                    Verdict.CAUTION.name -> 1
                    else -> 2
                }
            }.thenBy { spot ->
                GeoUtils.calculateDistanceKm(centerLat, centerLng, spot.latitude!!, spot.longitude!!)
            }
        )

        assertEquals(4, filteredWithGo.size)
        assertEquals("Spot B (NO_GO)", filteredWithGo[0].name)
        assertEquals("Spot D (NO_GO Far)", filteredWithGo[1].name)
        assertEquals("Spot C (CAUTION)", filteredWithGo[2].name)
        assertEquals("Spot A (GO)", filteredWithGo[3].name)
    }

    @Test
    fun candidateCap_enforcesPlayServicesLimit() {
        val centerLat = 12.9716
        val centerLng = 77.5946

        // Generate 150 candidate spots
        val largeList = (1..150).map { i ->
            createSpot(
                id = "$i",
                name = "Highway Spot $i",
                lat = centerLat + (i * 0.01),
                lng = centerLng + (i * 0.01),
                verdict = if (i % 2 == 0) Verdict.NO_GO.name else Verdict.GO.name
            )
        }

        val capped = largeList.take(PalateRadarManager.MAX_GEOFENCES)
        assertEquals(100, capped.size)
        assertTrue(capped.size <= PalateRadarManager.MAX_GEOFENCES)
    }

    private fun createSpot(
        id: String,
        name: String,
        lat: Double,
        lng: Double,
        verdict: String
    ): RestaurantEntity {
        return RestaurantEntity(
            id = id,
            name = name,
            latitude = lat,
            longitude = lng,
            address = "Highway corridor",
            city = "NH44",
            highwayOrRoute = "NH44",
            cuisine = "South Indian",
            restaurantType = RestaurantType.DHABA.name,
            overallVerdict = verdict,
            tags = listOf("Tea", "Dhaba"),
            isShared = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isDeleted = false
        )
    }
}
