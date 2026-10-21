package io.github.vitoosh.vitquest.core.domain.steps

import io.github.vitoosh.vitquest.core.model.DailySteps
import io.github.vitoosh.vitquest.core.model.StepRecord
import io.github.vitoosh.vitquest.core.model.StepSource
import io.github.vitoosh.vitquest.core.model.StepSourcePreference
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StepSourceResolverTest {

    private val day = LocalDate.parse("2026-10-19")
    private fun record(source: StepSource, steps: Int, date: LocalDate = day) = StepRecord(date, source, steps, Instant.EPOCH)

    @Test
    fun overlappingSourcesAreNeverAdded() {
        val records = listOf(record(StepSource.HEALTH_CONNECT, 6_000), record(StepSource.PHONE_SENSOR, 5_800))

        val resolved = StepSourceResolver.resolve(records, StepSourcePreference.AUTOMATIC)

        assertEquals(DailySteps(day, 6_000, StepSource.HEALTH_CONNECT), resolved[day])
    }

    @Test
    fun manualEntryWinsOverDeviceData() {
        val records = listOf(record(StepSource.HEALTH_CONNECT, 6_000), record(StepSource.MANUAL, 7_500))
        assertEquals(StepSource.MANUAL, StepSourceResolver.resolve(records, StepSourcePreference.AUTOMATIC)[day]?.source)
    }

    @Test
    fun automaticFallsBackToSensorPerDay() {
        val yesterday = day.minusDays(1)
        val records = listOf(record(StepSource.HEALTH_CONNECT, 6_000), record(StepSource.PHONE_SENSOR, 3_000, yesterday))

        val resolved = StepSourceResolver.resolve(records, StepSourcePreference.AUTOMATIC)

        assertEquals(StepSource.HEALTH_CONNECT, resolved[day]?.source)
        assertEquals(StepSource.PHONE_SENSOR, resolved[yesterday]?.source)
    }

    @Test
    fun explicitPreferenceIgnoresOtherAutomaticSource() {
        val records = listOf(record(StepSource.HEALTH_CONNECT, 6_000), record(StepSource.PHONE_SENSOR, 5_800))
        assertEquals(
            DailySteps(day, 5_800, StepSource.PHONE_SENSOR),
            StepSourceResolver.resolve(records, StepSourcePreference.PHONE_SENSOR)[day],
        )
        assertTrue(StepSourceResolver.resolve(listOf(record(StepSource.PHONE_SENSOR, 1)), StepSourcePreference.HEALTH_CONNECT).isEmpty())
    }

    @Test
    fun manualOnlyShowsOnlyManualEntries() {
        val records = listOf(record(StepSource.HEALTH_CONNECT, 6_000))
        assertTrue(StepSourceResolver.resolve(records, StepSourcePreference.MANUAL_ONLY).isEmpty())
        assertFalse(StepSourceResolver.usesPhoneSensor(StepSourcePreference.MANUAL_ONLY))
        assertFalse(StepSourceResolver.usesHealthConnect(StepSourcePreference.MANUAL_ONLY))
    }

    @Test
    fun zeroIsKeptAsRealData() {
        val resolved = StepSourceResolver.resolve(listOf(record(StepSource.PHONE_SENSOR, 0)), StepSourcePreference.AUTOMATIC)
        assertEquals(0, resolved[day]?.steps)
    }

    @Test
    fun theLargerPhoneCountWinsBecauseTheBuiltInCounterCanMissSteps() {
        val records = listOf(record(StepSource.PHONE_SENSOR, 265), record(StepSource.MOTION, 4_120))
        assertEquals(DailySteps(day, 4_120, StepSource.MOTION), StepSourceResolver.resolve(records, StepSourcePreference.PHONE_SENSOR)[day])
        assertEquals(DailySteps(day, 4_120, StepSource.MOTION), StepSourceResolver.resolve(records, StepSourcePreference.AUTOMATIC)[day])
        // Health Connect is a tier above, so the phone counts are not mixed with it.
        val withHealthConnect = records + record(StepSource.HEALTH_CONNECT, 3_900)
        assertEquals(StepSource.HEALTH_CONNECT, StepSourceResolver.resolve(withHealthConnect, StepSourcePreference.AUTOMATIC)[day]?.source)
        assertTrue(StepSourceResolver.resolve(records, StepSourcePreference.HEALTH_CONNECT).isEmpty())
    }
}
