package com.prismpixelvertex.palatelog.core.radar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.prismpixelvertex.palatelog.core.database.DishFeedbackDao
import com.prismpixelvertex.palatelog.core.database.RestaurantDao
import com.prismpixelvertex.palatelog.core.model.DishVerdict
import com.prismpixelvertex.palatelog.core.model.Verdict
import com.prismpixelvertex.palatelog.core.notification.PalateNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PalateRadarReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PalateRadar_Receiver"
        const val ACTION_RADAR_GEOFENCE = "com.prismpixelvertex.palatelog.ACTION_RADAR_GEOFENCE"
    }

    @Inject
    lateinit var restaurantDao: RestaurantDao

    @Inject
    lateinit var dishFeedbackDao: DishFeedbackDao

    @Inject
    lateinit var notificationManager: PalateNotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RADAR_GEOFENCE) return

        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "GeofencingEvent error: $errorMessage (code: ${geofencingEvent.errorCode})")
            return
        }

        if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences.orEmpty()
            Log.i(TAG, "Geofence transition ENTER triggered for ${triggeringGeofences.size} spots")

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val candidateRestaurants = triggeringGeofences.mapNotNull { geofence ->
                        restaurantDao.getRestaurantByIdSync(geofence.requestId)
                    }

                    if (candidateRestaurants.isNotEmpty()) {
                        // Prioritize NO_GO regrets first (0), CAUTION second (1), GO third (2)
                        val sorted = candidateRestaurants.sortedBy { spot ->
                            when (spot.overallVerdict) {
                                Verdict.NO_GO.name -> 0
                                Verdict.CAUTION.name -> 1
                                else -> 2
                            }
                        }

                        val topSpot = sorted.first()
                        val additionalCount = sorted.size - 1

                        val dishes = dishFeedbackDao.getDishesForRestaurantSync(topSpot.id)
                        val summaryText = buildProximitySummary(topSpot.overallVerdict, dishes, topSpot.tags)

                        notificationManager.postRadarProximityAlert(
                            restaurantId = topSpot.id,
                            restaurantName = topSpot.name,
                            overallVerdict = topSpot.overallVerdict,
                            summaryText = summaryText,
                            additionalNearbyCount = additionalCount
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling geofence transition in background", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun buildProximitySummary(
        verdict: String,
        dishes: List<com.prismpixelvertex.palatelog.core.database.DishFeedbackEntity>,
        restaurantTags: List<String>
    ): String {
        return when (verdict) {
            Verdict.NO_GO.name -> {
                val avoidDishes = dishes.filter { it.verdict == DishVerdict.NO_GO.name }.map { it.dishName }
                if (avoidDishes.isNotEmpty()) {
                    "Avoid: " + avoidDishes.distinct().take(2).joinToString(", ")
                } else if (restaurantTags.isNotEmpty()) {
                    restaurantTags.take(2).joinToString(", ")
                } else {
                    "Marked as Roadside Regret (NO-GO)"
                }
            }
            Verdict.GO.name -> {
                val mustTryDishes = dishes.filter { it.verdict == DishVerdict.GO.name }.map { it.dishName }
                if (mustTryDishes.isNotEmpty()) {
                    "Must Try: " + mustTryDishes.distinct().take(2).joinToString(", ")
                } else if (restaurantTags.isNotEmpty()) {
                    restaurantTags.take(2).joinToString(", ")
                } else {
                    "Saved Favorite Highway Stop (GO)"
                }
            }
            else -> {
                if (restaurantTags.isNotEmpty()) {
                    restaurantTags.take(2).joinToString(", ")
                } else {
                    "Mixed experience / Exercise caution"
                }
            }
        }
    }
}
