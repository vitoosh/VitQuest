package io.github.vitoosh.vitquest.core.domain.steps

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Last processed value of the phone's cumulative step counter. */
data class SensorCheckpoint(
    /** Settings.Global.BOOT_COUNT when the value was read, if the device exposes it. */
    val bootCount: Int?,
    /** Steps counted by the hardware since the last reboot. */
    val counter: Long,
    val recordedAt: Instant,
)

data class SensorReading(
    val bootCount: Int?,
    val counter: Long,
    val readAt: Instant,
    /** Wall-clock time of the last device boot (now minus elapsed realtime). */
    val bootedAt: Instant,
)

data class SensorUpdate(
    val checkpoint: SensorCheckpoint,
    /** New steps to add to each local day. Empty when nothing can be attributed. */
    val stepsByDate: Map<LocalDate, Int>,
)

/**
 * Turns readings of Android's cumulative TYPE_STEP_COUNTER into per-day steps.
 *
 * The hardware keeps counting while the app is closed, so the difference
 * between two readings is exact. When that interval crosses midnight the
 * steps are split in proportion to the time spent in each day, because the
 * sensor does not say when the steps happened.
 */
object StepCounterMath {

    /** Well above a sustained running cadence; larger jumps are treated as sensor glitches. */
    const val MAX_STEPS_PER_HOUR = 20_000L

    fun apply(previous: SensorCheckpoint?, reading: SensorReading, zone: ZoneId): SensorUpdate {
        val checkpoint = SensorCheckpoint(reading.bootCount, reading.counter, reading.readAt)
        // The first reading only sets the baseline: earlier steps have no known date.
        if (previous == null || reading.counter < 0) return SensorUpdate(checkpoint, emptyMap())

        val rebooted = (previous.bootCount != null && reading.bootCount != null && previous.bootCount != reading.bootCount) ||
            reading.counter < previous.counter

        val steps: Long
        val start: Instant
        if (rebooted) {
            // The counter restarted at zero; steps between the last reading and the shutdown are lost.
            steps = reading.counter
            start = maxOf(previous.recordedAt, reading.bootedAt)
        } else {
            steps = reading.counter - previous.counter
            start = previous.recordedAt
        }
        val end = reading.readAt
        val safeStart = if (start.isAfter(end)) end else start // device clock moved backwards

        if (steps <= 0L || isImplausible(steps, safeStart, end)) return SensorUpdate(checkpoint, emptyMap())
        return SensorUpdate(checkpoint, splitAcrossDays(steps, safeStart, end, zone))
    }

    private fun isImplausible(steps: Long, start: Instant, end: Instant): Boolean {
        val hours = Duration.between(start, end).toMinutes() / 60.0
        return steps > MAX_STEPS_PER_HOUR * maxOf(hours, 1.0)
    }

    /** Splits [steps] over the local days touched by [start, end], keeping the exact total. */
    fun splitAcrossDays(steps: Long, start: Instant, end: Instant, zone: ZoneId): Map<LocalDate, Int> {
        val endDate = end.atZone(zone).toLocalDate()
        val totalMillis = Duration.between(start, end).toMillis()
        if (totalMillis <= 0L) return mapOf(endDate to steps.toInt())

        val shares = linkedMapOf<LocalDate, Long>()
        var segmentStart = start
        var date = start.atZone(zone).toLocalDate()
        while (!date.isAfter(endDate)) {
            val nextMidnight = date.plusDays(1).atStartOfDay(zone).toInstant()
            val segmentEnd = if (nextMidnight.isBefore(end)) nextMidnight else end
            val millis = Duration.between(segmentStart, segmentEnd).toMillis()
            if (millis > 0) shares[date] = steps * millis / totalMillis
            segmentStart = segmentEnd
            date = date.plusDays(1)
        }
        // Rounding remainder goes to the most recent day so the total stays exact.
        val remainder = steps - shares.values.sum()
        shares[endDate] = (shares[endDate] ?: 0L) + remainder
        return shares.filterValues { it > 0 }.mapValues { it.value.toInt() }
    }
}
