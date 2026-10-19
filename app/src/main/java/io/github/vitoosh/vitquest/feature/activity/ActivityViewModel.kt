package io.github.vitoosh.vitquest.feature.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.core.data.repository.OfflineFirstStepsRepository
import io.github.vitoosh.vitquest.core.data.repository.ProfileRepository
import io.github.vitoosh.vitquest.core.data.repository.StepSourcesStatus
import io.github.vitoosh.vitquest.core.data.repository.StepsRepository
import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.domain.FieldResult
import io.github.vitoosh.vitquest.core.domain.InputLimits
import io.github.vitoosh.vitquest.core.domain.NumberInput
import io.github.vitoosh.vitquest.core.domain.steps.StepDay
import io.github.vitoosh.vitquest.core.domain.steps.StepStats
import io.github.vitoosh.vitquest.core.domain.steps.StepSummary
import io.github.vitoosh.vitquest.core.model.StepSourcePreference
import io.github.vitoosh.vitquest.core.time.TimeSource
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ActivityUiState {
    data object Loading : ActivityUiState

    data class Ready(
        val today: StepDay,
        val goal: Int?,
        /** Last 7 days, oldest first; the last entry is today. */
        val week: List<StepDay>,
        val weekSummary: StepSummary,
        /** Last 30 days, newest first. */
        val history: List<StepDay>,
        val status: StepSourcesStatus?,
        val preference: StepSourcePreference,
    ) : ActivityUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val stepsRepository: StepsRepository,
    private val profileRepository: ProfileRepository,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val today = MutableStateFlow(timeSource.today())

    val uiState: StateFlow<ActivityUiState> = today.flatMapLatest { date ->
        val from = date.minusDays(OfflineFirstStepsRepository.HISTORY_DAYS - 1)
        combine(
            stepsRepository.observeDailySteps(from, date),
            profileRepository.goals.map { it?.dailyStepGoal },
            stepsRepository.sourcesStatus,
            stepsRepository.preference,
        ) { byDate, goal, status, preference ->
            val history = StepStats.days(from, date, byDate)
            val week = history.takeLast(WEEK_DAYS)
            ActivityUiState.Ready(
                today = history.last(),
                goal = goal,
                week = week,
                weekSummary = StepStats.summarize(week, goal),
                history = history.reversed(),
                status = status,
                preference = preference,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityUiState.Loading)

    /** Called on every resume so permission changes made outside the app are picked up. */
    fun refresh(force: Boolean = false) {
        today.value = timeSource.today()
        viewModelScope.launch { stepsRepository.refresh(force) }
    }

    /** Returns an error to show, or null when the value was saved. */
    fun saveManualSteps(date: LocalDate, input: String): FieldError? {
        return when (val result = NumberInput.parseInt(input, InputLimits.MANUAL_DAILY_STEPS, required = true)) {
            is FieldResult.Invalid -> result.error
            is FieldResult.Valid -> {
                viewModelScope.launch { stepsRepository.setManualSteps(date, requireNotNull(result.value)) }
                null
            }
        }
    }

    fun clearManualSteps(date: LocalDate) {
        viewModelScope.launch { stepsRepository.clearManualSteps(date) }
    }

    fun saveStepGoal(input: String): FieldError? {
        return when (val result = NumberInput.parseInt(input, InputLimits.DAILY_STEPS, required = true)) {
            is FieldResult.Invalid -> result.error
            is FieldResult.Valid -> {
                viewModelScope.launch { profileRepository.updateDailyStepGoal(requireNotNull(result.value)) }
                null
            }
        }
    }

    fun setPreference(preference: StepSourcePreference) {
        viewModelScope.launch { stepsRepository.setPreference(preference) }
    }

    private companion object {
        const val WEEK_DAYS = 7
    }
}
