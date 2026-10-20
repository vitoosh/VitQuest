package io.github.vitoosh.vitquest.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WorkoutDao {

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY date DESC, started_at_epoch_ms DESC")
    abstract fun observeSessions(): Flow<List<WorkoutSessionWithExercises>>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE date BETWEEN :from AND :to ORDER BY date DESC, started_at_epoch_ms DESC")
    abstract fun observeSessions(from: String, to: String): Flow<List<WorkoutSessionWithExercises>>

    @Insert
    abstract suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Insert
    abstract suspend fun insertExercises(exercises: List<WorkoutSessionExerciseEntity>)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    abstract suspend fun deleteSession(id: Long)

    @Transaction
    open suspend fun insertSessionWithExercises(session: WorkoutSessionEntity, exercises: List<WorkoutSessionExerciseEntity>): Long {
        val id = insertSession(session)
        insertExercises(exercises.mapIndexed { index, item -> item.copy(id = 0, sessionId = id, position = index) })
        return id
    }
}

@Dao
abstract class HabitDao {

    @Transaction
    @Query("SELECT * FROM habits ORDER BY created_at_epoch_ms")
    abstract fun observeHabits(): Flow<List<HabitWithCheckIns>>

    @Insert
    abstract suspend fun insertHabit(habit: HabitEntity): Long

    @Query("UPDATE habits SET name = :name, target_days_per_week = :targetDays WHERE id = :id")
    abstract suspend fun updateHabit(id: Long, name: String, targetDays: Int)

    @Query("DELETE FROM habits WHERE id = :id")
    abstract suspend fun deleteHabit(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertCheckIn(checkIn: HabitCheckInEntity)

    @Query("DELETE FROM habit_check_ins WHERE habit_id = :habitId AND date = :date")
    abstract suspend fun deleteCheckIn(habitId: Long, date: String)
}
