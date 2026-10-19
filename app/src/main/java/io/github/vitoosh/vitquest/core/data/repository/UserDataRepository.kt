package io.github.vitoosh.vitquest.core.data.repository

import io.github.vitoosh.vitquest.core.data.local.VitQuestDatabase
import io.github.vitoosh.vitquest.core.data.preferences.UserPreferencesDataSource
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Handles the "delete all my data" request from Settings. */
interface UserDataRepository {
    suspend fun deleteAllUserData()
}

class LocalUserDataRepository @Inject constructor(
    private val database: VitQuestDatabase,
    private val preferences: UserPreferencesDataSource,
) : UserDataRepository {

    override suspend fun deleteAllUserData() {
        withContext(Dispatchers.IO) { database.clearAllTables() }
        // Clearing preferences also resets onboarding, which returns the app to the welcome flow.
        preferences.clear()
    }
}
