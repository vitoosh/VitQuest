package io.github.vitoosh.vitquest.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.core.data.health.HealthConnectAvailability
import io.github.vitoosh.vitquest.core.data.health.HealthConnectReadResult
import io.github.vitoosh.vitquest.core.data.local.VitQuestDatabase
import io.github.vitoosh.vitquest.core.model.StepSource
import io.github.vitoosh.vitquest.core.model.StepSourcePreference
import io.github.vitoosh.vitquest.testing.FakeHealthConnectStepsSource
import io.github.vitoosh.vitquest.testing.FakeStepSensorSource
import io.github.vitoosh.vitquest.testing.FakeUserPreferencesRepository
import io.github.vitoosh.vitquest.testing.FixedTimeSource
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StepsRepositoryTest {

    private lateinit var database: VitQuestDatabase
    private lateinit var repository: OfflineFirstStepsRepository
    private val healthConnect = FakeHealthConnectStepsSource()
    private val sensor = FakeStepSensorSource()
    private val preferences = FakeUserPreferencesRepository()
    // 19 October 2026, 10:00 in Jakarta.
    private val time = FixedTimeSource(Instant.parse("2026-10-19T03:00:00Z"))
    private val today = LocalDate.parse("2026-10-19")
    private val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), VitQuestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val tracker = StepSensorTracker(sensor, database.stepsDao(), preferences, time, scope)
        repository = OfflineFirstStepsRepository(database.stepsDao(), healthConnect, sensor, preferences, tracker, time)
    }

    @After
    fun tearDown() {
        scope.cancel()
        database.close()
    }

    private suspend fun today() = repository.observeDailySteps(today, today).first()[today]

    @Test
    fun connectedHealthConnectIsSyncedAndShown() = runTest {
        healthConnect.result = HealthConnectReadResult.Success(mapOf(today to 6_543L))

        repository.refresh()

        assertEquals(HealthConnectStatus.CONNECTED, repository.sourcesStatus.value?.healthConnect)
        assertEquals(6_543, today()?.steps)
        assertEquals(StepSource.HEALTH_CONNECT, today()?.source)
    }

    @Test
    fun missingPermissionIsReportedAndNothingIsRead() = runTest {
        healthConnect.permissionGranted = false
        sensor.permissionGranted = false

        repository.refresh()

        val status = repository.sourcesStatus.value
        assertEquals(HealthConnectStatus.PERMISSION_REQUIRED, status?.healthConnect)
        assertEquals(PhoneSensorStatus.PERMISSION_REQUIRED, status?.phoneSensor)
        assertEquals(0, healthConnect.reads)
        assertNull(today())
    }

    @Test
    fun unavailableSourcesAreReported() = runTest {
        healthConnect.availability = HealthConnectAvailability.UPDATE_REQUIRED
        sensor.isAvailable = false

        repository.refresh()

        assertEquals(HealthConnectStatus.UPDATE_REQUIRED, repository.sourcesStatus.value?.healthConnect)
        assertEquals(PhoneSensorStatus.NOT_AVAILABLE, repository.sourcesStatus.value?.phoneSensor)
    }

    @Test
    fun revokedPermissionDuringReadIsDetected() = runTest {
        healthConnect.result = HealthConnectReadResult.PermissionDenied
        repository.refresh()
        assertEquals(HealthConnectStatus.PERMISSION_REQUIRED, repository.sourcesStatus.value?.healthConnect)
    }

    @Test
    fun readFailureKeepsCachedData() = runTest {
        healthConnect.result = HealthConnectReadResult.Success(mapOf(today to 4_000L))
        repository.refresh()

        healthConnect.result = HealthConnectReadResult.Failed
        repository.refresh(force = true)

        assertEquals(HealthConnectStatus.READ_FAILED, repository.sourcesStatus.value?.healthConnect)
        assertEquals(4_000, today()?.steps)
    }

    @Test
    fun frequentRefreshesDoNotHitHealthConnectEveryTime() = runTest {
        repository.refresh()
        repository.refresh()
        assertEquals(1, healthConnect.reads)

        time.instant = time.instant.plus(Duration.ofMinutes(1))
        repository.refresh()
        assertEquals(2, healthConnect.reads)
    }

    @Test
    fun manualEntryOverridesAndCanBeRemoved() = runTest {
        healthConnect.result = HealthConnectReadResult.Success(mapOf(today to 6_000L))
        repository.refresh()

        repository.setManualSteps(today, 8_100)
        assertEquals(StepSource.MANUAL, today()?.source)

        repository.clearManualSteps(today)
        assertEquals(6_000, today()?.steps)
    }

    @Test
    fun preferenceChangesWhichSourceIsShown() = runTest {
        healthConnect.result = HealthConnectReadResult.Success(mapOf(today to 6_000L))
        repository.refresh()

        repository.setPreference(StepSourcePreference.MANUAL_ONLY)

        assertNull(today())
        assertEquals(StepSourcePreference.MANUAL_ONLY, repository.preference.first())
    }
}
