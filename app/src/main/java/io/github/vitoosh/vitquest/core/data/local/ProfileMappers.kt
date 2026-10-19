package io.github.vitoosh.vitquest.core.data.local

import io.github.vitoosh.vitquest.core.model.ActivityLevel
import io.github.vitoosh.vitquest.core.model.DietaryPreference
import io.github.vitoosh.vitquest.core.model.Equipment
import io.github.vitoosh.vitquest.core.model.FitnessExperience
import io.github.vitoosh.vitquest.core.model.Goals
import io.github.vitoosh.vitquest.core.model.UserProfile
import io.github.vitoosh.vitquest.core.model.WorkoutLocation

private const val SET_SEPARATOR = ","

fun UserProfileEntity.toModel(): UserProfile = UserProfile(
    age = age,
    heightCm = heightCm,
    currentWeightKg = currentWeightKg,
    fitnessExperience = fitnessExperience.toEnumOrNull<FitnessExperience>(),
    activityLevel = activityLevel.toEnumOrNull<ActivityLevel>(),
    workoutLocation = workoutLocation.toEnumOrNull<WorkoutLocation>(),
    equipment = equipment.toEnumSet<Equipment>(),
    dietaryPreferences = dietaryPreferences.toEnumSet<DietaryPreference>(),
)

fun UserProfile.toEntity(updatedAtEpochMs: Long): UserProfileEntity = UserProfileEntity(
    age = age,
    heightCm = heightCm,
    currentWeightKg = currentWeightKg,
    fitnessExperience = fitnessExperience?.name,
    activityLevel = activityLevel?.name,
    workoutLocation = workoutLocation?.name,
    equipment = equipment.toStoredSet(),
    dietaryPreferences = dietaryPreferences.toStoredSet(),
    updatedAtEpochMs = updatedAtEpochMs,
)

fun GoalsEntity.toModel(): Goals = Goals(
    targetWeightKg = targetWeightKg,
    dailyStepGoal = dailyStepGoal,
    dailyWaterTargetMl = dailyWaterTargetMl,
    waterServingMl = waterServingMl,
)

fun Goals.toEntity(updatedAtEpochMs: Long): GoalsEntity = GoalsEntity(
    targetWeightKg = targetWeightKg,
    dailyStepGoal = dailyStepGoal,
    dailyWaterTargetMl = dailyWaterTargetMl,
    waterServingMl = waterServingMl,
    updatedAtEpochMs = updatedAtEpochMs,
)

// Unknown names (e.g. written by a newer app version) are ignored instead of crashing.
private inline fun <reified E : Enum<E>> String?.toEnumOrNull(): E? =
    this?.let { name -> enumValues<E>().firstOrNull { it.name == name } }

private inline fun <reified E : Enum<E>> String.toEnumSet(): Set<E> =
    split(SET_SEPARATOR).mapNotNull { it.trim().toEnumOrNull<E>() }.toSet()

private fun Set<Enum<*>>.toStoredSet(): String =
    map { it.name }.sorted().joinToString(SET_SEPARATOR)
