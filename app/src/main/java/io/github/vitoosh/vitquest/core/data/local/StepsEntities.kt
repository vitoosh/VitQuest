package io.github.vitoosh.vitquest.core.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per day and source. The resolver chooses which source to show; rows are never summed. */
@Entity(tableName = "daily_steps", primaryKeys = ["date", "source"])
data class DailyStepsEntity(
    /** Local date as ISO-8601 (yyyy-MM-dd), so text order equals date order. */
    val date: String,
    /** StepSource enum name. */
    val source: String,
    val steps: Int,
    @ColumnInfo(name = "updated_at_epoch_ms") val updatedAtEpochMs: Long,
)

/** Last processed value of the phone step counter (single row). */
@Entity(tableName = "step_sensor_state")
data class StepSensorStateEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    @ColumnInfo(name = "boot_count") val bootCount: Int?,
    val counter: Long,
    @ColumnInfo(name = "recorded_at_epoch_ms") val recordedAtEpochMs: Long,
)
