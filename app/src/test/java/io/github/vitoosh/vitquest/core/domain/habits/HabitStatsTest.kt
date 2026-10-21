package io.github.vitoosh.vitquest.core.domain.habits

import io.github.vitoosh.vitquest.core.model.BuiltInHabit
import io.github.vitoosh.vitquest.core.model.CustomHabit
import io.github.vitoosh.vitquest.core.model.Goals
import io.github.vitoosh.vitquest.core.model.WorkoutSession
import io.github.vitoosh.vitquest.core.model.WorkoutSource
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitStatsTest {

    // A Wednesday.
    private val today = LocalDate.of(2026, 10, 21)
    private val goals = Goals(dailyStepGoal = 6_000, dailyWaterTargetMl = 2_000, waterServingMl = 250)

    @Test
    fun builtInHabitsFollowTheTrackersAndGoals() {
        val record = DayRecord(today, steps = 6_500, waterMl = 1_500, foodLogged = true, workoutCount = 0)
        assertEquals(true, HabitStats.isDone(BuiltInHabit.MOVEMENT, record, goals))
        assertEquals(false, HabitStats.isDone(BuiltInHabit.HYDRATION, record, goals))
        assertEquals(true, HabitStats.isDone(BuiltInHabit.FOOD_LOG, record, goals))
        assertEquals(false, HabitStats.isDone(BuiltInHabit.WORKOUT, record, goals))
        // Without a goal there is nothing to judge.
        assertNull(HabitStats.isDone(BuiltInHabit.MOVEMENT, record, null))
        assertEquals(false, HabitStats.isDone(BuiltInHabit.FOOD_LOG, null, goals))
    }

    @Test
    fun weekRunsMondayToSundayAndCountsOnlyDoneDays() {
        val done = setOf(today.minusDays(2), today)
        val week = HabitStats.week(today, targetDays = 3) { it in done }
        assertEquals(LocalDate.of(2026, 10, 19), week.days.first().first)
        assertEquals(DayStatus.DONE, week.days[0].second)
        assertEquals(DayStatus.NOT_DONE, week.days[1].second)
        assertEquals(DayStatus.FUTURE, week.days[3].second)
        assertEquals(2, week.doneCount)
        assertFalse(week.targetMet)
    }

    @Test
    fun weeksOnTargetLooksAtFullPastWeeksOnly() {
        // Three days in each of the two previous weeks, none this week.
        val done = (1..2).flatMap { back ->
            val start = HabitStats.weekStart(today).minusWeeks(back.toLong())
            listOf(start, start.plusDays(2), start.plusDays(4))
        }.toSet()
        assertEquals(2, HabitStats.weeksOnTarget(today, targetDays = 3) { it in done })
        assertEquals(0, HabitStats.weeksOnTarget(today, targetDays = 4) { it in done })
    }

    private fun session(date: LocalDate) = WorkoutSession(
        id = date.toEpochDay(), date = date, startedAt = Instant.EPOCH, endedAt = Instant.EPOCH, source = WorkoutSource.MANUAL,
        focus = null, level = null, location = null, intensity = null, plannedSeconds = 600, activeSeconds = 600,
        exercisesCompleted = 0, exercisesTotal = 0, completed = true, estimatedKcal = null, note = null,
    )

    @Test
    fun achievementsComeFromRecordsWithDates() {
        val monday = HabitStats.weekStart(today)
        val sessions = listOf(monday, monday.plusDays(1), monday.plusDays(2)).map(::session)
        val records = (0L until 8L).map { DayRecord(today.minusDays(it), steps = 7_000, waterMl = 500, foodLogged = it < 3) }
        val habit = CustomHabit(1, "Tidur cukup", 5, setOf(today))

        val result = Achievements.evaluate(records, sessions, goals, listOf(habit)).associateBy { it.achievement }

        assertEquals(monday, result.getValue(Achievement.FIRST_WORKOUT).earnedOn)
        assertEquals(3, result.getValue(Achievement.WORKOUTS_10).progress)
        assertFalse(result.getValue(Achievement.WORKOUTS_10).earned)
        assertEquals(monday.plusDays(2), result.getValue(Achievement.ACTIVE_WEEK).earnedOn)
        // Seventh day reaching the step goal, counted from the oldest.
        assertEquals(today.minusDays(1), result.getValue(Achievement.STEP_GOAL_DAYS_7).earnedOn)
        assertEquals(0, result.getValue(Achievement.HYDRATION_DAYS_7).progress)
        assertEquals(3, result.getValue(Achievement.FOOD_LOG_DAYS_7).progress)
        assertEquals(1, result.getValue(Achievement.HABIT_CHECK_INS_10).progress)
    }
}
