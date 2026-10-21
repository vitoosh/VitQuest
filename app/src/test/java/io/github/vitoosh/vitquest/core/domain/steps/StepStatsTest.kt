package io.github.vitoosh.vitquest.core.domain.steps

import io.github.vitoosh.vitquest.core.model.DailySteps
import io.github.vitoosh.vitquest.core.model.StepSource
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StepStatsTest {

    private val today = LocalDate.parse("2026-10-19")

    @Test
    fun daysFillsGapsWithMissingData() {
        val from = today.minusDays(6)
        val data = mapOf(today to DailySteps(today, 4_000, StepSource.PHONE_SENSOR))

        val days = StepStats.days(from, today, data)

        assertEquals(7, days.size)
        assertEquals(from, days.first().date)
        assertNull(days.first().steps)
        assertEquals(4_000, days.last().steps?.steps)
    }

    @Test
    fun summaryIgnoresDaysWithoutData() {
        val days = listOf(
            StepDay(today.minusDays(2), DailySteps(today.minusDays(2), 9_000, StepSource.MANUAL)),
            StepDay(today.minusDays(1), null),
            StepDay(today, DailySteps(today, 5_000, StepSource.PHONE_SENSOR)),
        )

        val summary = StepStats.summarize(days, goal = 8_000)

        assertEquals(14_000L, summary.totalSteps)
        assertEquals(7_000, summary.averageSteps)
        assertEquals(2, summary.daysWithData)
        assertEquals(1, summary.daysGoalMet)
        assertEquals(3, summary.dayCount)
    }

    @Test
    fun summaryWithoutDataHasNoAverage() {
        assertNull(StepStats.summarize(listOf(StepDay(today, null)), goal = 8_000).averageSteps)
    }

    @Test
    fun progressIsClamped() {
        assertEquals(0.5f, StepStats.progress(4_000, 8_000))
        assertEquals(1f, StepStats.progress(12_000, 8_000))
        assertEquals(0f, StepStats.progress(100, 0))
    }
}
