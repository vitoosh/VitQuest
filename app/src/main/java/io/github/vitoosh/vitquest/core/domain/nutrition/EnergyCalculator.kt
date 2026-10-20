package io.github.vitoosh.vitquest.core.domain.nutrition

import io.github.vitoosh.vitquest.core.model.ActivityLevel
import io.github.vitoosh.vitquest.core.model.BiologicalSex
import kotlin.math.min
import kotlin.math.roundToInt

enum class MissingEnergyInput { AGE, HEIGHT, WEIGHT, SEX }

enum class WeightDirection { LOSE, MAINTAIN, GAIN }

data class EnergyInputs(
    val age: Int?,
    val heightCm: Double?,
    val weightKg: Double?,
    val sex: BiologicalSex?,
    val activityLevel: ActivityLevel?,
    val targetWeightKg: Double?,
)

sealed interface EnergyEstimate {
    data class Missing(val inputs: Set<MissingEnergyInput>) : EnergyEstimate

    data class Available(
        /** Basal metabolic rate: energy used at complete rest. */
        val bmrKcal: Int,
        /** Total daily energy expenditure: BMR times an activity factor. */
        val tdeeKcal: Int,
        /** Suggested daily intake for the user's direction, after safety floors. */
        val recommendedKcal: Int,
        val direction: WeightDirection,
        /** True when activity level was not answered and the lowest factor was used. */
        val activityAssumed: Boolean,
        /** True when the safety floor limited the deficit. */
        val limitedByFloor: Boolean,
    ) : EnergyEstimate
}

/**
 * Energy estimates using the Mifflin-St Jeor equation (Mifflin et al., 1990)
 * and standard activity multipliers. All results are estimates, rounded to 10 kcal.
 */
object EnergyCalculator {

    /** At most 500 kcal or 20% of TDEE below maintenance, roughly 0.25–0.5 kg per week. */
    const val MAX_DEFICIT_KCAL = 500
    const val MAX_DEFICIT_FRACTION = 0.20
    const val GAIN_SURPLUS_KCAL = 250

    /** Commonly cited minimum intakes without medical supervision. */
    const val MIN_INTAKE_FEMALE = 1_200
    const val MIN_INTAKE_MALE = 1_500

    /** Below this difference the goal is treated as maintaining weight. */
    private const val MAINTAIN_TOLERANCE_KG = 0.5

    fun activityFactor(level: ActivityLevel): Double = when (level) {
        ActivityLevel.SEDENTARY -> 1.2
        ActivityLevel.LIGHT -> 1.375
        ActivityLevel.MODERATE -> 1.55
        ActivityLevel.ACTIVE -> 1.725
    }

    fun bmr(weightKg: Double, heightCm: Double, age: Int, sex: BiologicalSex): Double {
        val base = 10 * weightKg + 6.25 * heightCm - 5 * age
        return if (sex == BiologicalSex.MALE) base + 5 else base - 161
    }

    fun estimate(inputs: EnergyInputs): EnergyEstimate {
        val missing = buildSet {
            if (inputs.age == null) add(MissingEnergyInput.AGE)
            if (inputs.heightCm == null) add(MissingEnergyInput.HEIGHT)
            if (inputs.weightKg == null) add(MissingEnergyInput.WEIGHT)
            if (inputs.sex == null) add(MissingEnergyInput.SEX)
        }
        if (missing.isNotEmpty()) return EnergyEstimate.Missing(missing)

        val weight = requireNotNull(inputs.weightKg)
        val sex = requireNotNull(inputs.sex)
        val bmr = bmr(weight, requireNotNull(inputs.heightCm), requireNotNull(inputs.age), sex)
        val level = inputs.activityLevel ?: ActivityLevel.SEDENTARY
        val tdee = bmr * activityFactor(level)

        val direction = direction(weight, inputs.targetWeightKg)
        val floor = if (sex == BiologicalSex.FEMALE) MIN_INTAKE_FEMALE else MIN_INTAKE_MALE
        var limited = false
        val recommended = when (direction) {
            WeightDirection.MAINTAIN -> tdee
            WeightDirection.GAIN -> tdee + GAIN_SURPLUS_KCAL
            WeightDirection.LOSE -> {
                val deficit = min(MAX_DEFICIT_KCAL.toDouble(), tdee * MAX_DEFICIT_FRACTION)
                val target = tdee - deficit
                when {
                    tdee <= floor -> {
                        limited = true
                        tdee
                    }
                    target < floor -> {
                        limited = true
                        floor.toDouble()
                    }
                    else -> target
                }
            }
        }
        return EnergyEstimate.Available(
            bmrKcal = roundTo10(bmr),
            tdeeKcal = roundTo10(tdee),
            recommendedKcal = roundTo10(recommended),
            direction = direction,
            activityAssumed = inputs.activityLevel == null,
            limitedByFloor = limited,
        )
    }

    fun direction(currentKg: Double, targetKg: Double?): WeightDirection = when {
        targetKg == null -> WeightDirection.MAINTAIN
        targetKg < currentKg - MAINTAIN_TOLERANCE_KG -> WeightDirection.LOSE
        targetKg > currentKg + MAINTAIN_TOLERANCE_KG -> WeightDirection.GAIN
        else -> WeightDirection.MAINTAIN
    }

    /**
     * Rough energy used by walking: about 0.0005 kcal per step per kg of body
     * weight (≈0.04 kcal per step at 80 kg). Shown for information only; it is
     * not added to the intake target because both inputs are estimates.
     */
    fun walkingKcal(steps: Int, weightKg: Double?): Int? =
        weightKg?.let { (steps * it * WALKING_KCAL_PER_STEP_PER_KG).roundToInt() }

    private const val WALKING_KCAL_PER_STEP_PER_KG = 0.0005

    private fun roundTo10(value: Double): Int = (value / 10).roundToInt() * 10
}

/** The calorie target in use: the user's manual value, otherwise the recommendation. */
sealed interface CalorieTarget {
    val kcal: Int

    data class Manual(override val kcal: Int) : CalorieTarget
    data class Recommended(override val kcal: Int) : CalorieTarget

    companion object {
        fun resolve(manual: Int?, estimate: EnergyEstimate): CalorieTarget? = when {
            manual != null -> Manual(manual)
            estimate is EnergyEstimate.Available -> Recommended(estimate.recommendedKcal)
            else -> null
        }
    }
}
