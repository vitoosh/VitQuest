package io.github.vitoosh.vitquest.core.domain.weight

import io.github.vitoosh.vitquest.core.model.WeightEntry
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightStatsTest {

    // Monday 19 October 2026.
    private val today = LocalDate.parse("2026-10-19")
    private fun w(daysAgo: Long, kg: Double) = WeightEntry(daysAgo, today.minusDays(daysAgo), kg, null)

    @Test
    fun emptyHistoryHasNoNumbers() {
        val summary = WeightStats.summarize(emptyList(), 70.0, today)
        assertNull(summary.latest)
        assertNull(summary.weeklyPaceKg)
        assertNull(summary.progressToTarget)
        assertFalse(summary.fastLossWarning)
    }

    @Test
    fun summarizesTrendAndProgress() {
        val entries = listOf(w(35, 84.0), w(28, 83.0), w(14, 82.0), w(7, 81.5), w(1, 81.0), w(0, 80.8))

        val summary = WeightStats.summarize(entries, targetKg = 76.0, today = today)

        assertEquals(80.8, summary.latest!!.weightKg, 0.001)
        assertEquals(-3.2, summary.totalChangeKg!!, 0.001)
        assertEquals(80.8, summary.thisWeekAverageKg!!, 0.001) // only Monday so far this week
        assertEquals(81.25, summary.lastWeekAverageKg!!, 0.001) // Monday 12 (81.5) and Sunday 18 (81.0)
        // 30 days ago: the closest earlier entry is 35 days ago (84.0).
        assertEquals(-3.2, summary.last30DaysChangeKg!!, 0.001)
        // Last 28 days: 83.0 -> 80.8 over 28 days = -0.55 kg per week.
        assertEquals(-0.55, summary.weeklyPaceKg!!, 0.001)
        assertEquals(0.4f, summary.progressToTarget!!, 0.001f) // 3.2 of 8 kg
        assertFalse(summary.fastLossWarning)
    }

    @Test
    fun warnsAboutVeryFastLoss() {
        val summary = WeightStats.summarize(listOf(w(14, 90.0), w(0, 86.0)), targetKg = 80.0, today = today)
        // 2 kg per week is more than 1% of 86 kg.
        assertTrue(summary.fastLossWarning)
    }

    @Test
    fun paceNeedsAtLeastAWeekOfData() {
        assertNull(WeightStats.summarize(listOf(w(3, 80.0), w(0, 79.5)), null, today).weeklyPaceKg)
    }
}
