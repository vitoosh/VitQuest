package io.github.vitoosh.vitquest.core.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

/** One accelerometer reading. [timestampNs] is monotonic; [wallClockMs] is when it happened. */
data class MotionSample(val timestampNs: Long, val wallClockMs: Long, val x: Float, val y: Float, val z: Float)

/** The accelerometer, used by VitQuest's own step counting. */
interface MotionSensorSource {
    val isAvailable: Boolean

    /** True when the phone has a wake-up accelerometer that keeps delivering while the screen is off. */
    val hasWakeUpSensor: Boolean

    /** Emits samples while collected. [preferWakeUp] picks the wake-up sensor when there is one. */
    fun samples(preferWakeUp: Boolean): Flow<MotionSample>
}

@Singleton
class AndroidMotionSensorSource @Inject constructor(
    @ApplicationContext context: Context,
) : MotionSensorSource {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val regular: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val wakeUp: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true)

    override val isAvailable: Boolean get() = regular != null || wakeUp != null

    override val hasWakeUpSensor: Boolean get() = wakeUp != null

    override fun samples(preferWakeUp: Boolean): Flow<MotionSample> = callbackFlow {
        val sensor = (if (preferWakeUp) wakeUp ?: regular else regular ?: wakeUp)
        if (sensor == null || sensorManager == null) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Batched events arrive late; work out when each one actually happened.
                val ageMs = (SystemClock.elapsedRealtimeNanos() - event.timestamp) / 1_000_000L
                trySend(
                    MotionSample(event.timestamp, System.currentTimeMillis() - ageMs, event.values[0], event.values[1], event.values[2]),
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        // About 50 Hz, delivered in batches of up to 5 s so the CPU can sleep in between.
        sensorManager.registerListener(listener, sensor, SAMPLING_PERIOD_US, MAX_REPORT_LATENCY_US)
        awaitClose { sensorManager.unregisterListener(listener) }
    }.buffer(Channel.UNLIMITED)

    private companion object {
        const val SAMPLING_PERIOD_US = 20_000
        const val MAX_REPORT_LATENCY_US = 5_000_000
    }
}
