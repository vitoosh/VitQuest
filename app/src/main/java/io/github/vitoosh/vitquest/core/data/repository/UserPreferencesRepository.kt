package io.github.vitoosh.vitquest.core.data.repository

import io.github.vitoosh.vitquest.core.data.preferences.UserPreferencesDataSource
import io.github.vitoosh.vitquest.core.model.StepSourcePreference
import io.github.vitoosh.vitquest.core.model.ThemeMode
import io.github.vitoosh.vitquest.core.model.UserPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val userPreferences: Flow<UserPreferences>

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setStepSourcePreference(preference: StepSourcePreference)

    suspend fun setBackgroundStepTracking(enabled: Boolean)
}

class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataSource: UserPreferencesDataSource,
) : UserPreferencesRepository {

    override val userPreferences: Flow<UserPreferences> = dataSource.userPreferences

    override suspend fun setThemeMode(themeMode: ThemeMode) = dataSource.setThemeMode(themeMode)

    override suspend fun setOnboardingCompleted(completed: Boolean) =
        dataSource.setOnboardingCompleted(completed)

    override suspend fun setStepSourcePreference(preference: StepSourcePreference) =
        dataSource.setStepSourcePreference(preference)

    override suspend fun setBackgroundStepTracking(enabled: Boolean) = dataSource.setBackgroundStepTracking(enabled)
}
