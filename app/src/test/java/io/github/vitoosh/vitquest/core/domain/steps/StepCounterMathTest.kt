package io.github.vitoosh.vitquest.core.domain.steps

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StepCounterMathTest {

    private val jakarta = ZoneId.of("Asia/Jakarta") // UTC+7, no daylight saving
    private val boot = Instant.parse("2026-10-18T22:00:00Z")

    private fun at(localDateTime: String): Instant = java.time.LocalDateTime.parse(localDateTime).atZone(jakarta).toInstant()

    private fun reading(counter: Long, time: String, bootCount: Int? = 5, bootedAt: Instant = boot) =
        SensorReading(bootCount = bootCount, counter = counter, readAt = at(time), bootedAt = bootedAt)

    private fun checkpoint(counter: Long, time: String, bootCount: Int? = 5) =
        SensorCheckpoint(bootCount = bootCount, counter = counter, recordedAt = at(time))

    @Test
    fun firstReadingOnlySetsBaseline() {
        val update = StepCounterMath.apply(null, reading(12_345, "2026-10-19T08:00:00"), jakarta)
        assertTrue(update.stepsByDate.isEmpty())
        assertEquals(12_345L, update.checkpoint.counter)
    }

    @Test
    fun sameDayDifferenceIsAddedToThatDay() {
        val update = StepCounterMath.apply(
            checkpoint(1_000, "2026-10-19T08:00:00"),
            reading(3_500, "2026-10-19T12:00:00"),
            jakarta,
        )
        assertEquals(mapOf(LocalDate.parse("2026-10-19") to 2_500), update.stepsByDate)
    }

    @Test
    fun noNewStepsProducesNoChange() {
        val update = StepCounterMath.apply(checkpoint(500, "2026-10-19T08:00:00"), reading(500, "2026-10-19T09:00:00"), jakarta)
        assertTrue(update.stepsByDate.isEmpty())
    }

    @Test
    fun rebootDetectedByBootCountCountsStepsSinceBoot() {
        val bootedAt = at("2026-10-19T10:00:00")
        val update = StepCounterMath.apply(
            checkpoint(9_000, "2026-10-19T08:00:00", bootCount = 5),
            reading(800, "2026-10-19T11:00:00", bootCount = 6, bootedAt = bootedAt),
            jakarta,
        )
        assertEquals(mapOf(LocalDate.parse("2026-10-19") to 800), update.stepsByDate)
        assertEquals(6, update.checkpoint.bootCount)
    }

    @Test
    fun rebootDetectedByCounterGoingBackwardsWithoutBootCount() {
        val update = StepCounterMath.apply(
            checkpoint(9_000, "2026-10-19T08:00:00", bootCount = null),
            reading(1_200, "2026-10-19T11:00:00", bootCount = null, bootedAt = at("2026-10-19T10:30:00")),
            jakarta,
        )
        assertEquals(mapOf(LocalDate.parse("2026-10-19") to 1_200), update.stepsByDate)
    }

    @Test
    fun stepsAcrossMidnightAreSplitByTimeAndKeepTheTotal() {
        // 22:00 -> 02:00 is 2 hours before and 2 hours after midnight.
        val update = StepCounterMath.apply(
            checkpoint(10_000, "2026-10-18T22:00:00"),
            reading(11_000, "2026-10-19T02:00:00"),
            jakarta,
        )
        assertEquals(
            mapOf(LocalDate.parse("2026-10-18") to 500, LocalDate.parse("2026-10-19") to 500),
            update.stepsByDate,
        )
    }

    @Test
    fun longGapIsSpreadOverEveryDayWithExactTotal() {
        val update = StepCounterMath.apply(
            checkpoint(0, "2026-10-16T12:00:00"),
            reading(30_001, "2026-10-19T12:00:00"),
            jakarta,
        )
        assertEquals(4, update.stepsByDate.size)
        assertEquals(30_001, update.stepsByDate.values.sum())
        assertEquals(5_000, update.stepsByDate[LocalDate.parse("2026-10-16")])
    }

    @Test
    fun clockMovingBackwardsAttributesStepsToCurrentDay() {
        val update = StepCounterMath.apply(
            checkpoint(100, "2026-10-19T12:00:00"),
            reading(400, "2026-10-19T11:00:00"),
            jakarta,
        )
        assertEquals(mapOf(LocalDate.parse("2026-10-19") to 300), update.stepsByDate)
    }

    @Test
    fun implausibleJumpIsIgnoredButBaselineMoves() {
        val update = StepCounterMath.apply(
            checkpoint(100, "2026-10-19T12:00:00"),
            reading(500_100, "2026-10-19T12:30:00"),
            jakarta,
        )
        assertTrue(update.stepsByDate.isEmpty())
        assertEquals(500_100L, update.checkpoint.counter)
    }

    @Test
    fun splitHandlesDaylightSavingDays() {
        val amsterdam = ZoneId.of("Europe/Amsterdam")
        val start = java.time.LocalDateTime.parse("2026-10-24T12:00:00").atZone(amsterdam).toInstant()
        val end = java.time.LocalDateTime.parse("2026-10-26T12:00:00").atZone(amsterdam).toInstant()
        val split = StepCounterMath.splitAcrossDays(4_900, start, end, amsterdam)
        // 25 October has 25 hours, so it receives the largest share.
        assertEquals(4_900, split.values.sum())
        assertTrue(split.getValue(LocalDate.parse("2026-10-25")) > split.getValue(LocalDate.parse("2026-10-24")))
    }
}
