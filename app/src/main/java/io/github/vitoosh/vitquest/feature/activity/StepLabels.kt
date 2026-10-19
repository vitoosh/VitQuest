package io.github.vitoosh.vitquest.feature.activity

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.VqBadge
import io.github.vitoosh.vitquest.core.model.StepSource
import io.github.vitoosh.vitquest.core.model.StepSourcePreference
import io.github.vitoosh.vitquest.core.ui.currentLocale
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@get:StringRes
val StepSource.labelRes: Int
    get() = when (this) {
        StepSource.HEALTH_CONNECT -> R.string.step_source_health_connect
        StepSource.PHONE_SENSOR -> R.string.step_source_phone_sensor
        StepSource.MANUAL -> R.string.step_source_manual
    }

@get:StringRes
val StepSourcePreference.labelRes: Int
    get() = when (this) {
        StepSourcePreference.AUTOMATIC -> R.string.step_preference_automatic
        StepSourcePreference.HEALTH_CONNECT -> R.string.step_preference_health_connect
        StepSourcePreference.PHONE_SENSOR -> R.string.step_preference_phone_sensor
        StepSourcePreference.MANUAL_ONLY -> R.string.step_preference_manual_only
    }

@get:StringRes
val StepSourcePreference.descriptionRes: Int
    get() = when (this) {
        StepSourcePreference.AUTOMATIC -> R.string.step_preference_automatic_description
        StepSourcePreference.HEALTH_CONNECT -> R.string.step_preference_health_connect_description
        StepSourcePreference.PHONE_SENSOR -> R.string.step_preference_phone_sensor_description
        StepSourcePreference.MANUAL_ONLY -> R.string.step_preference_manual_only_description
    }

/** Shows which source a number came from. Device sources and manual entries look different. */
@Composable
fun StepSourceTag(source: StepSource, modifier: Modifier = Modifier) {
    val manual = source == StepSource.MANUAL
    VqBadge(
        text = stringResource(source.labelRes),
        containerColor = if (manual) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (manual) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier,
    )
}

/** "Today", "Yesterday" or a short localised date. */
@Composable
fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.day_today)
    today.minusDays(1) -> stringResource(R.string.day_yesterday)
    else -> DateTimeFormatter.ofPattern("EEE, d MMM", currentLocale()).format(date)
}

@Composable
fun shortWeekday(date: LocalDate): String = DateTimeFormatter.ofPattern("EEE", currentLocale()).format(date)
