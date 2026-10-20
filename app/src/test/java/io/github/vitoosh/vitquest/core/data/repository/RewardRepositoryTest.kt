package io.github.vitoosh.vitquest.core.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.core.data.local.ObtainOutcome
import io.github.vitoosh.vitquest.core.data.local.VitQuestDatabase
import io.github.vitoosh.vitquest.core.data.preferences.UserPreferencesDataSource
import io.github.vitoosh.vitquest.core.model.BudgetEntryKind
import io.github.vitoosh.vitquest.core.model.Goals
import io.github.vitoosh.vitquest.core.model.RewardReason
import io.github.vitoosh.vitquest.core.model.RewardType
import io.github.vitoosh.vitquest.core.model.WorkoutIntensity
import io.github.vitoosh.vitquest.testing.FakeHealthConnectStepsSource
import io.github.vitoosh.vitquest.testing.FakeProfileRepository
import io.github.vitoosh.vitquest.testing.FakeStepSensorSource
import io.github.vitoosh.vitquest.testing.FakeUserPreferencesRepository
import io.github.vitoosh.vitquest.testing.FixedTimeSource
import java.io.File
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
class RewardRepositoryTest {

    private lateinit var database: VitQuestDatabase
    private lateinit var rewards: OfflineRewardRepository
    private lateinit var workouts: OfflineWorkoutRepository
    private val profiles = FakeProfileRepository()
    private val time = FixedTimeSource(Instant.parse("2026-10-21T00:00:00Z"))
    private val today = LocalDate.parse("2026-10-21")
    private val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())
    private val prefsFile = File.createTempFile("rewards", ".preferences_pb").apply { delete() }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), VitQuestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val preferences = FakeUserPreferencesRepository()
        val sensor = FakeStepSensorSource()
        val tracker = StepSensorTracker(sensor, database.stepsDao(), preferences, time, scope)
        val steps = OfflineFirstStepsRepository(database.stepsDao(), FakeHealthConnectStepsSource(), sensor, preferences, tracker, time)
        workouts = OfflineWorkoutRepository(database.workoutDao(), time)
        val habits = OfflineHabitRepository(database.habitDao(), database.bodyDao(), database.foodDao(), database.workoutDao(), steps, time)
        val body = OfflineBodyRepository(database.bodyDao(), database.profileDao(), time)
        val store = UserPreferencesDataSource(PreferenceDataStoreFactory.create(scope = scope) { prefsFile })
        profiles.goalsState.value = Goals(dailyStepGoal = 6_000, dailyWaterTargetMl = 2_000, waterServingMl = 250)
        rewards = OfflineRewardRepository(database.rewardDao(), habits, workouts, body, profiles, store, time)
    }

    @After
    fun tearDown() {
        scope.cancel()
        database.close()
        prefsFile.delete()
    }

    @Test
    fun syncGrantsOnceAndReversesDeletedActivity() = runTest {
        workouts.logManual(ManualWorkoutInput(today, 20, WorkoutIntensity.MODERATE, null), weightKg = null)
        rewards.sync()
        rewards.sync()
        // Workout 50 + first-workout achievement 100.
        assertEquals(150, rewards.xp.first())

        workouts.deleteSession(workouts.sessions().first().single().id)
        rewards.sync()
        assertEquals(0, rewards.xp.first())
        val history = rewards.transactions().first()
        assertEquals(2, history.count { it.isReversal })
        assertTrue(history.all { it.reason == RewardReason.WORKOUT || it.reason == RewardReason.ACHIEVEMENT })
    }

    @Test
    fun markingObtainedIsAtomicAndNeverRecordsTwice() = runTest {
        rewards.saveReward(null, RewardInput("Botol minum", null, RewardType.PHYSICAL, priceRupiah = 85_000))
        val reward = rewards.rewards().first().single()

        val results = (1..5).map { async { rewards.markObtained("request-1", reward.id, 80_000) } }.awaitAll()
        assertEquals(1, results.count { it is ObtainOutcome.Recorded })
        assertEquals(4, results.count { it is ObtainOutcome.AlreadyRecorded })
        val got = rewards.history().first().single()
        assertEquals(Triple("Botol minum", 80_000L, today), Triple(got.rewardName, got.pricePaidRupiah, got.date))
        // The price paid is noted once as real spending for that reward.
        val spending = rewards.budgetEntries().first().single()
        assertEquals(Triple(BudgetEntryKind.SPENDING, 80_000L, reward.id), Triple(spending.kind, spending.amountRupiah, spending.rewardId))
        // Getting a reward never touches XP.
        assertEquals(0, rewards.xp.first())

        assertTrue(rewards.markObtained("request-2", 9_999, null) is ObtainOutcome.RewardMissing)
    }

    @Test
    fun undoRemovesTheRecordAndItsSpendingOnce() = runTest {
        rewards.saveReward(null, RewardInput("Nonton film", null, RewardType.EXPERIENCE, priceRupiah = null))
        val reward = rewards.rewards().first().single()
        val free = rewards.markObtained("a", reward.id, null) as ObtainOutcome.Recorded
        val paid = rewards.markObtained("b", reward.id, 50_000) as ObtainOutcome.Recorded
        assertEquals(1, rewards.budgetEntries().first().size)

        assertTrue(rewards.undoObtained(paid.historyId))
        assertFalse(rewards.undoObtained(paid.historyId))
        assertTrue(rewards.budgetEntries().first().isEmpty())
        assertEquals(listOf(free.historyId), rewards.history().first().map { it.id })
        // Deleting the reward keeps what the user got, with its name.
        rewards.deleteReward(reward.id)
        assertEquals("Nonton film", rewards.history().first().single().rewardName)
    }

    @Test
    fun realMoneyBudgetTracksSavings() = runTest {
        rewards.saveReward(null, RewardInput("Sepatu", null, RewardType.PHYSICAL, priceRupiah = 600_000))
        val shoes = rewards.rewards().first().single()
        rewards.addBudgetEntry(BudgetEntryInput(today, BudgetEntryKind.SAVING, 150_000, shoes.id, " tabungan "))
        rewards.setMonthlyBudget(300_000)
        assertEquals(300_000L, rewards.monthlyBudget.first())
        val entry = rewards.budgetEntries().first().single()
        assertEquals(150_000L, entry.amountRupiah)
        assertEquals("tabungan", entry.note)
        rewards.deleteBudgetEntry(entry.id)
        assertTrue(rewards.budgetEntries().first().isEmpty())
    }
}
