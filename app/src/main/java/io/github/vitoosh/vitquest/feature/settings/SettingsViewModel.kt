package io.github.vitoosh.vitquest.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.core.data.backup.BackupRepository
import io.github.vitoosh.vitquest.core.data.backup.RestoreError
import io.github.vitoosh.vitquest.core.data.backup.RestoreResult
import io.github.vitoosh.vitquest.core.data.repository.UserDataRepository
import io.github.vitoosh.vitquest.core.data.repository.UserPreferencesRepository
import io.github.vitoosh.vitquest.core.model.ThemeMode
import io.github.vitoosh.vitquest.core.time.TimeSource
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userDataRepository: UserDataRepository,
    private val backupRepository: BackupRepository,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val _backupState = MutableStateFlow(BackupState())
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

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

    /** Suggested file name, dated so several backups can sit side by side. */
    fun backupFileName(): String = "vitquest-backup-${timeSource.today()}.json"

    fun exportBackup(uri: Uri) = runBackup {
        if (backupRepository.exportTo(uri)) BackupMessage.EXPORTED else BackupMessage.EXPORT_FAILED
    }

    fun restoreBackup(uri: Uri) = runBackup {
        when (val result = backupRepository.restoreFrom(uri)) {
            RestoreResult.Restored -> BackupMessage.RESTORED
            is RestoreResult.Failed -> when (result.error) {
                RestoreError.UNREADABLE -> BackupMessage.UNREADABLE
                RestoreError.NOT_A_BACKUP -> BackupMessage.NOT_A_BACKUP
                RestoreError.NEWER_APP -> BackupMessage.NEWER_APP
                RestoreError.DAMAGED -> BackupMessage.DAMAGED
            }
        }
    }

    fun backupMessageShown() {
        _backupState.value = _backupState.value.copy(message = null)
    }

    private fun runBackup(work: suspend () -> BackupMessage) {
        if (_backupState.value.working) return
        _backupState.value = BackupState(working = true)
        viewModelScope.launch { _backupState.value = BackupState(working = false, message = work()) }
    }
}

data class BackupState(val working: Boolean = false, val message: BackupMessage? = null)

enum class BackupMessage { EXPORTED, EXPORT_FAILED, RESTORED, UNREADABLE, NOT_A_BACKUP, NEWER_APP, DAMAGED }
