package io.github.vitoosh.vitquest.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.core.data.repository.ProfileRepository
import io.github.vitoosh.vitquest.core.model.Goals
import io.github.vitoosh.vitquest.core.model.UserProfile
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Ready(
        val profile: UserProfile?,
        val goals: Goals?,
    ) : HomeUiState {
        /** Remaining kilograms to the target, only when both values were entered by the user. */
        val kgToTarget: Double?
            get() {
                val current = profile?.currentWeightKg ?: return null
                val target = goals?.targetWeightKg ?: return null
                return current - target
            }
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    profileRepository: ProfileRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        combine(profileRepository.profile, profileRepository.goals) { profile, goals ->
            HomeUiState.Ready(profile = profile, goals = goals)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading,
        )
}
