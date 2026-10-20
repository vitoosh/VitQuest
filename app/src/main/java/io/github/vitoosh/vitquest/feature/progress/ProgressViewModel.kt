package io.github.vitoosh.vitquest.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.core.data.repository.BodyRepository
import io.github.vitoosh.vitquest.core.data.repository.HabitRepository
import io.github.vitoosh.vitquest.core.data.repository.ProfileRepository
import io.github.vitoosh.vitquest.core.data.repository.WorkoutRepository
import io.github.vitoosh.vitquest.core.domain.habits.AchievementStatus
import io.github.vitoosh.vitquest.core.domain.habits.Achievements
import io.github.vitoosh.vitquest.core.domain.habits.ProgressStats
import io.github.vitoosh.vitquest.core.domain.habits.WeekSummary
import io.github.vitoosh.vitquest.core.model.WeightEntry
import io.github.vitoosh.vitquest.core.time.TimeSource
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class ProgressUiState(
    val today: LocalDate,
    /** Oldest first; the last one is this week. */
    val weeks: List<WeekSummary>,
    val weights: List<WeightEntry>,
    val targetWeightKg: Double?,
    val achievements: List<AchievementStatus>,
) {
    val thisWeek: WeekSummary get() = weeks.last()
    val lastWeek: WeekSummary get() = weeks[weeks.lastIndex - 1]
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProgressViewModel @Inject constructor(
    habitRepository: HabitRepository,
    workoutRepository: WorkoutRepository,
    bodyRepository: BodyRepository,
    profileRepository: ProfileRepository,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val today = MutableStateFlow(timeSource.today())

    val uiState: StateFlow<ProgressUiState?> = today.flatMapLatest { date ->
        combine(
            // Achievements look at all stored history, which stays small for a personal app.
            habitRepository.dayRecords(HISTORY_START, date),
            workoutRepository.sessions(),
            bodyRepository.weights(),
            profileRepository.goals,
            habitRepository.customHabits(),
        ) { records, sessions, weights, goals, customs ->
            ProgressUiState(
                today = date,
                weeks = ProgressStats.weeks(date, WEEKS, records, goals),
                weights = weights.filter { !it.date.isBefore(date.minusDays(WEIGHT_DAYS)) },
                targetWeightKg = goals?.targetWeightKg,
                achievements = Achievements.evaluate(records.values, sessions, goals, customs),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun refreshToday() {
        today.value = timeSource.today()
    }

    private companion object {
        const val WEEKS = 8
        const val WEIGHT_DAYS = 90L
        val HISTORY_START: LocalDate = LocalDate.of(2000, 1, 1)
    }
}
