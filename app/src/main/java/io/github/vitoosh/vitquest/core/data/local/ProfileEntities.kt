package io.github.vitoosh.vitquest.core.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** VitQuest is single-user; profile and goals each occupy one fixed row. */
const val SINGLE_ROW_ID = 1

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val age: Int?,
    @ColumnInfo(name = "height_cm") val heightCm: Double?,
    @ColumnInfo(name = "current_weight_kg") val currentWeightKg: Double?,
    @ColumnInfo(name = "fitness_experience") val fitnessExperience: String?,
    @ColumnInfo(name = "activity_level") val activityLevel: String?,
    @ColumnInfo(name = "workout_location") val workoutLocation: String?,
    /** Comma-separated enum names. */
    val equipment: String,
    /** Comma-separated enum names. */
    @ColumnInfo(name = "dietary_preferences") val dietaryPreferences: String,
    @ColumnInfo(name = "updated_at_epoch_ms") val updatedAtEpochMs: Long,
)

@Entity(tableName = "goals")
data class GoalsEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    @ColumnInfo(name = "target_weight_kg") val targetWeightKg: Double?,
    @ColumnInfo(name = "daily_step_goal") val dailyStepGoal: Int,
    @ColumnInfo(name = "daily_water_target_ml") val dailyWaterTargetMl: Int,
    @ColumnInfo(name = "water_serving_ml") val waterServingMl: Int,
    @ColumnInfo(name = "updated_at_epoch_ms") val updatedAtEpochMs: Long,
)
