package io.github.vitoosh.vitquest

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import io.github.vitoosh.vitquest.core.data.repository.StepSensorTracker
import javax.inject.Inject

@HiltAndroidApp
class VitQuestApplication : Application() {

    @Inject
    lateinit var stepSensorTracker: StepSensorTracker

    override fun onCreate() {
        super.onCreate()
        // The step counter is read only while the app is in the foreground.
        ProcessLifecycleOwner.get().lifecycle.addObserver(stepSensorTracker)
    }
}
