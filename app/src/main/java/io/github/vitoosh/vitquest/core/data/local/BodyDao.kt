package io.github.vitoosh.vitquest.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class DayTotal(val date: String, val total: Int)

@Dao
abstract class BodyDao {

    @Query("SELECT * FROM weight_entries ORDER BY date")
    abstract fun observeWeights(): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entries ORDER BY date DESC LIMIT 1")
    abstract suspend fun getLatestWeight(): WeightEntryEntity?

    @Query("SELECT * FROM weight_entries WHERE date = :date")
    abstract suspend fun getWeightOn(date: String): WeightEntryEntity?

    @Upsert
    abstract suspend fun upsertWeight(entry: WeightEntryEntity)

    @Query("DELETE FROM weight_entries WHERE id = :id")
    abstract suspend fun deleteWeight(id: Long)

    @Query("SELECT * FROM body_measurements ORDER BY date")
    abstract fun observeMeasurements(): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurements WHERE date = :date")
    abstract suspend fun getMeasurementOn(date: String): BodyMeasurementEntity?

    @Upsert
    abstract suspend fun upsertMeasurement(entry: BodyMeasurementEntity)

    @Query("DELETE FROM body_measurements WHERE id = :id")
    abstract suspend fun deleteMeasurement(id: Long)

    // ---- Water ----

    @Query("SELECT * FROM water_logs WHERE date BETWEEN :from AND :to ORDER BY logged_at_epoch_ms")
    abstract fun observeWater(from: String, to: String): Flow<List<WaterLogEntity>>

    @Query("SELECT date, SUM(amount_ml) AS total FROM water_logs WHERE date BETWEEN :from AND :to GROUP BY date")
    abstract fun observeWaterTotals(from: String, to: String): Flow<List<DayTotal>>

    @Insert
    abstract suspend fun insertWater(entry: WaterLogEntity): Long

    @Query("DELETE FROM water_logs WHERE id = :id")
    abstract suspend fun deleteWater(id: Long)
}
