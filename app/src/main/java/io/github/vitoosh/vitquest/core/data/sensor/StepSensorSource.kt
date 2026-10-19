package io.github.vitoosh.vitquest.core.data.sensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.vitoosh.vitquest.core.domain.steps.SensorReading
import io.github.vitoosh.vitquest.core.time.TimeSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** The phone's hardware step counter (Sensor.TYPE_STEP_COUNTER). */
interface StepSensorSource {
    val isAvailable: Boolean

    fun hasPermission(): Boolean

    /** Emits counter readings while collected. Collect only while the app is visible. */
    fun readings(): Flow<SensorReading>
}

@Singleton
class AndroidStepSensorSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeSource: TimeSource,
) : StepSensorSource {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val sensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    override val isAvailable: Boolean get() = sensor != null

    override fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    override fun readings(): Flow<SensorReading> = callbackFlow {
        val stepSensor = sensor
        if (stepSensor == null || !hasPermission()) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val now = timeSource.now()
                trySend(
                    SensorReading(
                        bootCount = bootCount(),
                        counter = event.values.firstOrNull()?.toLong() ?: return,
                        readAt = now,
                        bootedAt = now.minusMillis(SystemClock.elapsedRealtime()),
                    ),
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager?.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager?.unregisterListener(listener) }
    }

    private fun bootCount(): Int? =
        runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT) }.getOrNull()
}
