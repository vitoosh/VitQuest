package io.github.vitoosh.vitquest.feature.onboarding

import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.model.ActivityLevel
import io.github.vitoosh.vitquest.testing.FakeBodyRepository
import io.github.vitoosh.vitquest.testing.FakeProfileRepository
import io.github.vitoosh.vitquest.testing.FixedTimeSource
import java.time.Instant
import java.time.LocalDate
import io.github.vitoosh.vitquest.testing.FakeUserPreferencesRepository
import io.github.vitoosh.vitquest.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val profileRepository = FakeProfileRepository()
    private val preferencesRepository = FakeUserPreferencesRepository()
    private val bodyRepository = FakeBodyRepository(profileRepository)
    private val time = FixedTimeSource(Instant.parse("2026-10-19T03:00:00Z"))
    private val viewModel = OnboardingViewModel(profileRepository, preferencesRepository, bodyRepository, time)

    private fun goToGoalsStep() {
        repeat(4) { viewModel.next() }
    }

    @Test
    fun invalidBodyValuesBlockTheStep() {
        viewModel.next() // Welcome -> Body
        viewModel.updateForm { it.copy(currentWeightKg = "abc") }

        viewModel.next()

        val state = viewModel.uiState.value
        assertEquals(OnboardingStep.BODY, state.step)
        assertEquals(FieldError.INVALID_NUMBER, state.errors.currentWeightKg)
    }

    @Test
    fun skippingBodyStepDiscardsItsAnswers() {
        viewModel.next()
        viewModel.updateForm { it.copy(age = "200") }

        viewModel.skip()

        val state = viewModel.uiState.value
        assertEquals(OnboardingStep.EXPERIENCE, state.step)
        assertEquals("", state.form.age)
    }

    @Test
    fun goalsAreSuggestedFromAnswers() {
        viewModel.next()
        viewModel.updateForm { it.copy(currentWeightKg = "60", activityLevel = ActivityLevel.SEDENTARY) }

        repeat(3) { viewModel.next() }

        val state = viewModel.uiState.value
        assertEquals(OnboardingStep.GOALS, state.step)
        assertEquals("5000", state.form.dailyStepGoal)
        assertEquals("1800", state.form.dailyWaterTargetMl)
    }

    @Test
    fun finishingSavesProfileAndCompletesOnboarding() {
        viewModel.next()
        viewModel.updateForm { it.copy(age = "28", currentWeightKg = "70") }
        repeat(3) { viewModel.next() }

        viewModel.next() // Finish

        assertEquals(28, profileRepository.profileState.value?.age)
        assertEquals(2_100, profileRepository.goalsState.value?.dailyWaterTargetMl)
        assertTrue(preferencesRepository.state.value.onboardingCompleted)
        // The onboarding weight starts the weight history.
        assertEquals(listOf(70.0), bodyRepository.weightsState.value.map { it.weightKg })
        assertEquals(LocalDate.parse("2026-10-19"), bodyRepository.weightsState.value.single().date)
    }

    @Test
    fun finishingWithEverythingSkippedStillWorks() {
        goToGoalsStep()

        viewModel.next()

        assertNull(profileRepository.profileState.value?.age)
        assertTrue(preferencesRepository.state.value.onboardingCompleted)
        assertTrue(bodyRepository.weightsState.value.isEmpty())
    }

    @Test
    fun saveFailureIsReportedAndOnboardingStaysOpen() {
        profileRepository.failSaves = true
        goToGoalsStep()

        viewModel.next()

        val state = viewModel.uiState.value
        assertTrue(state.saveFailed)
        assertFalse(state.isSaving)
        assertFalse(preferencesRepository.state.value.onboardingCompleted)
    }

    @Test
    fun invalidGoalKeepsUserOnGoalsStep() {
        goToGoalsStep()
        viewModel.updateForm { it.copy(dailyStepGoal = "100", stepGoalEdited = true) }

        viewModel.next()

        val state = viewModel.uiState.value
        assertEquals(OnboardingStep.GOALS, state.step)
        assertEquals(FieldError.OUT_OF_RANGE, state.errors.dailyStepGoal)
        assertFalse(preferencesRepository.state.value.onboardingCompleted)
    }
}
