package io.github.vitoosh.vitquest.core.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder_settings")
data class ReminderSettingEntity(
    @PrimaryKey val type: String,
    val enabled: Boolean,
    @ColumnInfo(name = "interval_minutes") val intervalMinutes: Int?,
    /** Minutes after midnight. */
    @ColumnInfo(name = "time_minutes") val timeMinutes: Int?,
    /** Comma-separated DayOfWeek names. */
    val days: String,
)

/** Single row holding active and quiet hours as minutes after midnight. */
@Entity(tableName = "reminder_window")
data class ReminderWindowEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "active_start") val activeStart: Int?,
    @ColumnInfo(name = "active_end") val activeEnd: Int?,
    @ColumnInfo(name = "quiet_start") val quietStart: Int?,
    @ColumnInfo(name = "quiet_end") val quietEnd: Int?,
)
