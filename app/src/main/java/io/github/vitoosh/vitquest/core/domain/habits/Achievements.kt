package io.github.vitoosh.vitquest.core.domain.habits

import io.github.vitoosh.vitquest.core.model.CustomHabit
import io.github.vitoosh.vitquest.core.model.Goals
import io.github.vitoosh.vitquest.core.model.WorkoutSession
import java.time.LocalDate

/**
 * Milestones about consistency, not body weight, so none of them rewards
 * eating less or losing weight quickly.
 */
enum class Achievement(val goal: Int) {
    FIRST_WORKOUT(1),
    WORKOUTS_10(10),
    WORKOUTS_25(25),
    ACTIVE_WEEK(1),
    STEP_GOAL_DAYS_7(7),
    HYDRATION_DAYS_7(7),
    FOOD_LOG_DAYS_7(7),
    HABIT_CHECK_INS_10(10),
}

data class AchievementStatus(val achievement: Achievement, val progress: Int, val earnedOn: LocalDate?) {
    val earned: Boolean get() = earnedOn != null
}

/** Derived from stored records, so correcting a record also corrects the milestones. */
object Achievements {

    private const val ACTIVE_WEEK_DAYS = 3

    fun evaluate(
        records: Collection<DayRecord>,
        sessions: List<WorkoutSession>,
        goals: Goals?,
        customHabits: List<CustomHabit>,
    ): List<AchievementStatus> {
        val sessionDates = sessions.sortedWith(compareBy({ it.date }, { it.startedAt })).map { it.date }
        val sortedRecords = records.sortedBy { it.date }
        val stepDays = goals?.dailyStepGoal?.takeIf { it > 0 }?.let { goal ->
            sortedRecords.filter { (it.steps ?: 0) >= goal }.map { it.date }
        }.orEmpty()
        val waterDays = goals?.dailyWaterTargetMl?.takeIf { it > 0 }?.let { target ->
            sortedRecords.filter { it.waterMl >= target }.map { it.date }
        }.orEmpty()
        val foodDays = sortedRecords.filter { it.foodLogged }.map { it.date }
        val checkIns = customHabits.flatMap { it.checkIns }.sorted()

        return Achievement.entries.map { achievement ->
            val dates = when (achievement) {
                Achievement.FIRST_WORKOUT, Achievement.WORKOUTS_10, Achievement.WORKOUTS_25 -> sessionDates
                Achievement.ACTIVE_WEEK -> activeWeekDates(sessionDates)
                Achievement.STEP_GOAL_DAYS_7 -> stepDays
                Achievement.HYDRATION_DAYS_7 -> waterDays
                Achievement.FOOD_LOG_DAYS_7 -> foodDays
                Achievement.HABIT_CHECK_INS_10 -> checkIns
            }
            AchievementStatus(
                achievement = achievement,
                progress = dates.size.coerceAtMost(achievement.goal),
                earnedOn = dates.getOrNull(achievement.goal - 1),
            )
        }
    }

    /** The day each week first reached three workout days, in order. */
    private fun activeWeekDates(sessionDates: List<LocalDate>): List<LocalDate> =
        sessionDates.distinct()
            .groupBy { HabitStats.weekStart(it) }
            .toSortedMap()
            .values
            .mapNotNull { days -> days.sorted().getOrNull(ACTIVE_WEEK_DAYS - 1) }
}
