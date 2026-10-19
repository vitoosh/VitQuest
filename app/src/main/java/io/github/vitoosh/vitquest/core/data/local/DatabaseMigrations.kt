package io.github.vitoosh.vitquest.core.data.local

import androidx.room.migration.Migration

/**
 * Every schema change must add an explicit migration here (with a test).
 * Destructive fallback is intentionally not used so user data is never wiped
 * by an app update.
 */
object DatabaseMigrations {
    val ALL: Array<Migration> = arrayOf()
}
