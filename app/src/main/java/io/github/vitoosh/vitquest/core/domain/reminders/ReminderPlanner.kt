package io.github.vitoosh.vitquest.core.domain.reminders

import io.github.vitoosh.vitquest.core.model.ReminderSetting
import io.github.vitoosh.vitquest.core.model.ReminderWindow
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Why a reminder cannot be scheduled; the settings screen explains each one. */
enum class ReminderProblem { NEEDS_ACTIVE_HOURS, NEEDS_TIME, NEEDS_DAYS }

/**
 * Works out when a reminder should next fire. Pure and minute-based, so it
 * is easy to test across midnight, quiet hours and time-zone changes.
 */
object ReminderPlanner {
    private const val SEARCH_LIMIT_MINUTES = 8L * 24 * 60

    fun problem(setting: ReminderSetting, window: ReminderWindow): ReminderProblem? = when {
        setting.type.usesInterval && !window.hasActiveHours -> ReminderProblem.NEEDS_ACTIVE_HOURS
        !setting.type.usesInterval && setting.time == null -> ReminderProblem.NEEDS_TIME
        !setting.type.usesInterval && setting.days.isEmpty() -> ReminderProblem.NEEDS_DAYS
        else -> null
    }

    /**
     * The next time after [after], or null when the reminder is off or incomplete.
     * With [skipRestOfToday] (the goal is already reached) it starts looking at the next day.
     */
    fun next(setting: ReminderSetting, window: ReminderWindow, after: ZonedDateTime, skipRestOfToday: Boolean = false): ZonedDateTime? {
        if (!setting.enabled || problem(setting, window) != null) return null
        val start = after.truncatedTo(ChronoUnit.MINUTES)
        val tomorrow = after.toLocalDate().plusDays(1).atStartOfDay(after.zone)
        return if (setting.type.usesInterval) {
            val interval = (setting.intervalMinutes ?: 30).coerceAtLeast(MIN_INTERVAL_MINUTES).toLong()
            val first = if (skipRestOfToday) tomorrow else start.plusMinutes(interval)
            search(first) { time -> inRange(time.toLocalTime(), window.activeStart!!, window.activeEnd!!) && !isQuiet(time, window) }
        } else {
            val time = setting.time!!
            val firstDay = if (skipRestOfToday) tomorrow.toLocalDate() else after.toLocalDate()
            (0L..7L).asSequence()
                .map { firstDay.plusDays(it) }
                .filter { it.dayOfWeek in setting.days }
                .mapNotNull { day ->
                    // Quiet hours push the reminder later the same day, never to another day.
                    val base = day.atTime(time).atZone(after.zone)
                    generateSequence(base) { it.plusMinutes(1) }
                        .takeWhile { it.toLocalDate() == day }
                        .firstOrNull { !isQuiet(it, window) }
                }
                .firstOrNull { it.isAfter(after) }
        }
    }

    /** True while quiet hours are on; nothing is shown then. */
    fun isQuiet(time: ZonedDateTime, window: ReminderWindow): Boolean =
        window.hasQuietHours && inRange(time.toLocalTime(), window.quietStart!!, window.quietEnd!!)

    /** [start] inclusive, [end] exclusive; ranges may cross midnight. */
    fun inRange(time: LocalTime, start: LocalTime, end: LocalTime): Boolean =
        if (start.isBefore(end)) !time.isBefore(start) && time.isBefore(end) else !time.isBefore(start) || time.isBefore(end)

    private inline fun search(from: ZonedDateTime, allowed: (ZonedDateTime) -> Boolean): ZonedDateTime? {
        var candidate = from
        repeat(SEARCH_LIMIT_MINUTES.toInt()) {
            if (allowed(candidate)) return candidate
            candidate = candidate.plusMinutes(1)
        }
        return null
    }

    const val MIN_INTERVAL_MINUTES = 15
}
