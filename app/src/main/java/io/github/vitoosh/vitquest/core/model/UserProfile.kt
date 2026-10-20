package io.github.vitoosh.vitquest.core.model

/**
 * Personal information entered by the user. Every field is optional because
 * onboarding lets users skip questions; features must handle missing values
 * instead of assuming defaults for the person.
 */
data class UserProfile(
    val age: Int? = null,
    val sex: BiologicalSex? = null,
    val heightCm: Double? = null,
    val currentWeightKg: Double? = null,
    val fitnessExperience: FitnessExperience? = null,
    val activityLevel: ActivityLevel? = null,
    val workoutLocation: WorkoutLocation? = null,
    val equipment: Set<Equipment> = emptySet(),
    val dietaryPreferences: Set<DietaryPreference> = emptySet(),
) {
    /** True when the body measurements used for later estimates are present. */
    val hasBodyMetrics: Boolean
        get() = age != null && heightCm != null && currentWeightKg != null
}

enum class FitnessExperience { BEGINNER, INTERMEDIATE, ADVANCED }

enum class ActivityLevel { SEDENTARY, LIGHT, MODERATE, ACTIVE }

enum class WorkoutLocation { HOME, OFFICE, OUTDOOR, MIXED }

enum class Equipment { RESISTANCE_BAND, DUMBBELLS, PULL_UP_BAR, JUMP_ROPE, EXERCISE_MAT, CHAIR_OR_BENCH }

enum class DietaryPreference { HALAL, VEGETARIAN, VEGAN, LACTOSE_FREE, GLUTEN_FREE, LOW_SUGAR }
