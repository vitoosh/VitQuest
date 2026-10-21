package io.github.vitoosh.vitquest.core.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** One finished (or ended early) workout, from the player or logged by hand. */
@Entity(tableName = "workout_sessions", indices = [Index("date")])
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    @ColumnInfo(name = "started_at_epoch_ms") val startedAtEpochMs: Long,
    @ColumnInfo(name = "ended_at_epoch_ms") val endedAtEpochMs: Long,
    val source: String,
    val focus: String?,
    val level: String?,
    val location: String?,
    val intensity: String?,
    @ColumnInfo(name = "planned_seconds") val plannedSeconds: Int,
    @ColumnInfo(name = "active_seconds") val activeSeconds: Int,
    @ColumnInfo(name = "exercises_completed") val exercisesCompleted: Int,
    @ColumnInfo(name = "exercises_total") val exercisesTotal: Int,
    val completed: Boolean,
    @ColumnInfo(name = "estimated_kcal") val estimatedKcal: Double?,
    val note: String?,
)

/** Exercise names are snapshots so history reads the same even if the library changes. */
@Entity(
    tableName = "workout_session_exercises",
    foreignKeys = [
        ForeignKey(entity = WorkoutSessionEntity::class, parentColumns = ["id"], childColumns = ["session_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("session_id")],
)
data class WorkoutSessionExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    val position: Int,
    @ColumnInfo(name = "exercise_key") val exerciseKey: String,
    @ColumnInfo(name = "name_id") val nameId: String,
    @ColumnInfo(name = "name_en") val nameEn: String,
    @ColumnInfo(name = "planned_seconds") val plannedSeconds: Int,
    @ColumnInfo(name = "worked_seconds") val workedSeconds: Int,
    val status: String,
)

data class WorkoutSessionWithExercises(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "session_id")
    val exercises: List<WorkoutSessionExerciseEntity>,
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "target_days_per_week") val targetDaysPerWeek: Int,
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long,
)

@Entity(
    tableName = "habit_check_ins",
    primaryKeys = ["habit_id", "date"],
    foreignKeys = [ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habit_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("date")],
)
data class HabitCheckInEntity(
    @ColumnInfo(name = "habit_id") val habitId: Long,
    val date: String,
)

data class HabitWithCheckIns(
    @Embedded val habit: HabitEntity,
    @Relation(parentColumn = "id", entityColumn = "habit_id")
    val checkIns: List<HabitCheckInEntity>,
)
