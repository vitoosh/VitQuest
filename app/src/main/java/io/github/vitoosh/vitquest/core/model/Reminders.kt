package io.github.vitoosh.vitquest.core.model

import java.time.DayOfWeek
import java.time.LocalTime

enum class ReminderType(val usesInterval: Boolean, val defaultIntervalMinutes: Int?) {
    WATER(usesInterval = true, defaultIntervalMinutes = 30),
    MOVEMENT(usesInterval = true, defaultIntervalMinutes = 60),
    WORKOUT(usesInterval = false, defaultIntervalMinutes = null),
    FOOD_LOG(usesInterval = false, defaultIntervalMinutes = null),
    WEIGHT(usesInterval = false, defaultIntervalMinutes = null),
}

/**
 * Settings for one reminder. Interval reminders repeat within the active
 * hours; timed reminders fire at [time] on [days]. Times are never assumed:
 * a timed reminder stays off until the user picks a time.
 */
data class ReminderSetting(
    val type: ReminderType,
    val enabled: Boolean = false,
    val intervalMinutes: Int? = type.defaultIntervalMinutes,
    val time: LocalTime? = null,
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
)

/** Active hours (for interval reminders) and quiet hours (for all). Unset until the user chooses. */
data class ReminderWindow(
    val activeStart: LocalTime? = null,
    val activeEnd: LocalTime? = null,
    val quietStart: LocalTime? = null,
    val quietEnd: LocalTime? = null,
) {
    val hasActiveHours: Boolean get() = activeStart != null && activeEnd != null && activeStart != activeEnd
    val hasQuietHours: Boolean get() = quietStart != null && quietEnd != null && quietStart != quietEnd
}
