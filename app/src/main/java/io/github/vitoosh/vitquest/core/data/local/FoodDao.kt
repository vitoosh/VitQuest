package io.github.vitoosh.vitquest.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FoodDao {

    // ---- Foods ----

    /** Case-insensitive search on both names; favourites first, then alphabetical. */
    @Transaction
    @Query(
        """
        SELECT * FROM foods
        WHERE (:query = '' OR name_id LIKE '%' || :query || '%' OR name_en LIKE '%' || :query || '%')
        ORDER BY is_favorite DESC, name_id COLLATE NOCASE
        LIMIT :limit
        """,
    )
    abstract fun searchFoods(query: String, limit: Int): Flow<List<FoodWithServings>>

    @Transaction
    @Query("SELECT * FROM foods WHERE is_favorite = 1 ORDER BY name_id COLLATE NOCASE")
    abstract fun observeFavorites(): Flow<List<FoodWithServings>>

    /** Foods most recently logged, newest first. */
    @Transaction
    @Query(
        """
        SELECT foods.* FROM foods
        JOIN (SELECT food_id, MAX(created_at_epoch_ms) AS last_used FROM food_log_entries
              WHERE food_id IS NOT NULL GROUP BY food_id) recent ON recent.food_id = foods.id
        ORDER BY recent.last_used DESC
        LIMIT :limit
        """,
    )
    abstract fun observeRecent(limit: Int): Flow<List<FoodWithServings>>

    @Transaction
    @Query("SELECT * FROM foods WHERE id = :id")
    abstract suspend fun getFood(id: Long): FoodWithServings?

    @Query("SELECT * FROM foods WHERE seed_key = :seedKey")
    abstract suspend fun getBySeedKey(seedKey: String): FoodEntity?

    @Query("UPDATE foods SET is_favorite = :favorite WHERE id = :id")
    abstract suspend fun setFavorite(id: Long, favorite: Boolean)

    @Insert
    abstract suspend fun insertFood(food: FoodEntity): Long

    @Update
    abstract suspend fun updateFood(food: FoodEntity)

    @Query("DELETE FROM foods WHERE id = :id AND is_custom = 1")
    abstract suspend fun deleteCustomFood(id: Long)

    @Insert
    abstract suspend fun insertServings(servings: List<FoodServingEntity>)

    @Query("DELETE FROM food_servings WHERE food_id = :foodId")
    abstract suspend fun deleteServings(foodId: Long)

    /** Inserts or updates a food (matched by seed key for built-ins) together with its servings. */
    @Transaction
    open suspend fun upsertFoodWithServings(food: FoodEntity, servings: List<FoodServingEntity>): Long {
        val existing = food.seedKey?.let { getBySeedKey(it) }
        val id = when {
            existing != null -> {
                // Keep the row id and the user's favourite flag when reference values are refreshed.
                updateFood(food.copy(id = existing.id, isFavorite = existing.isFavorite, createdAtEpochMs = existing.createdAtEpochMs))
                existing.id
            }
            food.id != 0L -> {
                updateFood(food)
                food.id
            }
            else -> insertFood(food)
        }
        deleteServings(id)
        insertServings(servings.mapIndexed { index, serving -> serving.copy(id = 0, foodId = id, position = index) })
        return id
    }

    // ---- Food diary ----

    @Query("SELECT * FROM food_log_entries WHERE date = :date ORDER BY created_at_epoch_ms")
    abstract fun observeLog(date: String): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_log_entries WHERE date BETWEEN :from AND :to ORDER BY date, created_at_epoch_ms")
    abstract fun observeLogRange(from: String, to: String): Flow<List<FoodLogEntity>>

    @Query("SELECT DISTINCT date FROM food_log_entries WHERE date BETWEEN :from AND :to")
    abstract fun observeLoggedDates(from: String, to: String): Flow<List<String>>

    @Query("SELECT * FROM food_log_entries WHERE id = :id")
    abstract suspend fun getLogEntry(id: Long): FoodLogEntity?

    @Insert
    abstract suspend fun insertLog(entry: FoodLogEntity): Long

    @Update
    abstract suspend fun updateLog(entry: FoodLogEntity)

    @Query("DELETE FROM food_log_entries WHERE id = :id")
    abstract suspend fun deleteLog(id: Long)
}
