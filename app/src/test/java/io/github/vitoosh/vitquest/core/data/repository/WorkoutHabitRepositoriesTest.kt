package io.github.vitoosh.vitquest.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.core.data.local.FoodLogEntity
import io.github.vitoosh.vitquest.core.data.local.VitQuestDatabase
import io.github.vitoosh.vitquest.core.domain.workout.WorkoutGenerator
import io.github.vitoosh.vitquest.core.domain.workout.WorkoutPlayerEngine
import io.github.vitoosh.vitquest.core.model.FitnessExperience
import io.github.vitoosh.vitquest.core.model.StepSource
import io.github.vitoosh.vitquest.core.model.StepStatus
import io.github.vitoosh.vitquest.core.model.WorkoutFocus
import io.github.vitoosh.vitquest.core.model.WorkoutIntensity
import io.github.vitoosh.vitquest.core.model.WorkoutLocation
import io.github.vitoosh.vitquest.core.model.WorkoutRequest
import io.github.vitoosh.vitquest.core.model.WorkoutSource
import io.github.vitoosh.vitquest.testing.FakeHealthConnectStepsSource
import io.github.vitoosh.vitquest.testing.FakeStepSensorSource
import io.github.vitoosh.vitquest.testing.FakeUserPreferencesRepository
import io.github.vitoosh.vitquest.testing.FixedTimeSource
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class WorkoutHabitRepositoriesTest {

    private lateinit var database: VitQuestDatabase
    private lateinit var workouts: OfflineWorkoutRepository
    private lateinit var habits: OfflineHabitRepository
    private lateinit var steps: OfflineFirstStepsRepository
    // 21 October 2026, 07:00 in Jakarta.
    private val time = FixedTimeSource(Instant.parse("2026-10-21T00:00:00Z"))
    private val today = LocalDate.parse("2026-10-21")
    private val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), VitQuestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val preferences = FakeUserPreferencesRepository()
        val sensor = FakeStepSensorSource()
        val tracker = StepSensorTracker(sensor, database.stepsDao(), preferences, time, scope)
        steps = OfflineFirstStepsRepository(database.stepsDao(), FakeHealthConnectStepsSource(), sensor, preferences, tracker, time)
        workouts = OfflineWorkoutRepository(database.workoutDao(), time)
        habits = OfflineHabitRepository(database.habitDao(), database.bodyDao(), database.foodDao(), database.workoutDao(), steps, time)
    }

    @After
    fun tearDown() {
        scope.cancel()
        database.close()
    }

    @Test
    fun playerSessionsKeepEveryStepAndTheEstimate() = runTest {
        val plan = WorkoutGenerator.generate(WorkoutRequest(5, FitnessExperience.BEGINNER, WorkoutLocation.HOME, emptySet(), WorkoutFocus.FULL_BODY, 1))
        val state = WorkoutPlayerEngine.finishEarly(WorkoutPlayerEngine.tick(WorkoutPlayerEngine.start(plan), plan, WorkoutPlayerEngine.READY_MS + 45_000))
        workouts.savePlayerSession(plan, state, startedAt = time.now(), weightKg = 70.0)

        val session = workouts.sessions().first().single()
        assertEquals(today, session.date)
        assertEquals(WorkoutSource.PLAYER, session.source)
        assertFalse(session.completed)
        // 30 s warm-up, 5 s transition, then 10 s into the next exercise.
        assertEquals(40, session.activeSeconds)
        assertEquals(plan.steps.size, session.exercises.size)
        assertEquals(StepStatus.COMPLETED, session.exercises.first().status)
        assertEquals(plan.steps.first().exercise.name, session.exercises.first().name)
        assertTrue((session.estimatedKcal ?: 0.0) > 0.0)

        workouts.deleteSession(session.id)
        assertTrue(workouts.sessions().first().isEmpty())
        // Exercises go with their session.
        assertTrue(database.workoutDao().observeSessions().first().isEmpty())
    }

    @Test
    fun manualWorkoutsUseIntensityForTheEstimateOnlyWithAWeight() = runTest {
        workouts.logManual(ManualWorkoutInput(today.minusDays(1), 30, WorkoutIntensity.MODERATE, "  Futsal "), weightKg = 60.0)
        workouts.logManual(ManualWorkoutInput(today, 20, WorkoutIntensity.LIGHT, ""), weightKg = null)
        val sessions = workouts.sessions().first()
        assertEquals(listOf(today, today.minusDays(1)), sessions.map { it.date })
        assertEquals(null, sessions[0].estimatedKcal)
        assertEquals(null, sessions[0].note)
        // 3.8 MET × 60 kg × 0.5 h
        assertEquals(114.0, sessions[1].estimatedKcal!!, 0.01)
        assertEquals("Futsal", sessions[1].note)
    }

    @Test
    fun dayRecordsCombineEveryTracker() = runTest {
        steps.setManualSteps(today, 7_500)
        database.bodyDao().insertWater(io.github.vitoosh.vitquest.core.data.local.WaterLogEntity(date = today.toString(), amountMl = 500, loggedAtEpochMs = 1))
        database.bodyDao().insertWater(io.github.vitoosh.vitquest.core.data.local.WaterLogEntity(date = today.toString(), amountMl = 750, loggedAtEpochMs = 2))
        database.foodDao().insertLog(
            FoodLogEntity(
                date = today.minusDays(1).toString(), meal = "LUNCH", foodId = null, nameId = "Nasi", nameEn = "Rice", grams = 150.0, quantity = 1.0,
                servingLabelId = null, servingLabelEn = null, kcal = 270.0, proteinG = null, carbsG = null, fatG = null, createdAtEpochMs = 1,
            ),
        )
        workouts.logManual(ManualWorkoutInput(today, 15, WorkoutIntensity.LIGHT, null), weightKg = null)

        val records = habits.dayRecords(today.minusDays(6), today).first()
        val todayRecord = records.getValue(today)
        assertEquals(7_500, todayRecord.steps)
        assertEquals(1_250, todayRecord.waterMl)
        assertEquals(1, todayRecord.workoutCount)
        assertEquals(900, todayRecord.workoutSeconds)
        assertFalse(todayRecord.foodLogged)
        assertTrue(records.getValue(today.minusDays(1)).foodLogged)
        assertEquals(StepSource.MANUAL, steps.observeDailySteps(today, today).first().getValue(today).source)
    }

    @Test
    fun customHabitsCanBeEditedCheckedAndCorrected() = runTest {
        val id = habits.addHabit("  Tidur 7 jam ", 5)
        habits.setCheckIn(id, today, done = true)
        habits.setCheckIn(id, today.minusDays(1), done = true)
        habits.setCheckIn(id, today.minusDays(1), done = false)
        habits.updateHabit(id, "Tidur cukup", 4)

        val habit = habits.customHabits().first().single()
        assertEquals("Tidur cukup", habit.name)
        assertEquals(4, habit.targetDaysPerWeek)
        assertEquals(setOf(today), habit.checkIns)

        habits.deleteHabit(id)
        assertTrue(habits.customHabits().first().isEmpty())
    }
}
