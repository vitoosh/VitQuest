package io.github.vitoosh.vitquest.core.domain

import io.github.vitoosh.vitquest.core.model.ActivityLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalSuggestionsTest {

    @Test
    fun stepGoalIncreasesWithActivityLevel() {
        val goals = ActivityLevel.entries.map(GoalSuggestions::dailySteps)
        assertEquals(goals.sorted(), goals)
        assertEquals(5_000, GoalSuggestions.dailySteps(ActivityLevel.SEDENTARY))
        assertEquals(10_000, GoalSuggestions.dailySteps(ActivityLevel.ACTIVE))
    }

    @Test
    fun stepGoalDefaultsWhenActivityUnknown() {
        assertEquals(GoalSuggestions.DEFAULT_STEP_GOAL, GoalSuggestions.dailySteps(null))
    }

    @Test
    fun waterTargetIsThirtyMlPerKgRoundedToFifty() {
        assertEquals(2_100, GoalSuggestions.dailyWaterMl(70.0))
        // 68.3 kg * 30 = 2049 ml -> rounds to 2050.
        assertEquals(2_050, GoalSuggestions.dailyWaterMl(68.3))
    }

    @Test
    fun waterTargetStaysWithinModerateRange() {
        assertEquals(1_500, GoalSuggestions.dailyWaterMl(35.0))
        assertEquals(3_500, GoalSuggestions.dailyWaterMl(200.0))
    }

    @Test
    fun waterTargetDefaultsWithoutWeight() {
        assertEquals(GoalSuggestions.DEFAULT_WATER_TARGET_ML, GoalSuggestions.dailyWaterMl(null))
        assertEquals(GoalSuggestions.DEFAULT_WATER_TARGET_ML, GoalSuggestions.dailyWaterMl(0.0))
    }

    @Test
    fun defaultsAreWithinAcceptedInputLimits() {
        assertTrue(GoalSuggestions.DEFAULT_STEP_GOAL in InputLimits.DAILY_STEPS)
        assertTrue(GoalSuggestions.DEFAULT_WATER_TARGET_ML in InputLimits.DAILY_WATER_ML)
        assertTrue(GoalSuggestions.DEFAULT_WATER_SERVING_ML in InputLimits.WATER_SERVING_ML)
    }
}
