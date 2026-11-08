package com.prismpixelvertex.palatelog.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RestaurantDao {

    @Query("SELECT * FROM restaurants WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllRestaurants(): Flow<List<RestaurantEntity>>

    @Query("SELECT * FROM restaurants WHERE id = :id AND isDeleted = 0")
    fun getRestaurantById(id: String): Flow<RestaurantEntity?>

    @Query("SELECT * FROM restaurants WHERE id = :id")
    suspend fun getRestaurantByIdSync(id: String): RestaurantEntity?

    @Query("""
        SELECT * FROM restaurants
        WHERE isDeleted = 0
          AND (name LIKE '%' || :query || '%'
               OR city LIKE '%' || :query || '%'
               OR cuisine LIKE '%' || :query || '%'
               OR highwayOrRoute LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
    """)
    fun searchRestaurants(query: String): Flow<List<RestaurantEntity>>

    @Query("""
        SELECT * FROM restaurants
        WHERE isDeleted = 0
          AND latitude BETWEEN :minLat AND :maxLat
          AND longitude BETWEEN :minLng AND :maxLng
    """)
    suspend fun getRestaurantsInBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): List<RestaurantEntity>

    @Query("""
        SELECT * FROM restaurants
        WHERE isDeleted = 0
          AND latitude IS NOT NULL
          AND longitude IS NOT NULL
          AND (latitude != 0.0 OR longitude != 0.0)
    """)
    suspend fun getAllRestaurantsWithCoordinates(): List<RestaurantEntity>

    @Query("SELECT * FROM restaurants WHERE updatedAt > :sinceTimestamp")
    suspend fun getModifiedSince(sinceTimestamp: Long): List<RestaurantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(restaurant: RestaurantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(restaurants: List<RestaurantEntity>)

    @Update
    suspend fun update(restaurant: RestaurantEntity)

    @Query("UPDATE restaurants SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())
}
