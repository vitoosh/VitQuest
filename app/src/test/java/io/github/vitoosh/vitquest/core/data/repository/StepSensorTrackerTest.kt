package io.github.vitoosh.vitquest.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.core.data.local.VitQuestDatabase
import io.github.vitoosh.vitquest.core.data.sensor.MotionSample
import io.github.vitoosh.vitquest.core.model.StepSource
import io.github.vitoosh.vitquest.core.model.StepSourcePreference
import io.github.vitoosh.vitquest.testing.FakeMotionSensorSource
import io.github.vitoosh.vitquest.testing.FakeStepSensorSource
import io.github.vitoosh.vitquest.testing.FakeUserPreferencesRepository
import io.github.vitoosh.vitquest.testing.FixedTimeSource
import java.time.Instant
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
class StepSensorTrackerTest {

    private lateinit var database: VitQuestDatabase
    private lateinit var tracker: StepSensorTracker
    private val counter = FakeStepSensorSource()
    private val motion = FakeMotionSensorSource()
    private val preferences = FakeUserPreferencesRepository()
    private val start = Instant.parse("2026-10-21T01:00:00Z")
    private val time = FixedTimeSource(start)
    private val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())

    @Before
    fun setUp() {
        // Run queries on the calling thread so every sample is processed before the test checks the result.
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), VitQuestDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        tracker = StepSensorTracker(counter, motion, database.stepsDao(), preferences, time, scope)
    }

    @After
    fun tearDown() {
        scope.cancel()
        database.close()
    }

    /** One minute of walking at 1.8 steps per second, sampled at 50 Hz. */
    private suspend fun walkOneMinute() {
        for (i in 0 until 60 * 50) {
            val t = i / 50.0
            val at = start.plusMillis(i * 20L)
            time.instant = at
            motion.emitted.emit(MotionSample(i * 20_000_000L, at.toEpochMilli(), 0f, 0f, (9.81 + 2.5 * sin(2 * PI * 1.8 * t)).toFloat()))
        }
    }

    private suspend fun motionSteps(): Int = database.stepsDao().getSteps("2026-10-21", StepSource.MOTION.name) ?: 0

    @Test
    fun motionStepsAreStoredAndKeptWhenCollectionStops() = runTest {
        tracker.hold(StepSensorTracker.Holder.APP)
        assertTrue(tracker.diagnostics.value.collecting)
        walkOneMinute()
        tracker.release(StepSensorTracker.Holder.APP)

        assertTrue("got ${motionSteps()}", abs(motionSteps() - 108) <= 5)
        assertEquals(motionSteps().toLong(), tracker.diagnostics.value.motionStepsThisSession)
        assertFalse(tracker.diagnostics.value.collecting)
    }

    @Test
    fun theAppAndTheServiceShareOneCollector() = runTest {
        tracker.hold(StepSensorTracker.Holder.APP)
        tracker.hold(StepSensorTracker.Holder.SERVICE)
        assertTrue(tracker.diagnostics.value.serviceRunning)
        walkOneMinute()
        // The app goes to the background; the service keeps counting with the same collector.
        tracker.release(StepSensorTracker.Holder.APP)
        tracker.release(StepSensorTracker.Holder.SERVICE)

        assertTrue("got ${motionSteps()}", abs(motionSteps() - 108) <= 5)
    }

    @Test
    fun nothingIsCollectedWithoutPermissionOrWhenPhoneStepsAreNotUsed() = runTest {
        counter.permissionGranted = false
        tracker.hold(StepSensorTracker.Holder.APP)
        assertFalse(tracker.diagnostics.value.collecting)
        tracker.release(StepSensorTracker.Holder.APP)

        counter.permissionGranted = true
        preferences.setStepSourcePreference(StepSourcePreference.HEALTH_CONNECT)
        tracker.hold(StepSensorTracker.Holder.APP)
        walkOneMinute()
        tracker.release(StepSensorTracker.Holder.APP)
        assertEquals(0, motionSteps())
    }
}
