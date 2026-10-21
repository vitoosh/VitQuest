package io.github.vitoosh.vitquest.core.model

/** Daily targets chosen (or accepted from suggestions) by the user. */
data class Goals(
    val targetWeightKg: Double? = null,
    val dailyStepGoal: Int,
    val dailyWaterTargetMl: Int,
    val waterServingMl: Int,
    /** Manual daily calorie target; null means use the calculated recommendation. */
    val dailyCalorieTarget: Int? = null,
)
