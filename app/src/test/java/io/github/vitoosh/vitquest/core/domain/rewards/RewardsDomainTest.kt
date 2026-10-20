package io.github.vitoosh.vitquest.core.domain.rewards

import io.github.vitoosh.vitquest.core.domain.habits.Achievement
import io.github.vitoosh.vitquest.core.domain.habits.AchievementStatus
import io.github.vitoosh.vitquest.core.domain.habits.DayRecord
import io.github.vitoosh.vitquest.core.model.CustomHabit
import io.github.vitoosh.vitquest.core.model.Goals
import io.github.vitoosh.vitquest.core.model.RewardCurrency
import io.github.vitoosh.vitquest.core.model.RewardReason
import io.github.vitoosh.vitquest.core.model.WeightEntry
import io.github.vitoosh.vitquest.core.model.WorkoutSession
import io.github.vitoosh.vitquest.core.model.WorkoutSource
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardsDomainTest {

    private val today = LocalDate.of(2026, 10, 21)
    private val goals = Goals(dailyStepGoal = 6_000, dailyWaterTargetMl = 2_000, waterServingMl = 250)

    private fun session(id: Long, date: LocalDate, seconds: Int = 600, startedAt: Long = id) = WorkoutSession(
        id = id, date = date, startedAt = Instant.ofEpochSecond(startedAt), endedAt = Instant.EPOCH, source = WorkoutSource.MANUAL,
        focus = null, level = null, location = null, intensity = null, plannedSeconds = seconds, activeSeconds = seconds,
        exercisesCompleted = 0, exercisesTotal = 0, completed = true, estimatedKcal = null, note = null,
    )

    private fun events(
        records: Map<LocalDate, DayRecord> = emptyMap(),
        sessions: List<WorkoutSession> = emptyList(),
        weights: List<WeightEntry> = emptyList(),
        habits: List<CustomHabit> = emptyList(),
        achievements: List<AchievementStatus> = emptyList(),
    ) = RewardRules.events(today, records, sessions, weights, habits, goals, achievements)

    @Test
    fun levelsNeedGraduallyMoreXp() {
        assertEquals(LevelProgress(1, 0, 100, 0), Leveling.progress(0))
        assertEquals(LevelProgress(1, 99, 100, 99), Leveling.progress(99))
        assertEquals(LevelProgress(2, 0, 150, 100), Leveling.progress(100))
        assertEquals(LevelProgress(3, 10, 200, 260), Leveling.progress(260))
        assertEquals(1, Leveling.progress(-50).level)
    }

    @Test
    fun workoutsNeedFiveMinutesAndCountTwiceADayAtMost() {
        val result = events(
            sessions = listOf(
                session(1, today), session(2, today), session(3, today),
                session(4, today.minusDays(1), seconds = 240),
                session(5, today.minusDays(40)),
            ),
        )
        assertEquals(listOf("workout:1", "workout:2"), result.filter { it.rule == RewardRule.WORKOUT }.map { it.key })
    }

    @Test
    fun dailyGoalsWeeklyWeightAndCappedHabitCheckIns() {
        val records = mapOf(
            today to DayRecord(today, steps = 6_000, waterMl = 1_999, foodLogged = true),
            today.minusDays(1) to DayRecord(today.minusDays(1), steps = 5_999, waterMl = 2_000),
        )
        val weights = listOf(WeightEntry(1, today, 80.0, null), WeightEntry(2, today.minusDays(1), 80.2, null))
        val habits = (1L..4L).map { CustomHabit(it, "h$it", 5, setOf(today)) }
        val achievements = listOf(AchievementStatus(Achievement.FIRST_WORKOUT, 1, today.minusDays(100)), AchievementStatus(Achievement.WORKOUTS_10, 3, null))

        val keys = events(records, weights = weights, habits = habits, achievements = achievements).map { it.key }.toSet()

        assertTrue("steps:$today" in keys)
        assertTrue("steps:${today.minusDays(1)}" !in keys)
        assertTrue("water:${today.minusDays(1)}" in keys)
        assertTrue("water:$today" !in keys)
        assertTrue("food:$today" in keys)
        // Both weigh-ins are in the same Monday-start week.
        assertEquals(1, keys.count { it.startsWith("weight:") })
        assertEquals(3, keys.count { it.startsWith("habit:") })
        // Achievements count whenever they were earned; unearned ones do not.
        assertTrue("achievement:FIRST_WORKOUT" in keys)
        assertTrue("achievement:WORKOUTS_10" !in keys)
    }

    private fun apply(existing: List<LedgerLine>, events: List<RewardEvent>) =
        existing + LedgerReconciler.reconcile(events, existing, RewardRules.windowStart(today))

    private fun balance(lines: List<LedgerLine>, currency: RewardCurrency) = lines.filter { it.currency == currency }.sumOf { it.amount }

    @Test
    fun reconcilingIsIdempotentAndCorrectionsAppendLines() {
        val workout = RewardEvent("workout:7", RewardRule.WORKOUT, today)
        val first = apply(emptyList(), listOf(workout))
        assertEquals(50, balance(first, RewardCurrency.XP))
        assertEquals(5, balance(first, RewardCurrency.CREDIT))
        // Running again changes nothing.
        assertEquals(first, apply(first, listOf(workout)))

        // The workout is deleted: a reversal is appended, nothing is edited.
        val reversed = apply(first, emptyList())
        assertEquals(first, reversed.take(first.size))
        assertEquals(0, balance(reversed, RewardCurrency.XP))
        assertEquals(apply(reversed, emptyList()), reversed)

        // Logged again: a second grant.
        val again = apply(reversed, listOf(workout))
        assertEquals(50, balance(again, RewardCurrency.XP))
        assertTrue(again.any { it.sourceKey == "g:workout:7#2" })
        assertEquals(again.size, again.map { it.currency to it.sourceKey }.toSet().size)
    }

    @Test
    fun habitCheckInsEarnOnlyXpAndOldActivityIsNeverTakenBack() {
        val habit = apply(emptyList(), listOf(RewardEvent("habit:1:$today", RewardRule.HABIT_CHECK_IN, today)))
        assertEquals(listOf(RewardCurrency.XP), habit.map { it.currency })

        val old = listOf(LedgerLine(RewardCurrency.XP, 30, "g:steps:${today.minusDays(60)}#1", RewardReason.STEP_GOAL, today.minusDays(60)))
        assertEquals(old, apply(old, emptyList()))
        // Redemption lines are not the reconciler's business.
        val redemption = listOf(LedgerLine(RewardCurrency.CREDIT, -100, "redeem:abc", RewardReason.REDEMPTION, today))
        assertEquals(redemption, apply(redemption, emptyList()))
    }
}
