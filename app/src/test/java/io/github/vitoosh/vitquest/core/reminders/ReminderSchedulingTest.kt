package io.github.vitoosh.vitquest.core.reminders

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import io.github.vitoosh.vitquest.core.data.local.VitQuestDatabase
import io.github.vitoosh.vitquest.core.data.repository.OfflineReminderRepository
import io.github.vitoosh.vitquest.core.data.repository.ReminderRepository
import io.github.vitoosh.vitquest.core.model.ReminderSetting
import io.github.vitoosh.vitquest.core.model.ReminderType
import io.github.vitoosh.vitquest.core.model.ReminderWindow
import io.github.vitoosh.vitquest.testing.FixedTimeSource
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderSchedulingTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: VitQuestDatabase
    private lateinit var repository: ReminderRepository
    private lateinit var scheduler: WorkManagerReminderScheduler
    // Wednesday 21 October 2026, 09:00 in Jakarta.
    private val time = FixedTimeSource(Instant.parse("2026-10-21T02:00:00Z"))

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        database = Room.inMemoryDatabaseBuilder(context, VitQuestDatabase::class.java).allowMainThreadQueries().build()
        lateinit var repo: ReminderRepository
        scheduler = WorkManagerReminderScheduler(context, { repo }, time)
        repo = OfflineReminderRepository(database.reminderDao(), scheduler)
        repository = repo
    }

    @After
    fun tearDown() = database.close()

    private fun work(type: ReminderType): List<WorkInfo> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork(WorkManagerReminderScheduler.workName(type)).get()
            .filter { it.state == WorkInfo.State.ENQUEUED }

    @Test
    fun settingsArePersistedWithDefaults() = runTest {
        val settings = repository.settings.first()
        assertEquals(ReminderType.entries.toSet(), settings.keys)
        assertTrue(settings.values.none { it.enabled })
        assertEquals(30, settings.getValue(ReminderType.WATER).intervalMinutes)

        val workout = ReminderSetting(ReminderType.WORKOUT, enabled = true, time = LocalTime.of(18, 30), days = setOf(DayOfWeek.MONDAY))
        repository.save(workout)
        repository.saveWindow(ReminderWindow(LocalTime.of(8, 0), LocalTime.of(20, 0), LocalTime.of(12, 0), LocalTime.of(13, 0)))
        assertEquals(workout, repository.currentSettings().getValue(ReminderType.WORKOUT))
        assertEquals(LocalTime.of(12, 0), repository.window.first().quietStart)
    }

    @Test
    fun savingSchedulesExactlyOneWorkPerTypeAndDisablingCancelsIt() = runTest {
        repository.saveWindow(ReminderWindow(LocalTime.of(8, 0), LocalTime.of(20, 0)))
        val water = ReminderSetting(ReminderType.WATER, enabled = true)
        repository.save(water)
        repository.save(water.copy(intervalMinutes = 60))
        scheduler.ensureScheduled()
        scheduler.rescheduleAll()
        assertEquals(1, work(ReminderType.WATER).size)
        // Incomplete or off reminders are not scheduled at all.
        assertEquals(0, work(ReminderType.WORKOUT).size)

        repository.save(water.copy(enabled = false))
        assertEquals(0, work(ReminderType.WATER).size)
    }

    @Test
    fun intervalRemindersWaitForActiveHours() = runTest {
        repository.save(ReminderSetting(ReminderType.MOVEMENT, enabled = true))
        assertEquals(0, work(ReminderType.MOVEMENT).size)
        repository.saveWindow(ReminderWindow(LocalTime.of(7, 0), LocalTime.of(21, 0)))
        assertEquals(1, work(ReminderType.MOVEMENT).size)
    }
}
