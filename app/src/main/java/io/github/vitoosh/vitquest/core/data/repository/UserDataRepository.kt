package io.github.vitoosh.vitquest.core.data.repository

import io.github.vitoosh.vitquest.core.data.local.VitQuestDatabase
import io.github.vitoosh.vitquest.core.data.preferences.UserPreferencesDataSource
import io.github.vitoosh.vitquest.core.data.seed.SeedDataLoader
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
    private val seedDataLoader: SeedDataLoader,
) : UserDataRepository {

    override suspend fun deleteAllUserData() {
        withContext(Dispatchers.IO) { database.clearAllTables() }
        // Clearing preferences also resets onboarding, which returns the app to the welcome flow.
        preferences.clear()
        // The bundled food and recipe database is not personal data, so restore it right away.
        seedDataLoader.ensureLoaded()
    }
}
