package io.github.vitoosh.vitquest.core.model

import java.time.Instant
import java.time.LocalDate

/**
 * Where a daily step count came from. [PHONE_SENSOR] is the phone's built-in step
 * counter; [MOTION] is VitQuest's own count from the accelerometer, for phones
 * whose built-in counter misses steps.
 */
enum class StepSource { HEALTH_CONNECT, PHONE_SENSOR, MOTION, MANUAL }

/** Which automatic source the user wants VitQuest to use. Manual entries always win for their day. */
enum class StepSourcePreference { AUTOMATIC, HEALTH_CONNECT, PHONE_SENSOR, MANUAL_ONLY }

/** One stored value for one day from one source. Several sources may exist for the same day. */
data class StepRecord(
    val date: LocalDate,
    val source: StepSource,
    val steps: Int,
    val updatedAt: Instant,
)

/**
 * The single value shown for a day, chosen from one source. Values from
 * different sources are never added together, which prevents double counting.
 * A day without any record has no DailySteps at all, which is different from 0.
 */
data class DailySteps(
    val date: LocalDate,
    val steps: Int,
    val source: StepSource,
)
