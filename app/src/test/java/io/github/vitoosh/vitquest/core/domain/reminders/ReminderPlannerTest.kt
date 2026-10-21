package io.github.vitoosh.vitquest.core.domain.reminders

import io.github.vitoosh.vitquest.core.model.ReminderSetting
import io.github.vitoosh.vitquest.core.model.ReminderType
import io.github.vitoosh.vitquest.core.model.ReminderWindow
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderPlannerTest {

    private val zone = ZoneId.of("Asia/Jakarta")
    // Wednesday 21 October 2026.
    private fun at(day: Int, hour: Int, minute: Int = 0) = ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, zone)
    private val window = ReminderWindow(activeStart = LocalTime.of(8, 0), activeEnd = LocalTime.of(20, 0))
    private val water = ReminderSetting(ReminderType.WATER, enabled = true)

    @Test
    fun intervalRemindersStayInsideActiveHours() {
        assertEquals(at(21, 10, 30), ReminderPlanner.next(water, window, at(21, 10, 0)))
        // 19:45 + 30 min is past 20:00, so the next one is tomorrow at 08:00.
        assertEquals(at(22, 8, 0), ReminderPlanner.next(water, window, at(21, 19, 45)))
        assertEquals(at(21, 8, 0), ReminderPlanner.next(water, window, at(21, 3, 0)))
    }

    @Test
    fun quietHoursAndReachedGoalsDelayTheReminder() {
        val lunch = window.copy(quietStart = LocalTime.of(12, 0), quietEnd = LocalTime.of(13, 0))
        assertEquals(at(21, 13, 0), ReminderPlanner.next(water, lunch, at(21, 11, 45)))
        // Goal already reached today: resume tomorrow morning.
        assertEquals(at(22, 8, 0), ReminderPlanner.next(water, window, at(21, 10, 0), skipRestOfToday = true))
    }

    @Test
    fun activeHoursMayCrossMidnight() {
        val night = ReminderWindow(activeStart = LocalTime.of(22, 0), activeEnd = LocalTime.of(6, 0))
        assertEquals(at(22, 0, 30), ReminderPlanner.next(water, night, at(22, 0, 0)))
        assertEquals(at(21, 22, 0), ReminderPlanner.next(water, night, at(21, 6, 0)))
    }

    @Test
    fun timedRemindersUseTheChosenTimeAndDays() {
        val workout = ReminderSetting(ReminderType.WORKOUT, enabled = true, time = LocalTime.of(18, 0), days = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY))
        assertEquals(at(22, 18, 0), ReminderPlanner.next(workout, ReminderWindow(), at(21, 9, 0)))
        // Already past Thursday 18:00: next Monday.
        assertEquals(at(26, 18, 0), ReminderPlanner.next(workout, ReminderWindow(), at(22, 18, 0)))
        val food = ReminderSetting(ReminderType.FOOD_LOG, enabled = true, time = LocalTime.of(20, 0))
        assertEquals(at(21, 20, 0), ReminderPlanner.next(food, ReminderWindow(), at(21, 19, 59)))
        assertEquals(at(22, 20, 0), ReminderPlanner.next(food, ReminderWindow(), at(21, 12, 0), skipRestOfToday = true))
        // Quiet hours push it later the same day.
        val quiet = ReminderWindow(quietStart = LocalTime.of(19, 30), quietEnd = LocalTime.of(20, 30))
        assertEquals(at(21, 20, 30), ReminderPlanner.next(food, quiet, at(21, 12, 0)))
    }

    @Test
    fun incompleteOrDisabledRemindersAreNotScheduled() {
        assertEquals(ReminderProblem.NEEDS_ACTIVE_HOURS, ReminderPlanner.problem(water, ReminderWindow()))
        assertNull(ReminderPlanner.next(water, ReminderWindow(), at(21, 10)))
        val weight = ReminderSetting(ReminderType.WEIGHT, enabled = true)
        assertEquals(ReminderProblem.NEEDS_TIME, ReminderPlanner.problem(weight, window))
        assertEquals(ReminderProblem.NEEDS_DAYS, ReminderPlanner.problem(weight.copy(time = LocalTime.NOON, days = emptySet()), window))
        assertNull(ReminderPlanner.next(water.copy(enabled = false), window, at(21, 10)))
    }
}
