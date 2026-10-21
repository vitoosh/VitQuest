package io.github.vitoosh.vitquest.core.steps

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.vitoosh.vitquest.MainActivity
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.data.repository.StepSensorTracker
import io.github.vitoosh.vitquest.core.data.repository.StepsRepository
import io.github.vitoosh.vitquest.core.data.sensor.MotionSensorSource
import io.github.vitoosh.vitquest.core.reminders.ReminderNotifications
import io.github.vitoosh.vitquest.core.time.TimeSource
import java.text.NumberFormat
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps step collection running while VitQuest is closed, with an ongoing
 * notification showing today's steps. Android requires the "health" service
 * type for this, which in turn requires the physical activity permission.
 */
@AndroidEntryPoint
class StepTrackingService : Service() {

    @Inject lateinit var tracker: StepSensorTracker

    @Inject lateinit var stepsRepository: StepsRepository

    @Inject lateinit var motion: MotionSensorSource

    @Inject lateinit var timeSource: TimeSource

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasActivityPermission(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        StepTrackingNotifications.createChannel(this)
        ServiceCompat.startForeground(
            this,
            StepTrackingNotifications.NOTIFICATION_ID,
            StepTrackingNotifications.build(this, steps = null),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else 0,
        )
        if (!started) {
            started = true
            tracker.hold(StepSensorTracker.Holder.SERVICE)
            if (!motion.hasWakeUpSensor) keepCpuAwake()
            scope.launch { updateNotification() }
        }
        // If the system stops the service to free memory, it is started again.
        return START_STICKY
    }

    override fun onDestroy() {
        tracker.release(StepSensorTracker.Holder.SERVICE)
        scope.cancel()
        wakeLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    /**
     * Without a wake-up accelerometer, readings stop whenever the phone sleeps, so a
     * partial wake lock keeps the CPU (not the screen) on. Renewed with a timeout so
     * it can never be left on by mistake.
     */
    private fun keepCpuAwake() {
        val lock = getSystemService(PowerManager::class.java)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VitQuest:steps")
            ?.apply { setReferenceCounted(false) }
            ?: return
        wakeLock = lock
        scope.launch {
            while (isActive) {
                lock.acquire(WAKE_LOCK_TIMEOUT_MS)
                delay(WAKE_LOCK_TIMEOUT_MS - WAKE_LOCK_RENEW_MARGIN_MS)
            }
        }
    }

    private suspend fun updateNotification() {
        var shown: Int? = -1
        while (scope.isActive) {
            val today = timeSource.today()
            val steps = stepsRepository.observeDailySteps(today, today).first()[today]?.steps
            if (steps != shown && StepTrackingNotifications.canPost(this)) {
                try {
                    NotificationManagerCompat.from(this)
                        .notify(StepTrackingNotifications.NOTIFICATION_ID, StepTrackingNotifications.build(this, steps))
                } catch (_: SecurityException) {
                    // Notification permission revoked; the service keeps counting.
                }
                shown = steps
            }
            delay(NOTIFICATION_UPDATE_MS)
        }
    }

    companion object {
        private const val WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L
        private const val WAKE_LOCK_RENEW_MARGIN_MS = 30 * 1000L
        private const val NOTIFICATION_UPDATE_MS = 30 * 1000L

        fun hasActivityPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

        /** Returns false when Android did not allow the start (for example from the background). */
        fun start(context: Context): Boolean {
            if (!hasActivityPermission(context)) return false
            return try {
                ContextCompat.startForegroundService(context, Intent(context, StepTrackingService::class.java))
                true
            } catch (_: IllegalStateException) {
                // ForegroundServiceStartNotAllowedException on Android 12+ when started from the background.
                false
            } catch (_: SecurityException) {
                false
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, StepTrackingService::class.java))
        }
    }
}

object StepTrackingNotifications {
    const val CHANNEL_ID = "step_tracking"
    const val NOTIFICATION_ID = 4_200

    fun createChannel(context: Context) {
        val localized = ReminderNotifications.localizedContext(context)
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, localized.getString(R.string.step_tracking_channel), NotificationManager.IMPORTANCE_LOW).apply {
                description = localized.getString(R.string.step_tracking_channel_description)
                setShowBadge(false)
            },
        )
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun build(context: Context, steps: Int?): Notification {
        val localized = ReminderNotifications.localizedContext(context)
        val text = if (steps == null) {
            localized.getString(R.string.step_tracking_notification_waiting)
        } else {
            val number = NumberFormat.getIntegerInstance(localized.resources.configuration.locales[0]).format(steps)
            localized.getString(R.string.step_tracking_notification_steps, number)
        }
        val open = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_vitquest)
            .setContentTitle(localized.getString(R.string.step_tracking_notification_title))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_vitquest)
                    .setContentTitle(localized.getString(R.string.step_tracking_notification_title))
                    .build(),
            )
            .setContentIntent(open)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
