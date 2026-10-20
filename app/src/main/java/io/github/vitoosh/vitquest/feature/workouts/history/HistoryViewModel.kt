package io.github.vitoosh.vitquest.feature.workouts.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.core.data.repository.ManualWorkoutInput
import io.github.vitoosh.vitquest.core.data.repository.ProfileRepository
import io.github.vitoosh.vitquest.core.data.repository.WorkoutRepository
import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.domain.FieldResult
import io.github.vitoosh.vitquest.core.domain.InputLimits
import io.github.vitoosh.vitquest.core.domain.NumberInput
import io.github.vitoosh.vitquest.core.domain.habits.HabitStats
import io.github.vitoosh.vitquest.core.model.WorkoutIntensity
import io.github.vitoosh.vitquest.core.model.WorkoutSession
import io.github.vitoosh.vitquest.core.time.TimeSource
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val today: LocalDate,
    val sessions: List<WorkoutSession>,
    val weekSessions: Int,
    val weekActiveSeconds: Int,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val profileRepository: ProfileRepository,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val today = MutableStateFlow(timeSource.today())

    val uiState: StateFlow<HistoryUiState?> = combine(workoutRepository.sessions(), today) { sessions, date ->
        val week = sessions.filter { !it.date.isBefore(HabitStats.weekStart(date)) && !it.date.isAfter(date) }
        HistoryUiState(date, sessions, week.size, week.sumOf { it.activeSeconds })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun refreshToday() {
        today.value = timeSource.today()
    }

    fun logManual(date: LocalDate, minutesInput: String, intensity: WorkoutIntensity, note: String): FieldError? =
        when (val result = NumberInput.parseInt(minutesInput, InputLimits.WORKOUT_MINUTES, required = true)) {
            is FieldResult.Invalid -> result.error
            is FieldResult.Valid -> {
                viewModelScope.launch {
                    val weight = profileRepository.profile.first()?.currentWeightKg
                    workoutRepository.logManual(ManualWorkoutInput(date, requireNotNull(result.value), intensity, note), weight)
                }
                null
            }
        }

    fun delete(session: WorkoutSession) {
        viewModelScope.launch { workoutRepository.deleteSession(session.id) }
    }
}
