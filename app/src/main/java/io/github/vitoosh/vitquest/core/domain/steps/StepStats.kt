package io.github.vitoosh.vitquest.core.domain.steps

import io.github.vitoosh.vitquest.core.model.DailySteps
import java.time.LocalDate

/** A calendar day in a range, with or without step data. */
data class StepDay(val date: LocalDate, val steps: DailySteps?)

data class StepSummary(
    val totalSteps: Long,
    /** Average over days that have data; null when no day has data. */
    val averageSteps: Int?,
    val daysWithData: Int,
    val daysGoalMet: Int,
    val dayCount: Int,
)

object StepStats {

    /** Every day from [from] to [to] (inclusive), oldest first, filled from [byDate]. */
    fun days(from: LocalDate, to: LocalDate, byDate: Map<LocalDate, DailySteps>): List<StepDay> =
        generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .map { StepDay(it, byDate[it]) }
            .toList()

    fun summarize(days: List<StepDay>, goal: Int?): StepSummary {
        val withData = days.mapNotNull { it.steps }
        val total = withData.sumOf { it.steps.toLong() }
        return StepSummary(
            totalSteps = total,
            averageSteps = if (withData.isEmpty()) null else (total / withData.size).toInt(),
            daysWithData = withData.size,
            daysGoalMet = if (goal == null) 0 else withData.count { it.steps >= goal },
            dayCount = days.size,
        )
    }

    /** Progress toward the goal in 0..1 for progress bars. */
    fun progress(steps: Int, goal: Int): Float = if (goal <= 0) 0f else (steps.toFloat() / goal).coerceIn(0f, 1f)
}
