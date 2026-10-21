package io.github.vitoosh.vitquest.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.vitoosh.vitquest.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** "Today", "Yesterday" or a short localised date. */
@Composable
fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.day_today)
    today.minusDays(1) -> stringResource(R.string.day_yesterday)
    else -> DateTimeFormatter.ofPattern("EEE, d MMM", currentLocale()).format(date)
}

@Composable
fun shortWeekday(date: LocalDate): String = DateTimeFormatter.ofPattern("EEE", currentLocale()).format(date)

/** Short localised date such as "19 Okt" or "Oct 19". */
@Composable
fun shortDate(date: LocalDate): String = DateTimeFormatter.ofPattern("d MMM", currentLocale()).format(date)
