package io.github.vitoosh.vitquest.testing

import io.github.vitoosh.vitquest.core.data.health.HealthConnectAvailability
import io.github.vitoosh.vitquest.core.data.health.HealthConnectReadResult
import io.github.vitoosh.vitquest.core.data.health.HealthConnectStepsSource
import io.github.vitoosh.vitquest.core.data.repository.ProfileRepository
import io.github.vitoosh.vitquest.core.data.sensor.StepSensorSource
import io.github.vitoosh.vitquest.core.domain.steps.SensorReading
import io.github.vitoosh.vitquest.core.model.StepSourcePreference
import io.github.vitoosh.vitquest.core.time.TimeSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableSharedFlow
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

    override suspend fun updateDailyStepGoal(steps: Int) {
        goalsState.update { it?.copy(dailyStepGoal = steps) }
    }
}

class FakeUserPreferencesRepository : UserPreferencesRepository {
    val state = MutableStateFlow(UserPreferences())

    override val userPreferences: Flow<UserPreferences> = state

    override suspend fun setThemeMode(themeMode: ThemeMode) = state.update { it.copy(themeMode = themeMode) }

    override suspend fun setOnboardingCompleted(completed: Boolean) =
        state.update { it.copy(onboardingCompleted = completed) }

    override suspend fun setStepSourcePreference(preference: StepSourcePreference) =
        state.update { it.copy(stepSourcePreference = preference) }
}

class FakeHealthConnectStepsSource(
    var availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    var permissionGranted: Boolean = true,
    var result: HealthConnectReadResult = HealthConnectReadResult.Success(emptyMap()),
) : HealthConnectStepsSource {
    var reads = 0
    override val stepsPermissions: Set<String> = setOf("android.permission.health.READ_STEPS")
    override fun availability() = availability
    override suspend fun hasStepsPermission() = permissionGranted
    override suspend fun readDailySteps(from: LocalDate, to: LocalDate): HealthConnectReadResult {
        reads++
        return result
    }
}

class FakeStepSensorSource(
    override var isAvailable: Boolean = true,
    var permissionGranted: Boolean = true,
) : StepSensorSource {
    val emitted = MutableSharedFlow<SensorReading>()
    override fun hasPermission() = permissionGranted
    override fun readings(): Flow<SensorReading> = emitted
}

class FixedTimeSource(var instant: Instant, private val zoneId: ZoneId = ZoneId.of("Asia/Jakarta")) : TimeSource {
    override fun now(): Instant = instant
    override fun zone(): ZoneId = zoneId
}
