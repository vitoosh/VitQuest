package io.github.vitoosh.vitquest

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import io.github.vitoosh.vitquest.core.data.repository.StepSensorTracker
import io.github.vitoosh.vitquest.core.data.seed.SeedDataLoader
import io.github.vitoosh.vitquest.core.di.ApplicationScope
import io.github.vitoosh.vitquest.core.reminders.ReminderNotifications
import io.github.vitoosh.vitquest.core.reminders.WorkManagerReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class VitQuestApplication : Application() {

    @Inject
    lateinit var stepSensorTracker: StepSensorTracker

    @Inject
    lateinit var seedDataLoader: SeedDataLoader

    @Inject
    lateinit var reminderScheduler: WorkManagerReminderScheduler

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // The step counter is read only while the app is in the foreground.
        ProcessLifecycleOwner.get().lifecycle.addObserver(stepSensorTracker)
        // Loads or updates the bundled food and recipe database; a no-op once current.
        applicationScope.launch { seedDataLoader.ensureLoaded() }
        // Channels are recreated each start so their names follow the current language.
        ReminderNotifications.createChannels(this)
        applicationScope.launch { reminderScheduler.ensureScheduled() }
    }
}
