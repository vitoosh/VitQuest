package io.github.vitoosh.vitquest.feature.home

import androidx.annotation.StringRes
import io.github.vitoosh.vitquest.R

enum class DayPeriod(@StringRes val greetingRes: Int) {
    MORNING(R.string.home_greeting_morning),
    MIDDAY(R.string.home_greeting_midday),
    AFTERNOON(R.string.home_greeting_afternoon),
    EVENING(R.string.home_greeting_evening),
    ;

    companion object {
        /** Follows common Indonesian usage: pagi, siang, sore, malam. */
        fun fromHour(hour: Int): DayPeriod {
            require(hour in 0..23) { "Hour must be 0..23" }
            return when (hour) {
                in 4..10 -> MORNING
                in 11..14 -> MIDDAY
                in 15..17 -> AFTERNOON
                else -> EVENING
            }
        }
    }
}
