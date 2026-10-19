package io.github.vitoosh.vitquest.core.data.local

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the rules that keep app updates from losing user data. */
class DatabaseSchemaTest {

    @Test
    fun everyVersionUpgradeHasAMigration() {
        val covered = DatabaseMigrations.ALL.map { it.startVersion to it.endVersion }.toSet()
        val required = (1 until VitQuestDatabase.VERSION).map { it to it + 1 }.toSet()
        assertEquals(required, covered)
    }

    @Test
    fun schemaForEveryVersionIsExportedAndCommitted() {
        // Unit tests run with the module directory as the working directory.
        val schemaDir = File("schemas/${VitQuestDatabase::class.java.name}")
        (1..VitQuestDatabase.VERSION).forEach { version ->
            assertTrue("Missing schema $version.json", File(schemaDir, "$version.json").isFile)
        }
    }
}
