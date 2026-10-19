package io.github.vitoosh.vitquest.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.core.data.repository.ProfileRepository
import io.github.vitoosh.vitquest.core.data.repository.StepsRepository
import io.github.vitoosh.vitquest.core.model.DailySteps
import io.github.vitoosh.vitquest.core.time.TimeSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
        /** Null means no source reported steps today, which is different from 0. */
        val todaySteps: DailySteps? = null,
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    private val stepsRepository: StepsRepository,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val today = MutableStateFlow(timeSource.today())

    private val todaySteps = today.flatMapLatest { date ->
        stepsRepository.observeDailySteps(date, date).map { it[date] }
    }

    val uiState: StateFlow<HomeUiState> =
        combine(profileRepository.profile, profileRepository.goals, todaySteps) { profile, goals, steps ->
            HomeUiState.Ready(profile = profile, goals = goals, todaySteps = steps)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading,
        )

    fun refresh() {
        today.value = timeSource.today()
        viewModelScope.launch { stepsRepository.refresh() }
    }
}
