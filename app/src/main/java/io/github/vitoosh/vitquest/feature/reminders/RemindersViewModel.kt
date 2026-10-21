package io.github.vitoosh.vitquest.feature.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.core.data.repository.ReminderRepository
import io.github.vitoosh.vitquest.core.data.repository.ReminderScheduler
import io.github.vitoosh.vitquest.core.domain.reminders.ReminderPlanner
import io.github.vitoosh.vitquest.core.model.ReminderSetting
import io.github.vitoosh.vitquest.core.model.ReminderType
import io.github.vitoosh.vitquest.core.model.ReminderWindow
import io.github.vitoosh.vitquest.core.time.TimeSource
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RemindersUiState(
    val settings: Map<ReminderType, ReminderSetting>,
    val window: ReminderWindow,
    /** Next planned time per enabled reminder, shown so the user can see the schedule. */
    val next: Map<ReminderType, ZonedDateTime?>,
)

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val timeSource: TimeSource,
) : ViewModel() {

    val uiState: StateFlow<RemindersUiState?> = combine(repository.settings, repository.window) { settings, window ->
        val now = timeSource.now().atZone(timeSource.zone())
        RemindersUiState(settings, window, settings.mapValues { (_, setting) -> ReminderPlanner.next(setting, window, now) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(setting: ReminderSetting) {
        viewModelScope.launch { repository.save(setting) }
    }

    fun saveWindow(window: ReminderWindow) {
        viewModelScope.launch { repository.saveWindow(window) }
    }

    /** After the notification permission changes, make sure schedules match the settings. */
    fun permissionChanged() {
        viewModelScope.launch { scheduler.rescheduleAll() }
    }
}
