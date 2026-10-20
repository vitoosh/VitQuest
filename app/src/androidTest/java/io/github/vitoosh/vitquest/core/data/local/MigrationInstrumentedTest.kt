package io.github.vitoosh.vitquest.core.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs every migration on the phone's own SQLite version, which can differ from the
 * one Robolectric uses. Run with `./gradlew connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class MigrationInstrumentedTest {

    private val name = "migration-instrumented.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), VitQuestDatabase::class.java)

    @Test
    fun everyStepFromVersion1KeepsTheProfile() {
        helper.createDatabase(name, 1).use { db ->
            db.execSQL(
                "INSERT INTO user_profile (id, age, current_weight_kg, equipment, dietary_preferences, updated_at_epoch_ms) " +
                    "VALUES (1, 33, 81.5, 'EXERCISE_MAT', '', 1)",
            )
        }
        (2..VitQuestDatabase.VERSION).forEach { version ->
            helper.runMigrationsAndValidate(name, version, true, *DatabaseMigrations.ALL).close()
        }
        val database = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), VitQuestDatabase::class.java, name)
            .addMigrations(*DatabaseMigrations.ALL)
            .build()
        try {
            database.openHelper.readableDatabase.query("SELECT age FROM user_profile WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals(33, cursor.getInt(0))
            }
        } finally {
            database.close()
        }
    }
}
