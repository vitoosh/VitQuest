package io.github.vitoosh.vitquest.core.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ReminderDao {

    @Query("SELECT * FROM reminder_settings")
    abstract fun observeSettings(): Flow<List<ReminderSettingEntity>>

    @Query("SELECT * FROM reminder_settings")
    abstract suspend fun getSettings(): List<ReminderSettingEntity>

    @Upsert
    abstract suspend fun upsertSetting(setting: ReminderSettingEntity)

    @Query("SELECT * FROM reminder_window WHERE id = 1")
    abstract fun observeWindow(): Flow<ReminderWindowEntity?>

    @Query("SELECT * FROM reminder_window WHERE id = 1")
    abstract suspend fun getWindow(): ReminderWindowEntity?

    @Upsert
    abstract suspend fun upsertWindow(window: ReminderWindowEntity)
}
