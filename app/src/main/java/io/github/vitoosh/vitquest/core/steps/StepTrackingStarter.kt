package io.github.vitoosh.vitquest.core.steps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.github.vitoosh.vitquest.core.data.repository.UserPreferencesRepository
import io.github.vitoosh.vitquest.core.di.ApplicationScope
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Starts background step counting again after a reboot or an app update, if the user turned it on. */
@AndroidEntryPoint
class StepTrackingBootReceiver : BroadcastReceiver() {

    @Inject lateinit var preferences: UserPreferencesRepository

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        val appContext = context.applicationContext
        scope.launch {
            try {
                if (preferences.userPreferences.first().backgroundStepTracking) StepTrackingService.start(appContext)
            } finally {
                pending.finish()
            }
        }
    }
}
