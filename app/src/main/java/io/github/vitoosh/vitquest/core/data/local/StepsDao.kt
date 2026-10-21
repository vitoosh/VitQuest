package io.github.vitoosh.vitquest.core.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.github.vitoosh.vitquest.core.domain.steps.SensorCheckpoint
import io.github.vitoosh.vitquest.core.domain.steps.SensorReading
import io.github.vitoosh.vitquest.core.domain.steps.SensorUpdate
import io.github.vitoosh.vitquest.core.domain.steps.StepCounterMath
import io.github.vitoosh.vitquest.core.model.StepSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

@Dao
abstract class StepsDao {

    @Query("SELECT * FROM daily_steps WHERE date BETWEEN :from AND :to")
    abstract fun observeRange(from: String, to: String): Flow<List<DailyStepsEntity>>

    @Query("SELECT steps FROM daily_steps WHERE date = :date AND source = :source")
    abstract suspend fun getSteps(date: String, source: String): Int?

    @Upsert
    abstract suspend fun upsert(rows: List<DailyStepsEntity>)

    @Query("DELETE FROM daily_steps WHERE date = :date AND source = :source")
    abstract suspend fun delete(date: String, source: String)

    @Query("DELETE FROM daily_steps WHERE source = :source AND date BETWEEN :from AND :to")
    abstract suspend fun deleteSourceInRange(source: String, from: String, to: String)

    @Query("SELECT * FROM step_sensor_state WHERE id = $SINGLE_ROW_ID")
    abstract suspend fun getSensorState(): StepSensorStateEntity?

    @Query("SELECT * FROM step_sensor_state WHERE id = $SINGLE_ROW_ID")
    abstract fun observeSensorState(): Flow<StepSensorStateEntity?>

    /** Adds [steps] to a day's running total for [source]. */
    @Transaction
    open suspend fun addSteps(date: LocalDate, source: StepSource, steps: Int, now: Instant) {
        if (steps <= 0) return
        val existing = getSteps(date.toString(), source.name) ?: 0
        upsert(listOf(DailyStepsEntity(date.toString(), source.name, existing + steps, now.toEpochMilli())))
    }

    @Upsert
    abstract suspend fun upsertSensorState(state: StepSensorStateEntity)

    /**
     * Replaces cached Health Connect totals for the range. Days Health Connect
     * has no data for are removed, so another source can be shown for them.
     */
    @Transaction
    open suspend fun replaceHealthConnectDays(from: LocalDate, to: LocalDate, rows: List<DailyStepsEntity>) {
        deleteSourceInRange(StepSource.HEALTH_CONNECT.name, from.toString(), to.toString())
        upsert(rows)
    }

    /** Reads the previous checkpoint, adds the new steps per day, and stores the new checkpoint atomically. */
    @Transaction
    open suspend fun applySensorReading(reading: SensorReading, zone: ZoneId, now: Instant): SensorUpdate {
        val previous = getSensorState()?.let {
            SensorCheckpoint(it.bootCount, it.counter, Instant.ofEpochMilli(it.recordedAtEpochMs))
        }
        val update = StepCounterMath.apply(previous, reading, zone)
        val rows = update.stepsByDate.map { (date, steps) ->
            val existing = getSteps(date.toString(), StepSource.PHONE_SENSOR.name) ?: 0
            DailyStepsEntity(date.toString(), StepSource.PHONE_SENSOR.name, existing + steps, now.toEpochMilli())
        }
        if (rows.isNotEmpty()) upsert(rows)
        upsertSensorState(
            StepSensorStateEntity(
                bootCount = update.checkpoint.bootCount,
                counter = update.checkpoint.counter,
                recordedAtEpochMs = update.checkpoint.recordedAt.toEpochMilli(),
            ),
        )
        return update
    }
}
