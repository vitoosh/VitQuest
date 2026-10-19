package io.github.vitoosh.vitquest.core.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.core.data.repository.OfflineProfileRepository
import io.github.vitoosh.vitquest.core.model.ActivityLevel
import io.github.vitoosh.vitquest.core.model.Goals
import io.github.vitoosh.vitquest.core.model.UserProfile
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileDaoTest {

    private lateinit var database: VitQuestDatabase
    private lateinit var repository: OfflineProfileRepository
    private val clock = Clock.fixed(Instant.parse("2026-01-15T03:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VitQuestDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = OfflineProfileRepository(database.profileDao(), clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun emptyDatabaseHasNoProfileOrGoals() = runTest {
        assertNull(repository.profile.first())
        assertNull(repository.goals.first())
    }

    @Test
    fun savedProfileAndGoalsArePersisted() = runTest {
        val profile = UserProfile(age = 30, currentWeightKg = 80.0, activityLevel = ActivityLevel.LIGHT)
        val goals = Goals(targetWeightKg = 74.0, dailyStepGoal = 7_000, dailyWaterTargetMl = 2_400, waterServingMl = 250)

        repository.saveProfileAndGoals(profile, goals)

        assertEquals(profile, repository.profile.first())
        assertEquals(goals, repository.goals.first())
        assertEquals(clock.millis(), database.profileDao().observeProfile().first()?.updatedAtEpochMs)
    }

    @Test
    fun savingAgainReplacesTheSingleRow() = runTest {
        val goals = Goals(dailyStepGoal = 7_000, dailyWaterTargetMl = 2_000, waterServingMl = 250)
        repository.saveProfileAndGoals(UserProfile(age = 30), goals)
        repository.saveProfileAndGoals(UserProfile(age = 31), goals.copy(dailyStepGoal = 8_000))

        assertEquals(31, repository.profile.first()?.age)
        assertEquals(8_000, repository.goals.first()?.dailyStepGoal)
    }

    @Test
    fun clearingTablesRemovesAllRecords() = runTest {
        repository.saveProfileAndGoals(
            UserProfile(age = 30),
            Goals(dailyStepGoal = 7_000, dailyWaterTargetMl = 2_000, waterServingMl = 250),
        )

        database.clearAllTables()

        assertNull(repository.profile.first())
        assertNull(repository.goals.first())
    }
}
