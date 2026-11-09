package com.prismpixelvertex.palatelog.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DishFeedbackDao {

    @Query("SELECT * FROM dish_feedbacks WHERE restaurantId = :restaurantId AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getDishesForRestaurant(restaurantId: String): Flow<List<DishFeedbackEntity>>

    @Query("SELECT * FROM dish_feedbacks WHERE restaurantId = :restaurantId AND isDeleted = 0")
    suspend fun getDishesForRestaurantSync(restaurantId: String): List<DishFeedbackEntity>

    @Query("SELECT * FROM dish_feedbacks WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllDishes(): Flow<List<DishFeedbackEntity>>

    @Query("SELECT * FROM dish_feedbacks WHERE visitId = :visitId AND isDeleted = 0")
    fun getDishesForVisit(visitId: String): Flow<List<DishFeedbackEntity>>

    @Query("""
        SELECT * FROM dish_feedbacks
        WHERE isDeleted = 0
          AND dishName LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    fun searchDishes(query: String): Flow<List<DishFeedbackEntity>>

    @Query("SELECT DISTINCT dishName FROM dish_feedbacks WHERE restaurantId = :restaurantId AND isDeleted = 0")
    suspend fun getDistinctDishNamesForRestaurant(restaurantId: String): List<String>

    @Query("SELECT * FROM dish_feedbacks WHERE updatedAt > :sinceTimestamp")
    suspend fun getModifiedSince(sinceTimestamp: Long): List<DishFeedbackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dish: DishFeedbackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dishes: List<DishFeedbackEntity>)

    @Update
    suspend fun update(dish: DishFeedbackEntity)

    @Query("UPDATE dish_feedbacks SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE dish_feedbacks SET isDeleted = 1, updatedAt = :timestamp WHERE visitId = :visitId")
    suspend fun softDeleteForVisit(visitId: String, timestamp: Long = System.currentTimeMillis())
}
