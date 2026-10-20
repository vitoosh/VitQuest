package io.github.vitoosh.vitquest.core.domain.nutrition

import io.github.vitoosh.vitquest.core.model.ActivityLevel
import io.github.vitoosh.vitquest.core.model.BiologicalSex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnergyCalculatorTest {

    private fun inputs(
        age: Int? = 30,
        height: Double? = 180.0,
        weight: Double? = 80.0,
        sex: BiologicalSex? = BiologicalSex.MALE,
        activity: ActivityLevel? = ActivityLevel.MODERATE,
        target: Double? = 75.0,
    ) = EnergyInputs(age, height, weight, sex, activity, target)

    @Test
    fun bmrFollowsMifflinStJeor() {
        // 10 × 80 + 6.25 × 180 − 5 × 30 + 5 = 1,780
        assertEquals(1_780.0, EnergyCalculator.bmr(80.0, 180.0, 30, BiologicalSex.MALE), 0.001)
        // 10 × 60 + 6.25 × 165 − 5 × 25 − 161 = 1,345.25
        assertEquals(1_345.25, EnergyCalculator.bmr(60.0, 165.0, 25, BiologicalSex.FEMALE), 0.001)
    }

    @Test
    fun weightLossUsesAModerateDeficit() {
        val estimate = EnergyCalculator.estimate(inputs()) as EnergyEstimate.Available

        assertEquals(1_780, estimate.bmrKcal)
        assertEquals(2_760, estimate.tdeeKcal) // 1,780 × 1.55 = 2,759
        assertEquals(2_260, estimate.recommendedKcal) // 500 kcal below TDEE
        assertEquals(WeightDirection.LOSE, estimate.direction)
        assertFalse(estimate.limitedByFloor)
    }

    @Test
    fun deficitIsCappedAtTwentyPercentOfTdee() {
        val estimate = EnergyCalculator.estimate(
            inputs(age = 30, height = 175.0, weight = 70.0, activity = ActivityLevel.SEDENTARY, target = 65.0),
        ) as EnergyEstimate.Available
        // BMR 1,648.75; TDEE 1,978.5; 20% (395.7 kcal) is less than 500 -> 1,582.8.
        assertEquals(1_580, estimate.recommendedKcal)
        assertFalse(estimate.limitedByFloor)
    }

    @Test
    fun recommendationIsRaisedToTheSafetyFloor() {
        val estimate = EnergyCalculator.estimate(
            inputs(age = 60, height = 155.0, weight = 55.0, sex = BiologicalSex.FEMALE, activity = ActivityLevel.SEDENTARY, target = 50.0),
        ) as EnergyEstimate.Available
        // BMR 1,057.75; TDEE 1,269.3; minus 20% = 1,015 -> raised to 1,200.
        assertEquals(1_200, estimate.recommendedKcal)
        assertTrue(estimate.limitedByFloor)
    }

    @Test
    fun neverRecommendsEatingBelowMaintenanceWhenMaintenanceIsUnderTheFloor() {
        val estimate = EnergyCalculator.estimate(
            inputs(age = 70, height = 150.0, weight = 45.0, sex = BiologicalSex.FEMALE, activity = ActivityLevel.SEDENTARY, target = 40.0),
        ) as EnergyEstimate.Available
        assertEquals(estimate.tdeeKcal, estimate.recommendedKcal)
        assertTrue(estimate.limitedByFloor)
    }

    @Test
    fun maintainAndGainDirections() {
        val maintain = EnergyCalculator.estimate(inputs(target = 80.3)) as EnergyEstimate.Available
        assertEquals(WeightDirection.MAINTAIN, maintain.direction)
        assertEquals(maintain.tdeeKcal, maintain.recommendedKcal)

        val gain = EnergyCalculator.estimate(inputs(target = 85.0)) as EnergyEstimate.Available
        assertEquals(WeightDirection.GAIN, gain.direction)
        assertEquals(3_010, gain.recommendedKcal)

        val noTarget = EnergyCalculator.estimate(inputs(target = null)) as EnergyEstimate.Available
        assertEquals(WeightDirection.MAINTAIN, noTarget.direction)
    }

    @Test
    fun missingInputsAreListed() {
        val estimate = EnergyCalculator.estimate(inputs(age = null, sex = null)) as EnergyEstimate.Missing
        assertEquals(setOf(MissingEnergyInput.AGE, MissingEnergyInput.SEX), estimate.inputs)
    }

    @Test
    fun missingActivityUsesTheLowestFactorAndSaysSo() {
        val estimate = EnergyCalculator.estimate(inputs(activity = null)) as EnergyEstimate.Available
        assertEquals(2_140, estimate.tdeeKcal) // 1,780 × 1.2 = 2,136
        assertTrue(estimate.activityAssumed)
    }

    @Test
    fun walkingEstimateNeedsWeight() {
        assertEquals(400, EnergyCalculator.walkingKcal(10_000, 80.0))
        assertNull(EnergyCalculator.walkingKcal(10_000, null))
    }

    @Test
    fun manualTargetWinsOverRecommendation() {
        val estimate = EnergyCalculator.estimate(inputs())
        assertEquals(CalorieTarget.Manual(1_900), CalorieTarget.resolve(1_900, estimate))
        assertEquals(CalorieTarget.Recommended(2_260), CalorieTarget.resolve(null, estimate))
        assertNull(CalorieTarget.resolve(null, EnergyEstimate.Missing(setOf(MissingEnergyInput.AGE))))
    }

    @Test
    fun mealSuggestionFollowsTimeOfDay() {
        assertEquals(io.github.vitoosh.vitquest.core.model.MealType.BREAKFAST, mealForHour(7))
        assertEquals(io.github.vitoosh.vitquest.core.model.MealType.LUNCH, mealForHour(12))
        assertEquals(io.github.vitoosh.vitquest.core.model.MealType.DINNER, mealForHour(19))
        assertEquals(io.github.vitoosh.vitquest.core.model.MealType.SNACK, mealForHour(15))
        assertEquals(io.github.vitoosh.vitquest.core.model.MealType.SNACK, mealForHour(23))
    }
}
