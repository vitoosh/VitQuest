package io.github.vitoosh.vitquest.core.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.core.model.StepSource
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Creates a database exactly as version 1 shipped it (from the committed
 * schema JSON), then opens it with the current Room setup. Room validates
 * every table after migrating, so a wrong migration fails this test.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-test.db"

    @Before
    fun setUp() {
        context.deleteDatabase(name)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    private fun createVersion1Database() {
        val schema = JSONObject(File("schemas/${VitQuestDatabase::class.java.name}/1.json").readText())
            .getJSONObject("database")
        val callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                val entities = schema.getJSONArray("entities")
                for (i in 0 until entities.length()) {
                    val entity = entities.getJSONObject(i)
                    db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                }
                val setup = schema.getJSONArray("setupQueries")
                for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(callback).build(),
        )
        helper.writableDatabase.use { db ->
            db.insert(
                "user_profile",
                SQLiteDatabase.CONFLICT_REPLACE,
                ContentValues().apply {
                    put("id", 1)
                    put("age", 33)
                    put("current_weight_kg", 81.5)
                    put("equipment", "EXERCISE_MAT")
                    put("dietary_preferences", "")
                    put("updated_at_epoch_ms", 1L)
                },
            )
            db.insert(
                "goals",
                SQLiteDatabase.CONFLICT_REPLACE,
                ContentValues().apply {
                    put("id", 1)
                    put("daily_step_goal", 7_000)
                    put("daily_water_target_ml", 2_400)
                    put("water_serving_ml", 250)
                    put("updated_at_epoch_ms", 1L)
                },
            )
        }
        helper.close()
    }

    @Test
    fun migrating1To2KeepsProfileAndAddsStepTables() = runTest {
        createVersion1Database()

        val database = Room.databaseBuilder(context, VitQuestDatabase::class.java, name)
            .addMigrations(*DatabaseMigrations.ALL)
            .allowMainThreadQueries()
            .build()
        try {
            val profile = database.profileDao().observeProfile().first()
            assertEquals(33, profile?.age)
            assertEquals(7_000, database.profileDao().observeGoals().first()?.dailyStepGoal)

            database.stepsDao().upsert(listOf(DailyStepsEntity("2026-10-19", StepSource.MANUAL.name, 4_321, 1L)))
            assertEquals(4_321, database.stepsDao().getSteps("2026-10-19", StepSource.MANUAL.name))
        } finally {
            database.close()
        }
    }
}
