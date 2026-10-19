package io.github.vitoosh.vitquest.core.data.repository

import io.github.vitoosh.vitquest.core.data.local.ProfileDao
import io.github.vitoosh.vitquest.core.data.local.toEntity
import io.github.vitoosh.vitquest.core.data.local.toModel
import io.github.vitoosh.vitquest.core.model.Goals
import io.github.vitoosh.vitquest.core.model.UserProfile
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ProfileRepository {
    /** Emits null until the user has saved a profile. */
    val profile: Flow<UserProfile?>

    /** Emits null until the user has saved goals. */
    val goals: Flow<Goals?>

    suspend fun saveProfileAndGoals(profile: UserProfile, goals: Goals)
}

class OfflineProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val clock: Clock,
) : ProfileRepository {

    override val profile: Flow<UserProfile?> = profileDao.observeProfile().map { it?.toModel() }

    override val goals: Flow<Goals?> = profileDao.observeGoals().map { it?.toModel() }

    override suspend fun saveProfileAndGoals(profile: UserProfile, goals: Goals) {
        val now = clock.millis()
        profileDao.upsertProfileAndGoals(profile.toEntity(now), goals.toEntity(now))
    }
}
