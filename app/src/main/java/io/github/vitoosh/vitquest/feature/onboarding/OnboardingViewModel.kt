package io.github.vitoosh.vitquest.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.core.data.repository.ProfileRepository
import io.github.vitoosh.vitquest.core.data.repository.UserPreferencesRepository
import io.github.vitoosh.vitquest.feature.profile.ProfileFormErrors
import io.github.vitoosh.vitquest.feature.profile.ProfileFormResult
import io.github.vitoosh.vitquest.feature.profile.ProfileFormState
import io.github.vitoosh.vitquest.feature.profile.ProfileFormValidator
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep { WELCOME, BODY, EXPERIENCE, PREFERENCES, GOALS }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val form: ProfileFormState = ProfileFormState(),
    val errors: ProfileFormErrors = ProfileFormErrors(),
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
) {
    val stepNumber: Int get() = step.ordinal
    val totalQuestionSteps: Int get() = OnboardingStep.entries.size - 1
    val isLastStep: Boolean get() = step == OnboardingStep.GOALS
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun updateForm(transform: (ProfileFormState) -> ProfileFormState) {
        _uiState.update { state ->
            val form = transform(state.form)
            // Re-validate live only once errors are visible, so typing is not interrupted.
            val errors = if (state.errors.isEmpty) state.errors else currentErrors(form)
            state.copy(form = form, errors = errors, saveFailed = false)
        }
    }

    fun next() {
        val state = _uiState.value
        when (state.step) {
            OnboardingStep.BODY -> {
                val errors = ProfileFormValidator.validateBodyFields(state.form)
                if (errors.hasBodyErrors) {
                    _uiState.update { it.copy(errors = errors) }
                    return
                }
                moveTo(OnboardingStep.EXPERIENCE)
            }
            OnboardingStep.GOALS -> finish()
            else -> moveTo(OnboardingStep.entries[state.step.ordinal + 1])
        }
    }

    /** Skipping the body step discards its answers; other steps keep any selections made. */
    fun skip() {
        val state = _uiState.value
        if (state.step == OnboardingStep.BODY) {
            _uiState.update {
                it.copy(
                    form = it.form.copy(age = "", heightCm = "", currentWeightKg = "", targetWeightKg = ""),
                    errors = ProfileFormErrors(),
                )
            }
        }
        if (!state.isLastStep) moveTo(OnboardingStep.entries[state.step.ordinal + 1])
    }

    fun back() {
        val state = _uiState.value
        if (state.step != OnboardingStep.WELCOME) {
            _uiState.update { it.copy(step = OnboardingStep.entries[state.step.ordinal - 1]) }
        }
    }

    private fun moveTo(step: OnboardingStep) {
        _uiState.update { state ->
            val form = if (step == OnboardingStep.GOALS) ProfileFormValidator.applySuggestions(state.form) else state.form
            state.copy(step = step, form = form, errors = ProfileFormErrors())
        }
    }

    private fun finish() {
        val state = _uiState.value
        if (state.isSaving) return
        when (val result = ProfileFormValidator.validate(state.form)) {
            is ProfileFormResult.Invalid -> _uiState.update {
                it.copy(
                    errors = result.errors,
                    step = if (result.errors.hasBodyErrors) OnboardingStep.BODY else it.step,
                )
            }
            is ProfileFormResult.Valid -> {
                _uiState.update { it.copy(isSaving = true, saveFailed = false) }
                viewModelScope.launch {
                    try {
                        profileRepository.saveProfileAndGoals(result.profile, result.goals)
                        // The app switches to the main screens when this flag is observed.
                        userPreferencesRepository.setOnboardingCompleted(true)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                        _uiState.update { it.copy(isSaving = false, saveFailed = true) }
                    }
                }
            }
        }
    }

    private fun currentErrors(form: ProfileFormState): ProfileFormErrors =
        when (val result = ProfileFormValidator.validate(form)) {
            is ProfileFormResult.Invalid -> result.errors
            is ProfileFormResult.Valid -> ProfileFormErrors()
        }
}
