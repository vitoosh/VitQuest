package io.github.vitoosh.vitquest.testing

import io.github.vitoosh.vitquest.core.data.repository.ProfileRepository
import io.github.vitoosh.vitquest.core.data.repository.UserPreferencesRepository
import io.github.vitoosh.vitquest.core.model.Goals
import io.github.vitoosh.vitquest.core.model.ThemeMode
import io.github.vitoosh.vitquest.core.model.UserPreferences
import io.github.vitoosh.vitquest.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeProfileRepository : ProfileRepository {
    val profileState = MutableStateFlow<UserProfile?>(null)
    val goalsState = MutableStateFlow<Goals?>(null)
    var failSaves = false

    override val profile: Flow<UserProfile?> = profileState
    override val goals: Flow<Goals?> = goalsState

    override suspend fun saveProfileAndGoals(profile: UserProfile, goals: Goals) {
        if (failSaves) throw java.io.IOException("Disk full")
        profileState.value = profile
        goalsState.value = goals
    }
}

class FakeUserPreferencesRepository : UserPreferencesRepository {
    val state = MutableStateFlow(UserPreferences())

    override val userPreferences: Flow<UserPreferences> = state

    override suspend fun setThemeMode(themeMode: ThemeMode) = state.update { it.copy(themeMode = themeMode) }

    override suspend fun setOnboardingCompleted(completed: Boolean) =
        state.update { it.copy(onboardingCompleted = completed) }
}
