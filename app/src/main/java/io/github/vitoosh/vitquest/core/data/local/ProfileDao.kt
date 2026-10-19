package io.github.vitoosh.vitquest.core.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ProfileDao {

    @Query("SELECT * FROM user_profile WHERE id = $SINGLE_ROW_ID")
    abstract fun observeProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM goals WHERE id = $SINGLE_ROW_ID")
    abstract fun observeGoals(): Flow<GoalsEntity?>

    @Query("UPDATE goals SET daily_step_goal = :steps, updated_at_epoch_ms = :updatedAtEpochMs WHERE id = $SINGLE_ROW_ID")
    abstract suspend fun updateDailyStepGoal(steps: Int, updatedAtEpochMs: Long)

    @Upsert
    abstract suspend fun upsertProfile(profile: UserProfileEntity)

    @Upsert
    abstract suspend fun upsertGoals(goals: GoalsEntity)

    /** Profile and goals are saved together so the screens never see half an update. */
    @Transaction
    open suspend fun upsertProfileAndGoals(profile: UserProfileEntity, goals: GoalsEntity) {
        upsertProfile(profile)
        upsertGoals(goals)
    }
}
