package io.github.vitoosh.vitquest.core.data.repository

import io.github.vitoosh.vitquest.core.data.local.ReminderDao
import io.github.vitoosh.vitquest.core.data.local.toEntity
import io.github.vitoosh.vitquest.core.data.local.toModel
import io.github.vitoosh.vitquest.core.model.ReminderSetting
import io.github.vitoosh.vitquest.core.model.ReminderType
import io.github.vitoosh.vitquest.core.model.ReminderWindow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Schedules or cancels the background work behind reminders. */
interface ReminderScheduler {
    suspend fun reschedule(type: ReminderType)
    suspend fun rescheduleAll()
}

interface ReminderRepository {
    /** Every type, with defaults for types the user has not touched yet. */
    val settings: Flow<Map<ReminderType, ReminderSetting>>
    val window: Flow<ReminderWindow>
    suspend fun currentSettings(): Map<ReminderType, ReminderSetting>
    suspend fun currentWindow(): ReminderWindow

    /** Saves and immediately reschedules, so there is never a stale or duplicate schedule. */
    suspend fun save(setting: ReminderSetting)
    suspend fun saveWindow(window: ReminderWindow)
}

@Singleton
class OfflineReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao,
    private val scheduler: ReminderScheduler,
) : ReminderRepository {

    override val settings: Flow<Map<ReminderType, ReminderSetting>> =
        reminderDao.observeSettings().map { rows -> withDefaults(rows.mapNotNull { it.toModel() }) }

    override val window: Flow<ReminderWindow> = reminderDao.observeWindow().map { it.toModel() }

    override suspend fun currentSettings() = withDefaults(reminderDao.getSettings().mapNotNull { it.toModel() })

    override suspend fun currentWindow() = reminderDao.getWindow().toModel()

    override suspend fun save(setting: ReminderSetting) {
        reminderDao.upsertSetting(setting.toEntity())
        scheduler.reschedule(setting.type)
    }

    override suspend fun saveWindow(window: ReminderWindow) {
        reminderDao.upsertWindow(window.toEntity())
        scheduler.rescheduleAll()
    }

    private fun withDefaults(stored: List<ReminderSetting>): Map<ReminderType, ReminderSetting> {
        val byType = stored.associateBy { it.type }
        return ReminderType.entries.associateWith { byType[it] ?: ReminderSetting(it) }
    }
}
