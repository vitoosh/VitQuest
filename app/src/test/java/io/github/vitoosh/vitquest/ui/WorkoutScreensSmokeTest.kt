package io.github.vitoosh.vitquest.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.theme.VitQuestTheme
import io.github.vitoosh.vitquest.core.domain.habits.Achievements
import io.github.vitoosh.vitquest.core.domain.habits.DayRecord
import io.github.vitoosh.vitquest.core.domain.habits.ProgressStats
import io.github.vitoosh.vitquest.core.domain.workout.ExerciseCatalog
import io.github.vitoosh.vitquest.core.domain.workout.PlayerPhase
import io.github.vitoosh.vitquest.core.domain.workout.WorkoutEnergy
import io.github.vitoosh.vitquest.core.domain.workout.WorkoutGenerator
import io.github.vitoosh.vitquest.core.domain.workout.WorkoutPlayerEngine
import io.github.vitoosh.vitquest.core.model.CustomHabit
import io.github.vitoosh.vitquest.core.model.FitnessExperience
import io.github.vitoosh.vitquest.core.model.Goals
import io.github.vitoosh.vitquest.core.model.StepStatus
import io.github.vitoosh.vitquest.core.model.WorkoutFocus
import io.github.vitoosh.vitquest.core.model.WorkoutLocation
import io.github.vitoosh.vitquest.core.model.WorkoutRequest
import io.github.vitoosh.vitquest.core.model.WorkoutSession
import io.github.vitoosh.vitquest.core.model.WorkoutSessionExercise
import io.github.vitoosh.vitquest.core.model.WorkoutSource
import io.github.vitoosh.vitquest.feature.habits.HabitActions
import io.github.vitoosh.vitquest.feature.habits.HabitsOverview
import io.github.vitoosh.vitquest.feature.habits.HabitsScreen
import io.github.vitoosh.vitquest.feature.progress.ProgressScreen
import io.github.vitoosh.vitquest.feature.progress.ProgressUiState
import io.github.vitoosh.vitquest.feature.workouts.generator.GeneratorActions
import io.github.vitoosh.vitquest.feature.workouts.generator.GeneratorContent
import io.github.vitoosh.vitquest.feature.workouts.generator.GeneratorUiState
import io.github.vitoosh.vitquest.feature.workouts.history.HistoryContent
import io.github.vitoosh.vitquest.feature.workouts.history.HistoryUiState
import io.github.vitoosh.vitquest.feature.workouts.library.ExerciseDetailScreen
import io.github.vitoosh.vitquest.feature.workouts.player.FinishedWorkout
import io.github.vitoosh.vitquest.feature.workouts.player.PlayerActions
import io.github.vitoosh.vitquest.feature.workouts.player.WorkoutPlayerScreen
import io.github.vitoosh.vitquest.testing.assertAccessible
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en")
class WorkoutScreensSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.parse("2026-10-21")

    private fun string(id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private val request = WorkoutRequest(10, FitnessExperience.BEGINNER, WorkoutLocation.HOME, emptySet(), WorkoutFocus.FULL_BODY, seed = 3)
    private val plan = WorkoutGenerator.generate(request)

    @Test
    fun generatorShowsThePlanAndStartsIt() {
        var started: WorkoutRequest? = null
        var shuffled = false
        composeRule.setContent {
            VitQuestTheme {
                GeneratorContent(
                    uiState = GeneratorUiState(request, plan, todaySessions = 0, todayActiveSeconds = 0, weekWorkoutDays = 1, estimatedKcal = 42.0),
                    actions = GeneratorActions(start = { started = it }, shuffle = { shuffled = true }),
                )
            }
        }
        composeRule.onNodeWithText(string(R.string.workout_today_none)).assertIsDisplayed()
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(string(R.string.generator_start)))
        composeRule.onNodeWithText(string(R.string.generator_shuffle)).performClick()
        composeRule.onNodeWithText(string(R.string.generator_start)).performClick()
        assertEquals(request, started)
        assertTrue(shuffled)
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(string(R.string.block_cool_down)))
    }

    @Test
    fun exerciseDetailHasAnimatedTutorialAndGuidance() {
        val squat = requireNotNull(ExerciseCatalog.find("bodyweight_squat"))
        composeRule.setContent { VitQuestTheme { ExerciseDetailScreen(exercise = squat, onNavigateBack = {}) } }
        composeRule.assertAccessible()
        composeRule.onNodeWithContentDescription(string(R.string.exercise_animation_description, squat.name.en)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.exercise_end_pose)).performClick()
        composeRule.onNodeWithContentDescription(string(R.string.exercise_end_pose_description, squat.name.en)).assertIsDisplayed()
        composeRule.onNodeWithText(squat.steps.first().en).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(squat.modifications.first().en, substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(squat.safety.first().en, substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun walkingHasNoAnimationButStillHasGuidance() {
        val walk = requireNotNull(ExerciseCatalog.find("brisk_walk"))
        composeRule.setContent { VitQuestTheme { ExerciseDetailScreen(exercise = walk, onNavigateBack = {}) } }
        composeRule.onNodeWithText(string(R.string.exercise_no_animation)).assertIsDisplayed()
    }

    @Test
    fun playerShowsTheCurrentExerciseAndControls() {
        var paused = false
        var completed = false
        val working = WorkoutPlayerEngine.tick(WorkoutPlayerEngine.start(plan), plan, WorkoutPlayerEngine.READY_MS)
        // Move to the first rep-based exercise so the "done" button applies.
        var state = working
        while (plan.steps[state.index].exercise.dose !is io.github.vitoosh.vitquest.core.model.ExerciseDose.Reps) state = WorkoutPlayerEngine.skip(WorkoutPlayerEngine.skip(state, plan), plan)
        assertEquals(PlayerPhase.WORK, state.phase)
        val exercise = plan.steps[state.index].exercise
        composeRule.setContent {
            VitQuestTheme {
                WorkoutPlayerScreen(plan, state, finished = null, actions = PlayerActions(togglePause = { paused = true }, complete = { completed = true }), onDone = {})
            }
        }
        composeRule.assertAccessible()
        composeRule.onNodeWithText(exercise.name.en).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.player_pause)).performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.player_complete_set)).performScrollTo().performClick()
        assertTrue(paused)
        assertTrue(completed)
    }

    @Test
    fun playerSummaryReportsEstimateAndSaving() {
        val state = WorkoutPlayerEngine.finishEarly(WorkoutPlayerEngine.tick(WorkoutPlayerEngine.start(plan), plan, WorkoutPlayerEngine.READY_MS + 20_000))
        val summary = WorkoutEnergy.summarize(plan, state, weightKg = 70.0)
        composeRule.setContent {
            VitQuestTheme { WorkoutPlayerScreen(plan, state, FinishedWorkout(summary, saved = true), PlayerActions(), onDone = {}) }
        }
        composeRule.onNodeWithText(string(R.string.player_summary_ended)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.summary_saved)).performScrollTo().assertIsDisplayed()
    }

    private fun session(id: Long, completed: Boolean) = WorkoutSession(
        id = id, date = today, startedAt = Instant.EPOCH, endedAt = Instant.EPOCH, source = WorkoutSource.PLAYER,
        focus = WorkoutFocus.FULL_BODY, level = FitnessExperience.BEGINNER, location = WorkoutLocation.HOME, intensity = null,
        plannedSeconds = 600, activeSeconds = 420, exercisesCompleted = 7, exercisesTotal = 10, completed = completed,
        estimatedKcal = 35.0, note = null,
        exercises = listOf(WorkoutSessionExercise("plank", io.github.vitoosh.vitquest.core.model.Bilingual("Plank", "Plank"), 30, 30, StepStatus.COMPLETED)),
    )

    @Test
    fun historyListsSessionsAndOpensManualLog() {
        composeRule.setContent {
            VitQuestTheme {
                HistoryContent(HistoryUiState(today, listOf(session(1, completed = false)), 1, 420), onLogManual = { _, _, _, _ -> null }, onDelete = {})
            }
        }
        composeRule.onNodeWithText(string(R.string.history_ended_early)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.history_show_exercises)).performClick()
        composeRule.onNodeWithText("Plank").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.history_log_manual)).performClick()
        composeRule.onNodeWithText(string(R.string.history_intensity)).assertIsDisplayed()
    }

    @Test
    fun habitsShowWeeksAndToggleCustomDays() {
        val goals = Goals(dailyStepGoal = 6_000, dailyWaterTargetMl = 2_000, waterServingMl = 250)
        val records = mapOf(today to DayRecord(today, steps = 7_000, waterMl = 500, foodLogged = true))
        val habit = CustomHabit(5, "Sleep 7 hours", 5, setOf(today))
        val state = HabitsOverview.build(today, records, goals, listOf(habit))
        var toggled: LocalDate? = null
        composeRule.setContent {
            VitQuestTheme { HabitsScreen(state, HabitActions(toggle = { _, date -> toggled = date }), onNavigateBack = {}) }
        }
        composeRule.assertAccessible()
        composeRule.onNodeWithText(string(R.string.habits_today, "3", "5")).assertIsDisplayed()
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Sleep 7 hours"))
        assertEquals(3, state.todayDone)
        // Tapping yesterday on the custom habit corrects its record.
        composeRule.onNode(hasContentDescription(string(R.string.day_yesterday) + ": " + string(R.string.habit_day_not_done)) and isToggleable()).performClick()
        assertEquals(today.minusDays(1), toggled)
    }

    @Test
    fun progressShowsComparisonAndAchievements() {
        val records = mapOf(today to DayRecord(today, steps = 7_000, workoutCount = 1, workoutSeconds = 900))
        val state = ProgressUiState(
            today = today,
            weeks = ProgressStats.weeks(today, 8, records, null),
            weights = emptyList(),
            targetWeightKg = null,
            achievements = Achievements.evaluate(records.values, listOf(session(1, completed = true)), null, emptyList()),
        )
        composeRule.setContent { VitQuestTheme { ProgressScreen(state, onNavigateBack = {}, onOpenWeight = {}) } }
        composeRule.assertAccessible()
        composeRule.onNodeWithText(string(R.string.progress_this_week)).assertIsDisplayed()
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(string(R.string.achievement_first_workout)))
        composeRule.onNodeWithText(string(R.string.achievement_first_workout)).assertIsDisplayed()
    }
}
