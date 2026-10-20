package io.github.vitoosh.vitquest.core.domain.habits

import io.github.vitoosh.vitquest.core.model.BuiltInHabit
import io.github.vitoosh.vitquest.core.model.CustomHabit
import io.github.vitoosh.vitquest.core.model.Goals
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** What the trackers recorded on one day; the source for built-in habits and achievements. */
data class DayRecord(
    val date: LocalDate,
    /** Null when no step source reported anything that day. */
    val steps: Int? = null,
    val waterMl: Int = 0,
    val foodLogged: Boolean = false,
    val workoutCount: Int = 0,
    val workoutSeconds: Int = 0,
)

enum class DayStatus { DONE, NOT_DONE, FUTURE, NO_TARGET }

data class HabitWeek(
    /** Monday to Sunday. */
    val days: List<Pair<LocalDate, DayStatus>>,
    val doneCount: Int,
    val targetDays: Int,
) {
    val targetMet: Boolean get() = doneCount >= targetDays
}

/**
 * Weekly, target-based habit tracking. There are deliberately no streaks:
 * a missed day never resets anything, it only means fewer days this week.
 */
object HabitStats {

    fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** Null when the habit cannot be judged because the user has not set the matching goal. */
    fun isDone(habit: BuiltInHabit, record: DayRecord?, goals: Goals?): Boolean? = when (habit) {
        BuiltInHabit.MOVEMENT -> goals?.dailyStepGoal?.takeIf { it > 0 }?.let { goal -> (record?.steps ?: 0) >= goal }
        BuiltInHabit.WORKOUT -> (record?.workoutCount ?: 0) > 0
        BuiltInHabit.HYDRATION -> goals?.dailyWaterTargetMl?.takeIf { it > 0 }?.let { target -> (record?.waterMl ?: 0) >= target }
        BuiltInHabit.FOOD_LOG -> record?.foodLogged == true
    }

    fun week(today: LocalDate, targetDays: Int, isDone: (LocalDate) -> Boolean?): HabitWeek {
        val start = weekStart(today)
        val days = (0L until 7L).map { offset ->
            val day = start.plusDays(offset)
            day to when {
                day.isAfter(today) -> DayStatus.FUTURE
                else -> when (isDone(day)) {
                    true -> DayStatus.DONE
                    false -> DayStatus.NOT_DONE
                    null -> DayStatus.NO_TARGET
                }
            }
        }
        return HabitWeek(days, days.count { it.second == DayStatus.DONE }, targetDays)
    }

    /** How many of the last [weeks] full weeks (before this one) reached the weekly target. */
    fun weeksOnTarget(today: LocalDate, targetDays: Int, weeks: Int = 4, isDone: (LocalDate) -> Boolean?): Int {
        val thisWeek = weekStart(today)
        return (1..weeks).count { back ->
            val start = thisWeek.minusWeeks(back.toLong())
            (0L until 7L).count { isDone(start.plusDays(it)) == true } >= targetDays
        }
    }

    fun customDone(habit: CustomHabit): (LocalDate) -> Boolean? = { day -> day in habit.checkIns }

    fun builtInDone(habit: BuiltInHabit, records: Map<LocalDate, DayRecord>, goals: Goals?): (LocalDate) -> Boolean? =
        { day -> isDone(habit, records[day], goals) }
}
