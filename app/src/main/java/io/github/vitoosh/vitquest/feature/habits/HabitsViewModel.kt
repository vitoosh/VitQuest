package io.github.vitoosh.vitquest.feature.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.core.data.repository.HabitRepository
import io.github.vitoosh.vitquest.core.data.repository.ProfileRepository
import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.domain.InputLimits
import io.github.vitoosh.vitquest.core.domain.habits.DayRecord
import io.github.vitoosh.vitquest.core.domain.habits.HabitStats
import io.github.vitoosh.vitquest.core.domain.habits.HabitWeek
import io.github.vitoosh.vitquest.core.model.BuiltInHabit
import io.github.vitoosh.vitquest.core.model.CustomHabit
import io.github.vitoosh.vitquest.core.model.Goals
import io.github.vitoosh.vitquest.core.time.TimeSource
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BuiltInHabitItem(val habit: BuiltInHabit, val week: HabitWeek, val weeksOnTarget: Int, val doneToday: Boolean?)

data class CustomHabitItem(val habit: CustomHabit, val week: HabitWeek, val weeksOnTarget: Int, val doneToday: Boolean)

data class HabitsUiState(
    val today: LocalDate,
    val builtIns: List<BuiltInHabitItem>,
    val customs: List<CustomHabitItem>,
) {
    /** Habits that can be judged today (built-ins need their goal set). */
    val todayTotal: Int get() = builtIns.count { it.doneToday != null } + customs.size
    val todayDone: Int get() = builtIns.count { it.doneToday == true } + customs.count { it.doneToday }
}

/** Builds the habit overview; shared by the Habits screen and the Home card. */
object HabitsOverview {
    const val WEEKS_BACK = 4L

    fun from(today: LocalDate): LocalDate = HabitStats.weekStart(today).minusWeeks(WEEKS_BACK)

    fun build(today: LocalDate, records: Map<LocalDate, DayRecord>, goals: Goals?, customs: List<CustomHabit>) = HabitsUiState(
        today = today,
        builtIns = BuiltInHabit.entries.map { habit ->
            val isDone = HabitStats.builtInDone(habit, records, goals)
            BuiltInHabitItem(
                habit = habit,
                week = HabitStats.week(today, habit.targetDaysPerWeek, isDone),
                weeksOnTarget = HabitStats.weeksOnTarget(today, habit.targetDaysPerWeek, WEEKS_BACK.toInt(), isDone),
                doneToday = isDone(today),
            )
        },
        customs = customs.map { habit ->
            val isDone = HabitStats.customDone(habit)
            CustomHabitItem(
                habit = habit,
                week = HabitStats.week(today, habit.targetDaysPerWeek, isDone),
                weeksOnTarget = HabitStats.weeksOnTarget(today, habit.targetDaysPerWeek, WEEKS_BACK.toInt(), isDone),
                doneToday = today in habit.checkIns,
            )
        },
    )

    fun observe(today: LocalDate, habits: HabitRepository, profiles: ProfileRepository): Flow<HabitsUiState> =
        combine(habits.dayRecords(from(today), today), profiles.goals, habits.customHabits()) { records, goals, customs ->
            build(today, records, goals, customs)
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    profileRepository: ProfileRepository,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val today = MutableStateFlow(timeSource.today())

    val uiState: StateFlow<HabitsUiState?> = today
        .flatMapLatest { date -> HabitsOverview.observe(date, habitRepository, profileRepository) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun refreshToday() {
        today.value = timeSource.today()
    }

    /** Past days can be ticked or unticked to correct a record; future days cannot. */
    fun toggle(habit: CustomHabit, date: LocalDate) {
        if (date.isAfter(timeSource.today())) return
        viewModelScope.launch { habitRepository.setCheckIn(habit.id, date, date !in habit.checkIns) }
    }

    fun save(existing: CustomHabit?, name: String, targetDays: Int): FieldError? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return FieldError.REQUIRED
        if (trimmed.length !in InputLimits.HABIT_NAME_LENGTH) return FieldError.OUT_OF_RANGE
        val days = targetDays.coerceIn(1, 7)
        viewModelScope.launch {
            if (existing == null) habitRepository.addHabit(trimmed, days) else habitRepository.updateHabit(existing.id, trimmed, days)
        }
        return null
    }

    fun delete(habit: CustomHabit) {
        viewModelScope.launch { habitRepository.deleteHabit(habit.id) }
    }
}
