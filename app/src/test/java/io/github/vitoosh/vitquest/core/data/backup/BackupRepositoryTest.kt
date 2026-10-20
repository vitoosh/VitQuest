package io.github.vitoosh.vitquest.core.data.backup

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.core.data.local.VitQuestDatabase
import io.github.vitoosh.vitquest.core.data.local.toEntity
import io.github.vitoosh.vitquest.core.data.preferences.UserPreferencesDataSource
import io.github.vitoosh.vitquest.core.data.repository.ReminderScheduler
import io.github.vitoosh.vitquest.core.data.seed.SeedDataLoader
import io.github.vitoosh.vitquest.core.model.ActivityLevel
import io.github.vitoosh.vitquest.core.model.Goals
import io.github.vitoosh.vitquest.core.model.ReminderType
import io.github.vitoosh.vitquest.core.model.ThemeMode
import io.github.vitoosh.vitquest.core.model.UserProfile
import io.github.vitoosh.vitquest.testing.FixedTimeSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())
    private val prefsFile = File.createTempFile("backup", ".preferences_pb").apply { delete() }
    private val time = FixedTimeSource(Instant.parse("2026-10-21T03:00:00Z"))
    private lateinit var database: VitQuestDatabase
    private lateinit var preferences: UserPreferencesDataSource
    private lateinit var backups: LocalBackupRepository
    private var rescheduled = 0

    private val db: SupportSQLiteDatabase get() = database.openHelper.writableDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, VitQuestDatabase::class.java).allowMainThreadQueries().build()
        preferences = UserPreferencesDataSource(PreferenceDataStoreFactory.create(scope = scope) { prefsFile })
        val seeds = SeedDataLoader(context, database.foodDao(), database.recipeDao(), database.rewardDao(), preferences, time)
        val scheduler = object : ReminderScheduler {
            override suspend fun reschedule(type: ReminderType) = Unit
            override suspend fun rescheduleAll() {
                rescheduled++
            }
        }
        backups = LocalBackupRepository(context, database, preferences, seeds, scheduler, time)
        runBlocking { seeds.ensureLoaded() }
    }

    @After
    fun tearDown() {
        scope.cancel()
        database.close()
        prefsFile.delete()
    }

    private suspend fun recordSomeData() {
        database.profileDao().upsertProfileAndGoals(
            UserProfile(age = 34, currentWeightKg = 80.5, activityLevel = ActivityLevel.LIGHT).toEntity(1L),
            Goals(targetWeightKg = 74.0, dailyStepGoal = 7_000, dailyWaterTargetMl = 2_200, waterServingMl = 250).toEntity(1L),
        )
        listOf(
            "INSERT INTO weight_entries (id, date, weight_kg, note, updated_at_epoch_ms) VALUES (1, '2026-10-20', 80.5, 'pagi', 1)",
            "INSERT INTO habits (id, name, target_days_per_week, created_at_epoch_ms) VALUES (3, 'Tidur 7 jam', 5, 1)",
            "INSERT INTO habit_check_ins (habit_id, date) VALUES (3, '2026-10-20')",
            "INSERT INTO reward_transactions (currency, amount, reason, source_key, date, created_at_epoch_ms) VALUES ('XP', 50, 'WORKOUT', 'g:workout:1#1', '2026-10-20', 1)",
            "INSERT INTO budget_entries (id, date, kind, amount_rupiah, reward_id, note, created_at_epoch_ms) VALUES (1, '2026-10-20', 'SAVING', 25000, 1, NULL, 1)",
            "INSERT INTO reminder_settings (type, enabled, interval_minutes, time_minutes, days) VALUES ('WATER', 1, 30, NULL, '')",
            "INSERT INTO step_sensor_state (id, boot_count, counter, recorded_at_epoch_ms) VALUES (1, 4, 12000, 1)",
        ).forEach(db::execSQL)
        preferences.setThemeMode(ThemeMode.DARK)
        preferences.setMonthlyRewardBudget(300_000)
        preferences.setOnboardingCompleted(true)
    }

    private suspend fun exportToBytes(): ByteArray = ByteArrayOutputStream().also { backups.export(it) }.toByteArray()

    private suspend fun restore(text: String) = backups.restore(ByteArrayInputStream(text.toByteArray()))

    private fun backupTables() = BackupTables.read(db)

    @Test
    fun exportThenRestoreBringsBackEveryRecordAndSetting() = runTest {
        recordSomeData()
        val before = backupTables()
        val file = exportToBytes()
        assertTrue(String(file).startsWith("{\"app\":\"VitQuest\""))

        // Start over on "another phone": wipe everything, record something else.
        database.clearAllTables()
        preferences.clear()
        db.execSQL("INSERT INTO habits (id, name, target_days_per_week, created_at_epoch_ms) VALUES (9, 'Lain', 3, 1)")
        db.execSQL("INSERT INTO step_sensor_state (id, boot_count, counter, recorded_at_epoch_ms) VALUES (1, 1, 50, 2)")

        assertEquals(RestoreResult.Restored, backups.restore(ByteArrayInputStream(file)))
        assertEquals(before, backupTables())
        assertEquals(ThemeMode.DARK, preferences.userPreferences.first().themeMode)
        assertEquals(300_000L, preferences.monthlyRewardBudget.first())
        assertTrue(preferences.userPreferences.first().onboardingCompleted)
        assertEquals(1, rescheduled)
        // The step counter checkpoint belongs to this phone and is neither exported nor replaced.
        assertTrue("step_sensor_state" !in before)
        assertEquals(50L, db.query("SELECT counter FROM step_sensor_state").use { it.moveToFirst(); it.getLong(0) })
    }

    @Test
    fun filesThatCannotBeUsedChangeNothing() = runTest {
        recordSomeData()
        val before = backupTables()
        val good = String(exportToBytes())

        assertEquals(RestoreResult.Failed(RestoreError.NOT_A_BACKUP), restore("hello"))
        assertEquals(RestoreResult.Failed(RestoreError.NOT_A_BACKUP), restore("""{"app":"Other","format":1}"""))
        assertEquals(
            RestoreResult.Failed(RestoreError.NEWER_APP),
            restore(good.replace("\"databaseVersion\":${VitQuestDatabase.VERSION}", "\"databaseVersion\":${VitQuestDatabase.VERSION + 1}")),
        )
        // A database version that never existed has no schema to migrate from.
        assertEquals(
            RestoreResult.Failed(RestoreError.DAMAGED),
            restore(good.replace("\"databaseVersion\":${VitQuestDatabase.VERSION}", "\"databaseVersion\":0")),
        )
        // A check-in pointing at a habit that is not in the file breaks a foreign key.
        assertEquals(RestoreResult.Failed(RestoreError.DAMAGED), restore(good.replace("\"habit_id\":3", "\"habit_id\":404")))
        // A column this version does not know.
        assertEquals(RestoreResult.Failed(RestoreError.DAMAGED), restore(good.replace("\"weight_kg\":80.5", "\"weight_kg\":80.5,\"bmi\":1")))
        // Truncated file.
        assertEquals(RestoreResult.Failed(RestoreError.NOT_A_BACKUP), restore(good.dropLast(10)))

        assertEquals(before, backupTables())
        assertEquals(ThemeMode.DARK, preferences.userPreferences.first().themeMode)
        assertEquals(0, rescheduled)
    }

    @Test
    fun backupFromAnOlderVersionIsMigratedOnRestore() = runTest {
        // A version 5 backup still has Reward Credits, the old rewards table and redemptions.
        val v5 = """
            {"app":"VitQuest","format":1,"databaseVersion":5,"exportedAt":"2026-10-01T00:00:00Z",
             "preferences":{"themeMode":"LIGHT","onboardingCompleted":true,"seedDataVersion":2},
             "tables":{
               "habits":[{"id":1,"name":"Jalan pagi","target_days_per_week":4,"created_at_epoch_ms":1}],
               "reward_transactions":[
                 {"id":1,"currency":"XP","amount":50,"reason":"WORKOUT","source_key":"g:workout:1#1","date":"2026-09-30","created_at_epoch_ms":1},
                 {"id":2,"currency":"CREDIT","amount":5,"reason":"WORKOUT","source_key":"g:workout:1#1","date":"2026-09-30","created_at_epoch_ms":1}],
               "rewards":[{"id":7,"seed_key":null,"name":"Buku","description":null,"type":"PHYSICAL","credit_cost":120,"price_rupiah":95000,"wishlist":1,"created_at_epoch_ms":1}],
               "redemptions":[{"id":1,"request_id":"a","reward_id":7,"reward_name":"Buku","credit_cost":120,"redeemed_at_epoch_ms":2,"date":"2026-09-30","status":"REDEEMED"}]
             }}
        """.trimIndent()

        assertEquals(RestoreResult.Restored, restore(v5))
        val dao = database.rewardDao()
        assertEquals(50, dao.observeXp().first())
        assertEquals(listOf("Buku"), dao.observeRewards().first().map { it.name })
        assertEquals(listOf("a"), dao.observeHistory().first().map { it.requestId })
        assertEquals("Jalan pagi", database.habitDao().observeHabits().first().single().habit.name)
        assertEquals(ThemeMode.LIGHT, preferences.userPreferences.first().themeMode)
        // The scratch database used for migrating is cleaned up.
        assertTrue(!context.getDatabasePath("vitquest-restore-scratch.db").exists())
    }
}
