package io.github.vitoosh.vitquest.core.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.vitoosh.vitquest.core.model.StepSourcePreference
import io.github.vitoosh.vitquest.core.model.ThemeMode
import io.github.vitoosh.vitquest.core.model.UserPreferences
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UserPreferencesDataSourceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
    }

    /** Waits until every DataStore has released its file, so the next instance can open it. */
    private suspend fun closeAll() {
        scopes.forEach { it.coroutineContext.job.cancelAndJoin() }
        scopes.clear()
    }

    private fun dataSource(file: File): UserPreferencesDataSource {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also(scopes::add)
        return UserPreferencesDataSource(PreferenceDataStoreFactory.create(scope = scope) { file })
    }

    @Test
    fun defaultsFollowSystemThemeAndRequireOnboarding() = runTest {
        val source = dataSource(File(tempFolder.root, "defaults.preferences_pb"))
        assertEquals(UserPreferences(ThemeMode.SYSTEM, onboardingCompleted = false), source.userPreferences.first())
    }

    @Test
    fun themeAndOnboardingArePersistedAcrossInstances() = runTest {
        val file = File(tempFolder.root, "persist.preferences_pb")
        val first = dataSource(file)
        first.setThemeMode(ThemeMode.DARK)
        first.setOnboardingCompleted(true)
        closeAll()

        // A new DataStore instance on the same file simulates an app restart.
        val reopened = dataSource(file)
        assertEquals(UserPreferences(ThemeMode.DARK, onboardingCompleted = true), reopened.userPreferences.first())
    }

    @Test
    fun stepSourcePreferenceIsPersisted() = runTest {
        val file = File(tempFolder.root, "steps.preferences_pb")
        dataSource(file).setStepSourcePreference(StepSourcePreference.PHONE_SENSOR)
        closeAll()

        assertEquals(StepSourcePreference.PHONE_SENSOR, dataSource(file).userPreferences.first().stepSourcePreference)
    }

    @Test
    fun clearResetsToDefaults() = runTest {
        val source = dataSource(File(tempFolder.root, "clear.preferences_pb"))
        source.setThemeMode(ThemeMode.LIGHT)
        source.setOnboardingCompleted(true)

        source.clear()

        assertEquals(UserPreferences(), source.userPreferences.first())
    }
}
