package io.github.vitoosh.vitquest.feature.home

import org.junit.Assert.assertEquals
import org.junit.Test

class DayPeriodTest {

    @Test
    fun followsIndonesianTimeOfDayBoundaries() {
        assertEquals(DayPeriod.EVENING, DayPeriod.fromHour(3))
        assertEquals(DayPeriod.MORNING, DayPeriod.fromHour(4))
        assertEquals(DayPeriod.MORNING, DayPeriod.fromHour(10))
        assertEquals(DayPeriod.MIDDAY, DayPeriod.fromHour(11))
        assertEquals(DayPeriod.MIDDAY, DayPeriod.fromHour(14))
        assertEquals(DayPeriod.AFTERNOON, DayPeriod.fromHour(15))
        assertEquals(DayPeriod.AFTERNOON, DayPeriod.fromHour(17))
        assertEquals(DayPeriod.EVENING, DayPeriod.fromHour(18))
        assertEquals(DayPeriod.EVENING, DayPeriod.fromHour(23))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidHours() {
        DayPeriod.fromHour(24)
    }
}
