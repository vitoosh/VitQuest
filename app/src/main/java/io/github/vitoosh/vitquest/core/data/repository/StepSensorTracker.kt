package io.github.vitoosh.vitquest.core.data.repository

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.github.vitoosh.vitquest.core.data.local.StepsDao
import io.github.vitoosh.vitquest.core.data.sensor.MotionSensorSource
import io.github.vitoosh.vitquest.core.data.sensor.StepSensorSource
import io.github.vitoosh.vitquest.core.di.ApplicationScope
import io.github.vitoosh.vitquest.core.domain.steps.MotionStepDetector
import io.github.vitoosh.vitquest.core.domain.steps.StepDiagnostics
import io.github.vitoosh.vitquest.core.domain.steps.StepSourceResolver
import io.github.vitoosh.vitquest.core.model.StepSource
import io.github.vitoosh.vitquest.core.time.TimeSource
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Collects phone steps while VitQuest is visible or its background service runs.
 *
 * Two counts are kept: the phone's built-in step counter (cumulative, counted by
 * the hardware even while nothing listens) and VitQuest's own count from the
 * accelerometer, which only sees steps while collecting. There is a single
 * collector however many holders there are, so no step is counted twice.
 */
@Singleton
class StepSensorTracker @Inject constructor(
    private val sensor: StepSensorSource,
    private val motion: MotionSensorSource,
    private val stepsDao: StepsDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val timeSource: TimeSource,
    @param:ApplicationScope private val scope: CoroutineScope,
) : DefaultLifecycleObserver {

    enum class Holder { APP, SERVICE }

    private val holders = mutableSetOf<Holder>()
    private var counterJob: Job? = null
    private var motionJob: Job? = null

    private val _diagnostics = MutableStateFlow(StepDiagnostics())
    val diagnostics: StateFlow<StepDiagnostics> = _diagnostics.asStateFlow()

    override fun onStart(owner: LifecycleOwner) = hold(Holder.APP)

    override fun onStop(owner: LifecycleOwner) = release(Holder.APP)

    @Synchronized
    fun hold(holder: Holder) {
        if (holders.add(holder)) restart()
    }

    @Synchronized
    fun release(holder: Holder) {
        if (holders.remove(holder)) restart()
    }

    /** Starts or stops collection after a permission or preference change. */
    @Synchronized
    fun restartIfActive() {
        if (holders.isNotEmpty()) restart()
    }

    private fun restart() {
        counterJob?.cancel()
        motionJob?.cancel()
        counterJob = null
        motionJob = null
        val active = holders.isNotEmpty() && sensor.hasPermission()
        val service = Holder.SERVICE in holders
        _diagnostics.update {
            it.copy(
                collecting = active,
                serviceRunning = service,
                counterAvailable = sensor.isAvailable,
                motionAvailable = motion.isAvailable,
                motionWakeUp = service && motion.hasWakeUpSensor,
            )
        }
        if (!active) return
        if (sensor.isAvailable) counterJob = scope.launch { collectCounter() }
        if (motion.isAvailable) motionJob = scope.launch { collectMotion(preferWakeUp = service) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun collectCounter() {
        phoneStepsEnabled()
            .flatMapLatest { enabled -> if (enabled) sensor.readings() else emptyFlow() }
            .conflate()
            .collect { reading ->
                _diagnostics.update { it.copy(lastCounterValue = reading.counter, lastCounterAt = reading.readAt) }
                stepsDao.applySensorReading(reading, timeSource.zone(), timeSource.now())
                // Write at most every few seconds; the next reading still includes every step.
                delay(COUNTER_WRITE_INTERVAL_MS)
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun collectMotion(preferWakeUp: Boolean) {
        val detector = MotionStepDetector()
        val pending = mutableMapOf<LocalDate, Int>()
        var lastFlushMs = timeSource.now().toEpochMilli()
        var samples = 0L
        try {
            phoneStepsEnabled()
                .flatMapLatest { enabled -> if (enabled) motion.samples(preferWakeUp) else emptyFlow() }
                .collect { sample ->
                    val added = detector.onSample(sample.timestampNs, sample.x, sample.y, sample.z)
                    // Publish the sample count about once a second, not 50 times.
                    if (++samples % DIAGNOSTICS_EVERY_SAMPLES == 0L) {
                        _diagnostics.update { it.copy(motionSamplesThisSession = it.motionSamplesThisSession + DIAGNOSTICS_EVERY_SAMPLES) }
                    }
                    if (added > 0) {
                        val at = Instant.ofEpochMilli(sample.wallClockMs)
                        val date = at.atZone(timeSource.zone()).toLocalDate()
                        pending[date] = (pending[date] ?: 0) + added
                        _diagnostics.update { it.copy(motionStepsThisSession = it.motionStepsThisSession + added, lastMotionStepAt = at) }
                    }
                    val nowMs = timeSource.now().toEpochMilli()
                    if (pending.isNotEmpty() && nowMs - lastFlushMs >= MOTION_WRITE_INTERVAL_MS) {
                        flush(pending)
                        lastFlushMs = nowMs
                    }
                }
        } finally {
            // Keep what was counted when collection stops.
            withContext(NonCancellable) { flush(pending) }
        }
    }

    private suspend fun flush(pending: MutableMap<LocalDate, Int>) {
        val now = timeSource.now()
        pending.forEach { (date, steps) -> stepsDao.addSteps(date, StepSource.MOTION, steps, now) }
        pending.clear()
    }

    private fun phoneStepsEnabled() = preferencesRepository.userPreferences
        .map { StepSourceResolver.usesPhoneSensor(it.stepSourcePreference) }
        .distinctUntilChanged()

    private companion object {
        const val COUNTER_WRITE_INTERVAL_MS = 10_000L
        const val MOTION_WRITE_INTERVAL_MS = 15_000L
        const val DIAGNOSTICS_EVERY_SAMPLES = 50L
    }
}
