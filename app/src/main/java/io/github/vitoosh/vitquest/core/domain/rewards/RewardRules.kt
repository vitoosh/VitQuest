package io.github.vitoosh.vitquest.core.domain.rewards

import io.github.vitoosh.vitquest.core.domain.habits.AchievementStatus
import io.github.vitoosh.vitquest.core.domain.habits.DayRecord
import io.github.vitoosh.vitquest.core.domain.habits.HabitStats
import io.github.vitoosh.vitquest.core.model.CustomHabit
import io.github.vitoosh.vitquest.core.model.Goals
import io.github.vitoosh.vitquest.core.model.RewardReason
import io.github.vitoosh.vitquest.core.model.WeightEntry
import io.github.vitoosh.vitquest.core.model.WorkoutSession
import java.time.LocalDate

/** How much one kind of activity earns, and how often. Shown to the user as-is. */
enum class RewardRule(val reason: RewardReason, val xp: Int, val credits: Int) {
    WORKOUT(RewardReason.WORKOUT, xp = 50, credits = 5),
    STEP_GOAL(RewardReason.STEP_GOAL, xp = 30, credits = 3),
    WATER_GOAL(RewardReason.WATER_GOAL, xp = 20, credits = 2),
    FOOD_LOG(RewardReason.FOOD_LOG, xp = 10, credits = 1),
    WEIGHT_LOG(RewardReason.WEIGHT_LOG, xp = 10, credits = 1),
    HABIT_CHECK_IN(RewardReason.HABIT_CHECK_IN, xp = 5, credits = 0),
    ACHIEVEMENT(RewardReason.ACHIEVEMENT, xp = 100, credits = 10),
}

/** Something that earns XP and/or credits. The key identifies it across syncs. */
data class RewardEvent(
    val key: String,
    val rule: RewardRule,
    val date: LocalDate,
)

/**
 * Turns stored records into earning events. Limits keep it about consistency
 * rather than volume: at most two workouts and three habit check-ins a day,
 * weight once a week, and nothing for eating less or losing weight faster.
 */
object RewardRules {
    /** Activity older than this is never granted or taken back, so old balances stay stable. */
    const val WINDOW_DAYS = 30L
    const val MIN_WORKOUT_SECONDS = 300
    const val WORKOUTS_PER_DAY = 2
    const val HABIT_CHECK_INS_PER_DAY = 3

    fun windowStart(today: LocalDate): LocalDate = today.minusDays(WINDOW_DAYS - 1)

    fun events(
        today: LocalDate,
        records: Map<LocalDate, DayRecord>,
        sessions: List<WorkoutSession>,
        weights: List<WeightEntry>,
        customHabits: List<CustomHabit>,
        goals: Goals?,
        achievements: List<AchievementStatus>,
    ): List<RewardEvent> {
        val start = windowStart(today)
        fun inWindow(date: LocalDate) = !date.isBefore(start) && !date.isAfter(today)
        val events = mutableListOf<RewardEvent>()

        sessions.filter { inWindow(it.date) && it.activeSeconds >= MIN_WORKOUT_SECONDS }
            .groupBy { it.date }
            .forEach { (_, day) ->
                day.sortedWith(compareBy({ it.startedAt }, { it.id })).take(WORKOUTS_PER_DAY).forEach { session ->
                    events += RewardEvent("workout:${session.id}", RewardRule.WORKOUT, session.date)
                }
            }

        val stepGoal = goals?.dailyStepGoal?.takeIf { it > 0 }
        val waterTarget = goals?.dailyWaterTargetMl?.takeIf { it > 0 }
        records.values.filter { inWindow(it.date) }.forEach { record ->
            if (stepGoal != null && (record.steps ?: 0) >= stepGoal) events += RewardEvent("steps:${record.date}", RewardRule.STEP_GOAL, record.date)
            if (waterTarget != null && record.waterMl >= waterTarget) events += RewardEvent("water:${record.date}", RewardRule.WATER_GOAL, record.date)
            if (record.foodLogged) events += RewardEvent("food:${record.date}", RewardRule.FOOD_LOG, record.date)
        }

        weights.filter { inWindow(it.date) }
            .groupBy { HabitStats.weekStart(it.date) }
            .forEach { (week, entries) -> events += RewardEvent("weight:$week", RewardRule.WEIGHT_LOG, entries.minOf { it.date }) }

        customHabits.flatMap { habit -> habit.checkIns.filter(::inWindow).map { date -> habit.id to date } }
            .groupBy { it.second }
            .forEach { (date, checkIns) ->
                checkIns.sortedBy { it.first }.take(HABIT_CHECK_INS_PER_DAY).forEach { (habitId, _) ->
                    events += RewardEvent("habit:$habitId:$date", RewardRule.HABIT_CHECK_IN, date)
                }
            }

        achievements.forEach { status ->
            status.earnedOn?.let { events += RewardEvent("achievement:${status.achievement.name}", RewardRule.ACHIEVEMENT, it) }
        }
        return events
    }
}

/** XP needed to go from [level] to the next one: 100, 150, 200, ... */
object Leveling {
    fun xpForLevel(level: Int): Int = 100 + 50 * (level - 1)

    fun progress(totalXp: Int): LevelProgress {
        var level = 1
        var remaining = totalXp.coerceAtLeast(0)
        while (remaining >= xpForLevel(level)) {
            remaining -= xpForLevel(level)
            level++
        }
        return LevelProgress(level, remaining, xpForLevel(level), totalXp)
    }
}

data class LevelProgress(val level: Int, val xpIntoLevel: Int, val xpForNext: Int, val totalXp: Int) {
    val fraction: Float get() = xpIntoLevel.toFloat() / xpForNext
}
