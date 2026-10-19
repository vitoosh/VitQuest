package io.github.vitoosh.vitquest.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.core.data.repository.ProfileRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val form: ProfileFormState = ProfileFormState(),
    val errors: ProfileFormErrors = ProfileFormErrors(),
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val saved: Boolean = false,
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Load once: the form must not be overwritten by later emissions while the user edits.
            val form = ProfileFormState.from(profileRepository.profile.first(), profileRepository.goals.first())
            _uiState.update { it.copy(isLoading = false, form = form) }
        }
    }

    fun updateForm(transform: (ProfileFormState) -> ProfileFormState) {
        _uiState.update { state ->
            val form = transform(state.form)
            val errors = if (state.errors.isEmpty) state.errors else form.errors()
            state.copy(form = form, errors = errors, saveFailed = false)
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isLoading || state.isSaving) return
        when (val result = ProfileFormValidator.validate(state.form)) {
            is ProfileFormResult.Invalid -> _uiState.update { it.copy(errors = result.errors) }
            is ProfileFormResult.Valid -> {
                _uiState.update { it.copy(isSaving = true, saveFailed = false, errors = ProfileFormErrors()) }
                viewModelScope.launch {
                    try {
                        profileRepository.saveProfileAndGoals(result.profile, result.goals)
                        _uiState.update { it.copy(isSaving = false, saved = true) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                        _uiState.update { it.copy(isSaving = false, saveFailed = true) }
                    }
                }
            }
        }
    }

    private fun ProfileFormState.errors(): ProfileFormErrors =
        (ProfileFormValidator.validate(this) as? ProfileFormResult.Invalid)?.errors ?: ProfileFormErrors()
}
