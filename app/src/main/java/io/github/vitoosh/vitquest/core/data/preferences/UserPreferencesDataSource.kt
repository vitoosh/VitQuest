package io.github.vitoosh.vitquest.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.vitoosh.vitquest.core.model.StepSourcePreference
import io.github.vitoosh.vitquest.core.model.ThemeMode
import io.github.vitoosh.vitquest.core.model.UserPreferences
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Lightweight app preferences. The UI language is deliberately not stored
 * here: Android's per-app language setting is its single source of truth.
 */
class UserPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val userPreferences: Flow<UserPreferences> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            UserPreferences(
                themeMode = prefs[Keys.THEME_MODE]
                    ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
                    ?: ThemeMode.SYSTEM,
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
                stepSourcePreference = prefs[Keys.STEP_SOURCE]
                    ?.let { stored -> StepSourcePreference.entries.firstOrNull { it.name == stored } }
                    ?: StepSourcePreference.AUTOMATIC,
            )
        }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setStepSourcePreference(preference: StepSourcePreference) {
        dataStore.edit { it[Keys.STEP_SOURCE] = preference.name }
    }

    /** Version of the bundled food and recipe data already loaded into the database. */
    suspend fun seedDataVersion(): Int =
        dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { it[Keys.SEED_VERSION] ?: 0 }
            .first()

    suspend fun setSeedDataVersion(version: Int) {
        dataStore.edit { it[Keys.SEED_VERSION] = version }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val STEP_SOURCE = stringPreferencesKey("step_source_preference")
        val SEED_VERSION = intPreferencesKey("seed_data_version")
    }
}
