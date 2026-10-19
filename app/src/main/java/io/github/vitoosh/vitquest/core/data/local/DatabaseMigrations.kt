package io.github.vitoosh.vitquest.core.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Every schema change must add an explicit migration here (with a test).
 * Destructive fallback is intentionally not used so user data is never wiped
 * by an app update. SQL is copied from the exported schema in app/schemas/.
 */
object DatabaseMigrations {

    /** v2 (Phase 2): daily step history and the phone step-counter checkpoint. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `daily_steps` (`date` TEXT NOT NULL, `source` TEXT NOT NULL, " +
                    "`steps` INTEGER NOT NULL, `updated_at_epoch_ms` INTEGER NOT NULL, PRIMARY KEY(`date`, `source`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `step_sensor_state` (`id` INTEGER NOT NULL, `boot_count` INTEGER, " +
                    "`counter` INTEGER NOT NULL, `recorded_at_epoch_ms` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
