package io.github.vitoosh.vitquest.core.domain.habits

import io.github.vitoosh.vitquest.core.model.Goals
import java.time.LocalDate
import kotlin.math.roundToInt

/** One Monday-to-Sunday week (only days up to today count). */
data class WeekSummary(
    val start: LocalDate,
    /** Average over days with step data; null when there were none. */
    val averageSteps: Int?,
    val stepDays: Int,
    val workoutDays: Int,
    val workoutSeconds: Int,
    /** Null when no water target is set. */
    val waterTargetDays: Int?,
    val foodLogDays: Int,
)

object ProgressStats {

    fun week(start: LocalDate, today: LocalDate, records: Map<LocalDate, DayRecord>, goals: Goals?): WeekSummary {
        val days = (0L until 7L).map { start.plusDays(it) }.filter { !it.isAfter(today) }.mapNotNull { records[it] }
        val stepValues = days.mapNotNull { it.steps }
        val waterTarget = goals?.dailyWaterTargetMl?.takeIf { it > 0 }
        return WeekSummary(
            start = start,
            averageSteps = stepValues.takeIf { it.isNotEmpty() }?.average()?.roundToInt(),
            stepDays = stepValues.size,
            workoutDays = days.count { it.workoutCount > 0 },
            workoutSeconds = days.sumOf { it.workoutSeconds },
            waterTargetDays = waterTarget?.let { target -> days.count { it.waterMl >= target } },
            foodLogDays = days.count { it.foodLogged },
        )
    }

    /** The last [count] weeks, oldest first, ending with the current week. */
    fun weeks(today: LocalDate, count: Int, records: Map<LocalDate, DayRecord>, goals: Goals?): List<WeekSummary> {
        val current = HabitStats.weekStart(today)
        return (count - 1 downTo 0).map { back -> week(current.minusWeeks(back.toLong()), today, records, goals) }
    }
}
