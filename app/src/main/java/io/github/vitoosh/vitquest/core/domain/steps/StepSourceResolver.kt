package io.github.vitoosh.vitquest.core.domain.steps

import io.github.vitoosh.vitquest.core.model.DailySteps
import io.github.vitoosh.vitquest.core.model.StepRecord
import io.github.vitoosh.vitquest.core.model.StepSource
import io.github.vitoosh.vitquest.core.model.StepSourcePreference
import java.time.LocalDate

/**
 * Picks exactly one source per day. Health Connect already de-duplicates the
 * apps that write to it, so its daily total is never added to the phone
 * sensor; the next source is only used when the preferred one has no value.
 */
object StepSourceResolver {

    fun priority(preference: StepSourcePreference): List<StepSource> = when (preference) {
        StepSourcePreference.AUTOMATIC -> listOf(StepSource.MANUAL, StepSource.HEALTH_CONNECT, StepSource.PHONE_SENSOR)
        StepSourcePreference.HEALTH_CONNECT -> listOf(StepSource.MANUAL, StepSource.HEALTH_CONNECT)
        StepSourcePreference.PHONE_SENSOR -> listOf(StepSource.MANUAL, StepSource.PHONE_SENSOR)
        StepSourcePreference.MANUAL_ONLY -> listOf(StepSource.MANUAL)
    }

    fun resolve(records: List<StepRecord>, preference: StepSourcePreference): Map<LocalDate, DailySteps> {
        val order = priority(preference)
        return records
            .filter { it.source in order }
            .groupBy { it.date }
            .mapValues { (date, dayRecords) ->
                val chosen = dayRecords.minBy { order.indexOf(it.source) }
                DailySteps(date = date, steps = chosen.steps, source = chosen.source)
            }
    }

    fun usesHealthConnect(preference: StepSourcePreference): Boolean =
        StepSource.HEALTH_CONNECT in priority(preference)

    fun usesPhoneSensor(preference: StepSourcePreference): Boolean =
        StepSource.PHONE_SENSOR in priority(preference)
}
