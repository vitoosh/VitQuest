package io.github.vitoosh.vitquest.core.domain

/** Accepted ranges for user-entered values. Values outside are rejected, not clamped. */
object InputLimits {
    val AGE_YEARS = 13..100
    val HEIGHT_CM = 100.0..250.0
    val WEIGHT_KG = 30.0..300.0
    val DAILY_STEPS = 1_000..50_000
    val DAILY_WATER_ML = 500..6_000
    val WATER_SERVING_ML = 50..1_000
    val MANUAL_DAILY_STEPS = 0..100_000
    val DAILY_CALORIE_TARGET = 1_000..5_000
    val KCAL_PER_100G = 0.0..900.0
    val MACRO_PER_100G = 0.0..100.0
    val FOOD_GRAMS = 1.0..3_000.0
    val SERVING_QUANTITY = 0.25..20.0
    val WATER_LOG_ML = 10..3_000
    val BODY_MEASUREMENT_CM = 20.0..250.0
    val WORKOUT_MINUTES = 1..300
    val HABIT_NAME_LENGTH = 1..40
}
