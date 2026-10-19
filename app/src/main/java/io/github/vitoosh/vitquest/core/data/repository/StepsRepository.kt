package io.github.vitoosh.vitquest.core.data.repository

import io.github.vitoosh.vitquest.core.data.health.HealthConnectAvailability
import io.github.vitoosh.vitquest.core.data.health.HealthConnectReadResult
import io.github.vitoosh.vitquest.core.data.health.HealthConnectStepsSource
import io.github.vitoosh.vitquest.core.data.local.DailyStepsEntity
import io.github.vitoosh.vitquest.core.data.local.StepsDao
import io.github.vitoosh.vitquest.core.data.sensor.StepSensorSource
import io.github.vitoosh.vitquest.core.domain.steps.StepSourceResolver
import io.github.vitoosh.vitquest.core.model.DailySteps
import io.github.vitoosh.vitquest.core.model.StepRecord
import io.github.vitoosh.vitquest.core.model.StepSource
import io.github.vitoosh.vitquest.core.model.StepSourcePreference
import io.github.vitoosh.vitquest.core.time.TimeSource
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class HealthConnectStatus { NOT_SUPPORTED, UPDATE_REQUIRED, PERMISSION_REQUIRED, CONNECTED, READ_FAILED }

enum class PhoneSensorStatus { NOT_AVAILABLE, PERMISSION_REQUIRED, ACTIVE }

data class StepSourcesStatus(
    val healthConnect: HealthConnectStatus,
    val phoneSensor: PhoneSensorStatus,
    val lastHealthConnectSync: Instant? = null,
)

interface StepsRepository {
    val preference: Flow<StepSourcePreference>

    /** Null until the first [refresh] has checked availability and permissions. */
    val sourcesStatus: StateFlow<StepSourcesStatus?>

    /** One resolved value per day that has data in [from]..[to]. */
    fun observeDailySteps(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, DailySteps>>

    /** Re-checks permissions and pulls recent Health Connect totals. Cheap to call on every resume. */
    suspend fun refresh(force: Boolean = false)

    suspend fun setManualSteps(date: LocalDate, steps: Int)

    suspend fun clearManualSteps(date: LocalDate)

    suspend fun setPreference(preference: StepSourcePreference)
}

@Singleton
class OfflineFirstStepsRepository @Inject constructor(
    private val stepsDao: StepsDao,
    private val healthConnect: HealthConnectStepsSource,
    private val phoneSensor: StepSensorSource,
    private val preferencesRepository: UserPreferencesRepository,
    private val sensorTracker: StepSensorTracker,
    private val timeSource: TimeSource,
) : StepsRepository {

    private val refreshMutex = Mutex()
    private val _sourcesStatus = MutableStateFlow<StepSourcesStatus?>(null)
    override val sourcesStatus: StateFlow<StepSourcesStatus?> = _sourcesStatus.asStateFlow()

    override val preference: Flow<StepSourcePreference> =
        preferencesRepository.userPreferences.map { it.stepSourcePreference }

    override fun observeDailySteps(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, DailySteps>> =
        combine(stepsDao.observeRange(from.toString(), to.toString()), preference) { rows, pref ->
            StepSourceResolver.resolve(rows.mapNotNull { it.toRecord() }, pref)
        }

    override suspend fun refresh(force: Boolean) = refreshMutex.withLock {
        val pref = preference.first()
        val previous = _sourcesStatus.value
        val sensorStatus = when {
            !phoneSensor.isAvailable -> PhoneSensorStatus.NOT_AVAILABLE
            !phoneSensor.hasPermission() -> PhoneSensorStatus.PERMISSION_REQUIRED
            else -> PhoneSensorStatus.ACTIVE
        }
        var lastSync = previous?.lastHealthConnectSync
        val hcStatus = when (healthConnect.availability()) {
            HealthConnectAvailability.NOT_SUPPORTED -> HealthConnectStatus.NOT_SUPPORTED
            HealthConnectAvailability.UPDATE_REQUIRED -> HealthConnectStatus.UPDATE_REQUIRED
            HealthConnectAvailability.AVAILABLE -> when {
                !healthConnect.hasStepsPermission() -> HealthConnectStatus.PERMISSION_REQUIRED
                !StepSourceResolver.usesHealthConnect(pref) -> HealthConnectStatus.CONNECTED
                // Synced moments ago: keep the last result instead of querying Health Connect again.
                !force && lastSync != null && Duration.between(lastSync, timeSource.now()) < MIN_SYNC_INTERVAL ->
                    if (previous.healthConnect == HealthConnectStatus.READ_FAILED) {
                        HealthConnectStatus.READ_FAILED
                    } else {
                        HealthConnectStatus.CONNECTED
                    }
                else -> when (val result = syncHealthConnect()) {
                    HealthConnectReadResult.Failed -> HealthConnectStatus.READ_FAILED
                    HealthConnectReadResult.PermissionDenied -> HealthConnectStatus.PERMISSION_REQUIRED
                    is HealthConnectReadResult.Success -> {
                        lastSync = timeSource.now()
                        HealthConnectStatus.CONNECTED
                    }
                }
            }
        }
        _sourcesStatus.value = StepSourcesStatus(hcStatus, sensorStatus, lastSync)
        // Starts or stops sensor collection when the permission or preference changed.
        sensorTracker.restartIfActive()
    }

    private suspend fun syncHealthConnect(): HealthConnectReadResult {
        val to = timeSource.today()
        val from = to.minusDays(HISTORY_DAYS - 1)
        val result = healthConnect.readDailySteps(from, to)
        if (result is HealthConnectReadResult.Success) {
            val now = timeSource.now().toEpochMilli()
            val rows = result.stepsByDate.map { (date, steps) ->
                DailyStepsEntity(date.toString(), StepSource.HEALTH_CONNECT.name, steps.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), now)
            }
            stepsDao.replaceHealthConnectDays(from, to, rows)
        }
        return result
    }

    override suspend fun setManualSteps(date: LocalDate, steps: Int) {
        stepsDao.upsert(
            listOf(DailyStepsEntity(date.toString(), StepSource.MANUAL.name, steps, timeSource.now().toEpochMilli())),
        )
    }

    override suspend fun clearManualSteps(date: LocalDate) {
        stepsDao.delete(date.toString(), StepSource.MANUAL.name)
    }

    override suspend fun setPreference(preference: StepSourcePreference) {
        preferencesRepository.setStepSourcePreference(preference)
        refresh(force = true)
    }

    private fun DailyStepsEntity.toRecord(): StepRecord? {
        val source = StepSource.entries.firstOrNull { it.name == source } ?: return null
        val date = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
        return StepRecord(date, source, steps, Instant.ofEpochMilli(updatedAtEpochMs))
    }

    companion object {
        /** Health Connect only allows reading about 30 days before access was granted. */
        const val HISTORY_DAYS = 30L
        private val MIN_SYNC_INTERVAL: Duration = Duration.ofSeconds(30)
    }
}
