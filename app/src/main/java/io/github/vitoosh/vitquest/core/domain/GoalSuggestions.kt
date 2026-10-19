package io.github.vitoosh.vitquest.core.domain

import io.github.vitoosh.vitquest.core.model.ActivityLevel
import kotlin.math.roundToInt

/**
 * Conservative starting points for daily goals. These are adjustable
 * estimates, not medical recommendations; the UI labels them as suggestions.
 */
object GoalSuggestions {

    const val DEFAULT_STEP_GOAL = 7_000
    const val DEFAULT_WATER_TARGET_ML = 2_000
    const val DEFAULT_WATER_SERVING_ML = 250

    private const val WATER_ML_PER_KG = 30
    private const val WATER_ROUNDING_ML = 50
    private const val MIN_SUGGESTED_WATER_ML = 1_500
    private const val MAX_SUGGESTED_WATER_ML = 3_500

    /**
     * Starts close to the user's current activity so the goal is reachable,
     * leaving room to increase it gradually.
     */
    fun dailySteps(activityLevel: ActivityLevel?): Int = when (activityLevel) {
        ActivityLevel.SEDENTARY -> 5_000
        ActivityLevel.LIGHT -> 7_000
        ActivityLevel.MODERATE -> 8_000
        ActivityLevel.ACTIVE -> 10_000
        null -> DEFAULT_STEP_GOAL
    }

    /** About 30 ml per kg of body weight, rounded and kept within a moderate range. */
    fun dailyWaterMl(weightKg: Double?): Int {
        if (weightKg == null || weightKg <= 0.0) return DEFAULT_WATER_TARGET_ML
        val raw = weightKg * WATER_ML_PER_KG
        val rounded = (raw / WATER_ROUNDING_ML).roundToInt() * WATER_ROUNDING_ML
        return rounded.coerceIn(MIN_SUGGESTED_WATER_ML, MAX_SUGGESTED_WATER_ML)
    }
}
