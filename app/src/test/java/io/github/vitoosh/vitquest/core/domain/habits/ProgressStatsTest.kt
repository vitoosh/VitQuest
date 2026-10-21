package io.github.vitoosh.vitquest.core.domain.habits

import io.github.vitoosh.vitquest.core.model.Goals
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressStatsTest {

    private val today = LocalDate.of(2026, 10, 21) // Wednesday
    private val monday = LocalDate.of(2026, 10, 19)

    @Test
    fun weekCountsOnlyDaysSoFarAndKeepsMissingStepsSeparateFromZero() {
        val records = mapOf(
            monday to DayRecord(monday, steps = 4_000, waterMl = 2_000, foodLogged = true, workoutCount = 1, workoutSeconds = 600),
            monday.plusDays(1) to DayRecord(monday.plusDays(1), steps = null, waterMl = 500),
            today to DayRecord(today, steps = 6_000, foodLogged = true),
            // A future record (for example a planned entry) must not count yet.
            today.plusDays(1) to DayRecord(today.plusDays(1), steps = 20_000),
        )
        val week = ProgressStats.week(monday, today, records, Goals(dailyStepGoal = 5_000, dailyWaterTargetMl = 2_000, waterServingMl = 250))
        assertEquals(5_000, week.averageSteps)
        assertEquals(2, week.stepDays)
        assertEquals(1, week.workoutDays)
        assertEquals(600, week.workoutSeconds)
        assertEquals(1, week.waterTargetDays)
        assertEquals(2, week.foodLogDays)
        assertNull(ProgressStats.week(monday, today, records, null).waterTargetDays)
    }

    @Test
    fun weeksEndWithTheCurrentWeek() {
        val weeks = ProgressStats.weeks(today, 8, emptyMap(), null)
        assertEquals(8, weeks.size)
        assertEquals(monday, weeks.last().start)
        assertEquals(monday.minusWeeks(7), weeks.first().start)
        assertNull(weeks.last().averageSteps)
    }
}
