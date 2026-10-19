package io.github.vitoosh.vitquest.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.core.data.repository.UserDataRepository
import io.github.vitoosh.vitquest.core.data.repository.UserPreferencesRepository
import io.github.vitoosh.vitquest.core.model.ThemeMode
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userDataRepository: UserDataRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode?> = userPreferencesRepository.userPreferences
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch { userPreferencesRepository.setThemeMode(themeMode) }
    }

    /** Wipes local records; the app then returns to onboarding automatically. */
    fun deleteAllData() {
        viewModelScope.launch { userDataRepository.deleteAllUserData() }
    }
}
