package io.github.vitoosh.vitquest.core.data.repository

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.github.vitoosh.vitquest.core.data.local.StepsDao
import io.github.vitoosh.vitquest.core.data.sensor.StepSensorSource
import io.github.vitoosh.vitquest.core.di.ApplicationScope
import io.github.vitoosh.vitquest.core.domain.steps.StepSourceResolver
import io.github.vitoosh.vitquest.core.time.TimeSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Reads the phone step counter while VitQuest is visible. The hardware keeps
 * counting in the background, so each reading captures every step since the
 * previous one; nothing runs while the app is closed.
 */
@Singleton
class StepSensorTracker @Inject constructor(
    private val sensor: StepSensorSource,
    private val stepsDao: StepsDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val timeSource: TimeSource,
    @param:ApplicationScope private val scope: CoroutineScope,
) : DefaultLifecycleObserver {

    private var job: Job? = null
    private var started = false

    override fun onStart(owner: LifecycleOwner) {
        started = true
        start()
    }

    override fun onStop(owner: LifecycleOwner) {
        started = false
        job?.cancel()
        job = null
    }

    fun restartIfActive() {
        if (!started) return
        job?.cancel()
        start()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun start() {
        if (!sensor.isAvailable || !sensor.hasPermission()) return
        job = scope.launch {
            preferencesRepository.userPreferences
                .map { StepSourceResolver.usesPhoneSensor(it.stepSourcePreference) }
                .flatMapLatest { enabled -> if (enabled) sensor.readings() else emptyFlow() }
                .conflate()
                .collect { reading ->
                    stepsDao.applySensorReading(reading, timeSource.zone(), timeSource.now())
                    // Write at most every few seconds; the next reading still includes every step.
                    delay(WRITE_INTERVAL_MS)
                }
        }
    }

    private companion object {
        const val WRITE_INTERVAL_MS = 10_000L
    }
}
