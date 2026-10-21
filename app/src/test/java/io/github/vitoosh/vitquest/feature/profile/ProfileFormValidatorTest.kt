package io.github.vitoosh.vitquest.feature.profile

import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.model.ActivityLevel
import io.github.vitoosh.vitquest.core.model.Equipment
import io.github.vitoosh.vitquest.core.model.Goals
import io.github.vitoosh.vitquest.core.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileFormValidatorTest {

    @Test
    fun skippedOptionalQuestionsProduceEmptyProfileWithDefaultGoals() {
        val result = ProfileFormValidator.validate(ProfileFormState()) as ProfileFormResult.Valid

        assertEquals(UserProfile(), result.profile)
        assertNull(result.goals.targetWeightKg)
        assertEquals(7_000, result.goals.dailyStepGoal)
        assertEquals(2_000, result.goals.dailyWaterTargetMl)
        assertEquals(250, result.goals.waterServingMl)
    }

    @Test
    fun filledFormIsParsed() {
        val form = ProfileFormState(
            age = "34",
            heightCm = "170,5",
            currentWeightKg = "82.4",
            targetWeightKg = "75",
            activityLevel = ActivityLevel.LIGHT,
            equipment = setOf(Equipment.EXERCISE_MAT),
            dailyStepGoal = "8.000",
            dailyWaterTargetMl = "2500",
            waterServingMl = "330",
        )

        val result = ProfileFormValidator.validate(form) as ProfileFormResult.Valid

        assertEquals(34, result.profile.age)
        assertEquals(170.5, result.profile.heightCm!!, 0.0)
        assertEquals(82.4, result.profile.currentWeightKg!!, 0.0)
        assertEquals(setOf(Equipment.EXERCISE_MAT), result.profile.equipment)
        assertEquals(Goals(75.0, 8_000, 2_500, 330), result.goals)
    }

    @Test
    fun invalidFieldsAreReportedIndividually() {
        val form = ProfileFormState(age = "5", heightCm = "abc", dailyStepGoal = "")

        val errors = (ProfileFormValidator.validate(form) as ProfileFormResult.Invalid).errors

        assertEquals(FieldError.OUT_OF_RANGE, errors.age)
        assertEquals(FieldError.INVALID_NUMBER, errors.heightCm)
        assertEquals(FieldError.REQUIRED, errors.dailyStepGoal)
        assertNull(errors.currentWeightKg)
        assertTrue(errors.hasBodyErrors)
        assertTrue(errors.hasGoalErrors)
    }

    @Test
    fun suggestionsFollowAnswersUntilUserEditsGoals() {
        val form = ProfileFormState(currentWeightKg = "80", activityLevel = ActivityLevel.ACTIVE)

        val suggested = ProfileFormValidator.applySuggestions(form)
        assertEquals("10000", suggested.dailyStepGoal)
        assertEquals("2400", suggested.dailyWaterTargetMl)

        val edited = suggested.copy(dailyStepGoal = "6000", stepGoalEdited = true)
        val again = ProfileFormValidator.applySuggestions(edited.copy(activityLevel = ActivityLevel.SEDENTARY))
        assertEquals("6000", again.dailyStepGoal)
    }

    @Test
    fun formRoundTripsThroughSavedModels() {
        val profile = UserProfile(age = 40, heightCm = 165.0, currentWeightKg = 70.5, activityLevel = ActivityLevel.MODERATE)
        val goals = Goals(targetWeightKg = 65.0, dailyStepGoal = 9_000, dailyWaterTargetMl = 2_200, waterServingMl = 200)

        val form = ProfileFormState.from(profile, goals)
        val result = ProfileFormValidator.validate(form) as ProfileFormResult.Valid

        assertEquals("165", form.heightCm)
        assertEquals("70.5", form.currentWeightKg)
        assertEquals(profile, result.profile)
        assertEquals(goals, result.goals)
        assertTrue(form.stepGoalEdited)
    }
}
