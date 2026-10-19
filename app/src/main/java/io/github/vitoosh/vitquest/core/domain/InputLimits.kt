package io.github.vitoosh.vitquest.core.domain

/** Accepted ranges for user-entered values. Values outside are rejected, not clamped. */
object InputLimits {
    val AGE_YEARS = 13..100
    val HEIGHT_CM = 100.0..250.0
    val WEIGHT_KG = 30.0..300.0
    val DAILY_STEPS = 1_000..50_000
    val DAILY_WATER_ML = 500..6_000
    val WATER_SERVING_ML = 50..1_000
}
