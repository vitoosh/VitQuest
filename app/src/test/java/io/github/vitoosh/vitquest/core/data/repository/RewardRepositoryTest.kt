package io.github.vitoosh.vitquest.core.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.core.data.local.RedeemOutcome
import io.github.vitoosh.vitquest.core.data.local.VitQuestDatabase
import io.github.vitoosh.vitquest.core.data.preferences.UserPreferencesDataSource
import io.github.vitoosh.vitquest.core.model.BudgetEntryKind
import io.github.vitoosh.vitquest.core.model.Goals
import io.github.vitoosh.vitquest.core.model.RedemptionStatus
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
        assertEquals(15, rewards.credits.first())

        workouts.deleteSession(workouts.sessions().first().single().id)
        rewards.sync()
        assertEquals(0, rewards.xp.first())
        val history = rewards.transactions().first()
        assertEquals(4, history.count { it.isReversal })
        assertTrue(history.all { it.reason == RewardReason.WORKOUT || it.reason == RewardReason.ACHIEVEMENT })
    }

    private suspend fun earnCredits() {
        // Two workouts a day for a few days, so there is something to spend.
        (0L..4L).forEach { back ->
            repeat(2) { workouts.logManual(ManualWorkoutInput(today.minusDays(back), 15, WorkoutIntensity.LIGHT, null), weightKg = null) }
        }
        rewards.sync()
    }

    @Test
    fun redeemingIsAtomicAndNeverChargesTwice() = runTest {
        earnCredits()
        val start = rewards.credits.first()
        rewards.saveReward(null, RewardInput("Botol minum", null, RewardType.PHYSICAL, creditCost = 30, priceRupiah = 85_000))
        val reward = rewards.rewards().first().single()

        val results = (1..5).map { async { rewards.redeem("request-1", reward.id) } }.awaitAll()
        assertEquals(1, results.count { it is RedeemOutcome.Redeemed })
        assertEquals(4, results.count { it is RedeemOutcome.AlreadyRedeemed })
        assertEquals(start - 30, rewards.credits.first())
        assertEquals(1, rewards.redemptions().first().size)

        val expensive = RewardInput("Liburan", null, RewardType.EXPERIENCE, creditCost = 10_000, priceRupiah = null)
        rewards.saveReward(null, expensive)
        val big = rewards.rewards().first().first { it.name == "Liburan" }
        assertTrue(rewards.redeem("request-2", big.id) is RedeemOutcome.NotEnoughCredits)
        assertEquals(start - 30, rewards.credits.first())
    }

    @Test
    fun cancellingRefundsOnceAndKeepsTheHistory() = runTest {
        earnCredits()
        val start = rewards.credits.first()
        rewards.saveReward(null, RewardInput("Nonton film", null, RewardType.EXPERIENCE, creditCost = 40, priceRupiah = null))
        val reward = rewards.rewards().first().single()
        val outcome = rewards.redeem("r", reward.id) as RedeemOutcome.Redeemed

        assertTrue(rewards.cancelRedemption(outcome.redemptionId))
        assertFalse(rewards.cancelRedemption(outcome.redemptionId))
        assertEquals(start, rewards.credits.first())
        assertEquals(RedemptionStatus.CANCELLED, rewards.redemptions().first().single().status)
        // Deleting the reward keeps the redemption and its name.
        rewards.deleteReward(reward.id)
        assertEquals("Nonton film", rewards.redemptions().first().single().rewardName)
    }

    @Test
    fun realMoneyBudgetIsSeparateFromCredits() = runTest {
        rewards.saveReward(null, RewardInput("Sepatu", null, RewardType.PHYSICAL, creditCost = 500, priceRupiah = 600_000))
        val shoes = rewards.rewards().first().single()
        rewards.addBudgetEntry(BudgetEntryInput(today, BudgetEntryKind.SAVING, 150_000, shoes.id, " tabungan "))
        rewards.setMonthlyBudget(300_000)
        assertEquals(300_000L, rewards.monthlyBudget.first())
        val entry = rewards.budgetEntries().first().single()
        assertEquals(150_000L, entry.amountRupiah)
        assertEquals("tabungan", entry.note)
        assertEquals(0, rewards.credits.first())
        rewards.deleteBudgetEntry(entry.id)
        assertTrue(rewards.budgetEntries().first().isEmpty())
    }
}
