package io.github.vitoosh.vitquest.core.domain.weight

import io.github.vitoosh.vitquest.core.model.WeightEntry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

data class WeightSummary(
    val latest: WeightEntry?,
    val start: WeightEntry?,
    /** Change from the first entry to the latest, negative when weight went down. */
    val totalChangeKg: Double?,
    val thisWeekAverageKg: Double?,
    val lastWeekAverageKg: Double?,
    val last30DaysChangeKg: Double?,
    /** Average change per week over the last 28 days; null with too little data. */
    val weeklyPaceKg: Double?,
    /** 0..1 progress from the start weight toward the target, when a target exists. */
    val progressToTarget: Float?,
    /** True when losing faster than about 1% of body weight per week. */
    val fastLossWarning: Boolean,
)

object WeightStats {

    private const val FAST_LOSS_FRACTION_PER_WEEK = 0.01
    private const val PACE_WINDOW_DAYS = 28L
    private const val MIN_PACE_SPAN_DAYS = 7L

    fun summarize(entries: List<WeightEntry>, targetKg: Double?, today: LocalDate): WeightSummary {
        val sorted = entries.sortedBy { it.date }
        val latest = sorted.lastOrNull()
        val start = sorted.firstOrNull()

        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val thisWeek = sorted.filter { !it.date.isBefore(weekStart) && !it.date.isAfter(today) }
        val lastWeek = sorted.filter { !it.date.isBefore(weekStart.minusWeeks(1)) && it.date.isBefore(weekStart) }

        val monthAgo = today.minusDays(30)
        val baseline30 = sorted.lastOrNull { !it.date.isAfter(monthAgo) } ?: sorted.firstOrNull { !it.date.isBefore(monthAgo) }
        val last30Change = if (latest != null && baseline30 != null && baseline30 != latest) {
            latest.weightKg - baseline30.weightKg
        } else {
            null
        }

        val windowEntries = sorted.filter { !it.date.isBefore(today.minusDays(PACE_WINDOW_DAYS)) }
        val pace = if (windowEntries.size >= 2) {
            val first = windowEntries.first()
            val last = windowEntries.last()
            val days = ChronoUnit.DAYS.between(first.date, last.date)
            if (days >= MIN_PACE_SPAN_DAYS) (last.weightKg - first.weightKg) / days * 7 else null
        } else {
            null
        }

        val progress = if (targetKg != null && start != null && latest != null && start.weightKg != targetKg) {
            ((start.weightKg - latest.weightKg) / (start.weightKg - targetKg)).toFloat().coerceIn(0f, 1f)
        } else {
            null
        }

        return WeightSummary(
            latest = latest,
            start = start,
            totalChangeKg = if (latest != null && start != null && latest != start) latest.weightKg - start.weightKg else null,
            thisWeekAverageKg = thisWeek.takeIf { it.isNotEmpty() }?.map { it.weightKg }?.average(),
            lastWeekAverageKg = lastWeek.takeIf { it.isNotEmpty() }?.map { it.weightKg }?.average(),
            last30DaysChangeKg = last30Change,
            weeklyPaceKg = pace,
            progressToTarget = progress,
            fastLossWarning = pace != null && latest != null && -pace > latest.weightKg * FAST_LOSS_FRACTION_PER_WEEK,
        )
    }
}
