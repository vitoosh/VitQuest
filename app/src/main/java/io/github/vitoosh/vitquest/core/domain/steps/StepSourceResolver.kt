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
 *
 * The phone has two counts of the same walking: its built-in step counter and
 * VitQuest's motion count. They are one tier and the larger one is shown,
 * because a phone's counter can miss steps but neither invents them.
 */
object StepSourceResolver {

    fun priority(preference: StepSourcePreference): List<StepSource> = when (preference) {
        StepSourcePreference.AUTOMATIC -> listOf(StepSource.MANUAL, StepSource.HEALTH_CONNECT, StepSource.PHONE_SENSOR, StepSource.MOTION)
        StepSourcePreference.HEALTH_CONNECT -> listOf(StepSource.MANUAL, StepSource.HEALTH_CONNECT)
        StepSourcePreference.PHONE_SENSOR -> listOf(StepSource.MANUAL, StepSource.PHONE_SENSOR, StepSource.MOTION)
        StepSourcePreference.MANUAL_ONLY -> listOf(StepSource.MANUAL)
    }

    fun resolve(records: List<StepRecord>, preference: StepSourcePreference): Map<LocalDate, DailySteps> {
        val order = priority(preference)
        return records
            .filter { it.source in order }
            .groupBy { it.date }
            .mapValues { (date, dayRecords) ->
                val bestTier = dayRecords.minOf { tier(order, it.source) }
                val chosen = dayRecords.filter { tier(order, it.source) == bestTier }.maxBy { it.steps }
                DailySteps(date = date, steps = chosen.steps, source = chosen.source)
            }
    }

    /** The motion count shares the built-in counter's tier. */
    private fun tier(order: List<StepSource>, source: StepSource): Int =
        order.indexOf(if (source == StepSource.MOTION) StepSource.PHONE_SENSOR else source)

    fun usesHealthConnect(preference: StepSourcePreference): Boolean =
        StepSource.HEALTH_CONNECT in priority(preference)

    fun usesPhoneSensor(preference: StepSourcePreference): Boolean =
        StepSource.PHONE_SENSOR in priority(preference)
}
