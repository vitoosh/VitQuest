package io.github.vitoosh.vitquest.integration

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.core.data.backup.LocalBackupRepository
import io.github.vitoosh.vitquest.core.data.backup.RestoreResult
import io.github.vitoosh.vitquest.core.data.local.VitQuestDatabase
import io.github.vitoosh.vitquest.core.data.preferences.UserPreferencesDataSource
import io.github.vitoosh.vitquest.core.data.repository.ManualWorkoutInput
import io.github.vitoosh.vitquest.core.data.repository.OfflineBodyRepository
import io.github.vitoosh.vitquest.core.data.repository.OfflineFirstStepsRepository
import io.github.vitoosh.vitquest.core.data.repository.OfflineHabitRepository
import io.github.vitoosh.vitquest.core.data.repository.OfflineWorkoutRepository
import io.github.vitoosh.vitquest.core.data.repository.ReminderScheduler
import io.github.vitoosh.vitquest.core.data.repository.StepSensorTracker
import io.github.vitoosh.vitquest.core.data.seed.SeedDataLoader
import io.github.vitoosh.vitquest.core.domain.habits.Achievements
import io.github.vitoosh.vitquest.core.model.Goals
import io.github.vitoosh.vitquest.core.model.ReminderType
import io.github.vitoosh.vitquest.core.model.WorkoutIntensity
import io.github.vitoosh.vitquest.testing.FakeHealthConnectStepsSource
import io.github.vitoosh.vitquest.testing.FakeProfileRepository
import io.github.vitoosh.vitquest.testing.FakeStepSensorSource
import io.github.vitoosh.vitquest.testing.FakeUserPreferencesRepository
import io.github.vitoosh.vitquest.testing.FixedTimeSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A day of use through the real repositories and database, then a move to a
 * "new phone" with a backup: nothing is lost.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class OfflineJourneyIntegrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())
    private val prefsFile = File.createTempFile("journey", ".preferences_pb").apply { delete() }
    private val time = FixedTimeSource(Instant.parse("2026-10-21T05:00:00Z"))
    private val today = LocalDate.parse("2026-10-21")
    private val profiles = FakeProfileRepository()

    private lateinit var database: VitQuestDatabase
    private lateinit var workouts: OfflineWorkoutRepository
    private lateinit var body: OfflineBodyRepository
    private lateinit var habits: OfflineHabitRepository
    private lateinit var backups: LocalBackupRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, VitQuestDatabase::class.java).allowMainThreadQueries().build()
        val sensor = FakeStepSensorSource()
        val userPrefs = FakeUserPreferencesRepository()
        val tracker = StepSensorTracker(sensor, database.stepsDao(), userPrefs, time, scope)
        val steps = OfflineFirstStepsRepository(database.stepsDao(), FakeHealthConnectStepsSource(), sensor, userPrefs, tracker, time)
        val store = UserPreferencesDataSource(PreferenceDataStoreFactory.create(scope = scope) { prefsFile })
        workouts = OfflineWorkoutRepository(database.workoutDao(), time)
        body = OfflineBodyRepository(database.bodyDao(), database.profileDao(), time)
        habits = OfflineHabitRepository(database.habitDao(), database.bodyDao(), database.foodDao(), database.workoutDao(), steps, time)
        profiles.goalsState.value = Goals(dailyStepGoal = 6_000, dailyWaterTargetMl = 2_000, waterServingMl = 250)
        val seeds = SeedDataLoader(context, database.foodDao(), database.recipeDao(), store, time)
        val scheduler = object : ReminderScheduler {
            override suspend fun reschedule(type: ReminderType) = Unit
            override suspend fun rescheduleAll() = Unit
        }
        backups = LocalBackupRepository(context, database, store, seeds, scheduler, time)
    }

    @After
    fun tearDown() {
        scope.cancel()
        database.close()
        prefsFile.delete()
    }

    @Test
    fun aDayOfUseSurvivesMovingToANewPhone() = runTest {
        workouts.logManual(ManualWorkoutInput(today, 25, WorkoutIntensity.MODERATE, null), weightKg = null)
        repeat(8) { body.addWater(today, 250) }
        body.logWeight(today, 79.4, null)
        val habit = habits.addHabit("Tidur 7 jam", 5)
        habits.setCheckIn(habit, today, done = true)
        val file = ByteArrayOutputStream().also { backups.export(it) }.toByteArray()
        database.clearAllTables()
        assertTrue(workouts.sessions().first().isEmpty())

        assertEquals(RestoreResult.Restored, backups.restore(ByteArrayInputStream(file)))

        assertEquals(listOf(25 * 60), workouts.sessions().first().map { it.activeSeconds })
        // Achievements are worked out from the restored records.
        val records = habits.dayRecords(today.minusDays(30), today).first()
        assertTrue(Achievements.evaluate(records.values, workouts.sessions().first(), profiles.goalsState.value, habits.customHabits().first()).any { it.earnedOn != null })
        assertEquals(2_000, habits.dayRecords(today, today).first().getValue(today).waterMl)
        assertEquals(listOf(79.4), body.weights().first().map { it.weightKg })
        assertEquals(listOf(today), habits.customHabits().first().single().checkIns.toList())
    }
}
