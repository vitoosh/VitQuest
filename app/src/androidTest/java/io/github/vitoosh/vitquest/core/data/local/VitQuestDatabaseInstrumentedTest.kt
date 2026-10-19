package io.github.vitoosh.vitquest.core.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.core.model.ActivityLevel
import io.github.vitoosh.vitquest.core.model.Goals
import io.github.vitoosh.vitquest.core.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on a real device or emulator: `./gradlew connectedDebugAndroidTest`. */
@RunWith(AndroidJUnit4::class)
class VitQuestDatabaseInstrumentedTest {

    private lateinit var database: VitQuestDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VitQuestDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun profileAndGoalsAreSavedTogether() = runTest {
        val dao = database.profileDao()
        val profile = UserProfile(age = 35, currentWeightKg = 78.5, activityLevel = ActivityLevel.MODERATE)
        val goals = Goals(targetWeightKg = 72.0, dailyStepGoal = 8_000, dailyWaterTargetMl = 2_350, waterServingMl = 250)

        dao.upsertProfileAndGoals(profile.toEntity(1L), goals.toEntity(1L))

        assertEquals(profile, dao.observeProfile().first()?.toModel())
        assertEquals(goals, dao.observeGoals().first()?.toModel())
    }
}
