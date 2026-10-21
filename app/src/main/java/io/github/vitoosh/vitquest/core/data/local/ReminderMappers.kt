package io.github.vitoosh.vitquest.core.data.local

import io.github.vitoosh.vitquest.core.model.ReminderSetting
import io.github.vitoosh.vitquest.core.model.ReminderType
import io.github.vitoosh.vitquest.core.model.ReminderWindow
import java.time.DayOfWeek
import java.time.LocalTime

private fun Int.toTime(): LocalTime = LocalTime.of(this / 60 % 24, this % 60)

private fun LocalTime.toMinutes(): Int = hour * 60 + minute

fun ReminderSettingEntity.toModel(): ReminderSetting? {
    val type = type.toEnumOrNull<ReminderType>() ?: return null
    return ReminderSetting(
        type = type,
        enabled = enabled,
        intervalMinutes = intervalMinutes ?: type.defaultIntervalMinutes,
        time = timeMinutes?.toTime(),
        days = days.toEnumSet<DayOfWeek>(),
    )
}

fun ReminderSetting.toEntity() = ReminderSettingEntity(
    type = type.name,
    enabled = enabled,
    intervalMinutes = intervalMinutes,
    timeMinutes = time?.toMinutes(),
    days = days.sortedBy { it.ordinal }.joinToString(",") { it.name },
)

fun ReminderWindowEntity?.toModel() = ReminderWindow(
    activeStart = this?.activeStart?.toTime(),
    activeEnd = this?.activeEnd?.toTime(),
    quietStart = this?.quietStart?.toTime(),
    quietEnd = this?.quietEnd?.toTime(),
)

fun ReminderWindow.toEntity() = ReminderWindowEntity(
    activeStart = activeStart?.toMinutes(),
    activeEnd = activeEnd?.toMinutes(),
    quietStart = quietStart?.toMinutes(),
    quietEnd = quietEnd?.toMinutes(),
)
