package io.github.vitoosh.vitquest.core.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.vitoosh.vitquest.core.data.repository.BodyRepository
import io.github.vitoosh.vitquest.core.data.repository.HabitRepository
import io.github.vitoosh.vitquest.core.data.repository.ProfileRepository
import io.github.vitoosh.vitquest.core.data.repository.ReminderRepository
import io.github.vitoosh.vitquest.core.data.repository.ReminderScheduler
import io.github.vitoosh.vitquest.core.domain.reminders.ReminderPlanner
import io.github.vitoosh.vitquest.core.model.ReminderType
import io.github.vitoosh.vitquest.core.time.TimeSource
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** Whether today's goal for a reminder is already met, plus numbers for the message. */
data class ReminderStatus(val goalReached: Boolean, val content: ReminderContent)

@Singleton
class ReminderGoalChecker @Inject constructor(
    private val habitRepository: HabitRepository,
    private val bodyRepository: BodyRepository,
    private val profileRepository: ProfileRepository,
    private val timeSource: TimeSource,
) {
    suspend fun status(type: ReminderType): ReminderStatus {
        val today = timeSource.today()
        val record = habitRepository.dayRecords(today, today).first()[today]
        val goals = profileRepository.goals.first()
        return when (type) {
            ReminderType.WATER -> {
                val target = goals?.dailyWaterTargetMl
                val water = record?.waterMl ?: 0
                ReminderStatus(target != null && water >= target, ReminderContent(type, water, target))
            }
            ReminderType.MOVEMENT -> {
                val goal = goals?.dailyStepGoal
                val steps = record?.steps
                ReminderStatus(goal != null && steps != null && steps >= goal, ReminderContent(type, steps, goal))
            }
            ReminderType.WORKOUT -> ReminderStatus((record?.workoutCount ?: 0) > 0, ReminderContent(type))
            ReminderType.FOOD_LOG -> ReminderStatus(record?.foodLogged == true, ReminderContent(type))
            ReminderType.WEIGHT -> {
                // A weigh-in within the last 6 days counts for this week's reminder.
                val recent = bodyRepository.weights().first().any { !it.date.isBefore(today.minusDays(6)) }
                ReminderStatus(recent, ReminderContent(type))
            }
        }
    }
}

/**
 * One-time work per reminder type. Each run decides whether to notify, then
 * queues the next run; WorkManager keeps the queue across reboots.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun reminderRepository(): ReminderRepository
        fun goalChecker(): ReminderGoalChecker
        fun scheduler(): WorkManagerReminderScheduler
        fun timeSource(): TimeSource
    }

    override suspend fun doWork(): Result {
        val type = inputData.getString(KEY_TYPE)?.let { name -> ReminderType.entries.firstOrNull { it.name == name } } ?: return Result.success()
        val deps = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)
        val setting = deps.reminderRepository().currentSettings().getValue(type)
        if (!setting.enabled) return Result.success()
        val window = deps.reminderRepository().currentWindow()
        val now = deps.timeSource().now().atZone(deps.timeSource().zone())
        val status = deps.goalChecker().status(type)

        // Work can run late (Doze, battery savers), so re-check the hours before showing anything.
        val allowedNow = !ReminderPlanner.isQuiet(now, window) &&
            (!type.usesInterval || ReminderPlanner.inRange(now.toLocalTime(), window.activeStart!!, window.activeEnd!!))
        if (!status.goalReached && allowedNow) ReminderNotifications.show(applicationContext, status.content)

        ReminderPlanner.next(setting, window, now, skipRestOfToday = status.goalReached)?.let { next ->
            deps.scheduler().enqueue(type, now, next, ExistingWorkPolicy.APPEND_OR_REPLACE)
        }
        return Result.success()
    }

    companion object {
        const val KEY_TYPE = "type"
    }
}

@Singleton
class WorkManagerReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderRepository: dagger.Lazy<ReminderRepository>,
    private val timeSource: TimeSource,
) : ReminderScheduler {

    override suspend fun reschedule(type: ReminderType) = schedule(type, ExistingWorkPolicy.REPLACE)

    override suspend fun rescheduleAll() = ReminderType.entries.forEach { schedule(it, ExistingWorkPolicy.REPLACE) }

    /** On app start: keeps pending reminders as they are and only fills in missing ones. */
    suspend fun ensureScheduled() = ReminderType.entries.forEach { schedule(it, ExistingWorkPolicy.KEEP) }

    private suspend fun schedule(type: ReminderType, policy: ExistingWorkPolicy) {
        val repository = reminderRepository.get()
        val setting = repository.currentSettings().getValue(type)
        val now = timeSource.now().atZone(timeSource.zone())
        val next = ReminderPlanner.next(setting, repository.currentWindow(), now)
        if (next == null) {
            workManager()?.cancelUniqueWork(workName(type))
        } else {
            enqueue(type, now, next, policy)
        }
    }

    fun enqueue(type: ReminderType, now: ZonedDateTime, at: ZonedDateTime, policy: ExistingWorkPolicy) {
        val delay = Duration.between(now, at).toMillis().coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putString(ReminderWorker.KEY_TYPE, type.name).build())
            .addTag(TAG)
            .build()
        // A unique name per type means a reminder can never be scheduled twice.
        workManager()?.enqueueUniqueWork(workName(type), policy, request)
    }

    private fun workManager(): WorkManager? = try {
        WorkManager.getInstance(context)
    } catch (_: IllegalStateException) {
        // Not initialised (for example in some test environments); there is nothing to schedule with.
        null
    }

    companion object {
        const val TAG = "vitquest-reminder"
        fun workName(type: ReminderType) = "reminder-${type.name.lowercase()}"
    }
}
