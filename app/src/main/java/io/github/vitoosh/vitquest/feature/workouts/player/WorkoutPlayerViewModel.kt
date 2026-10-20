package io.github.vitoosh.vitquest.feature.workouts.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.core.data.repository.ProfileRepository
import io.github.vitoosh.vitquest.core.data.repository.WorkoutRepository
import io.github.vitoosh.vitquest.core.domain.workout.PlayerPhase
import io.github.vitoosh.vitquest.core.domain.workout.PlayerState
import io.github.vitoosh.vitquest.core.domain.workout.WorkoutEnergy
import io.github.vitoosh.vitquest.core.domain.workout.WorkoutGenerator
import io.github.vitoosh.vitquest.core.domain.workout.WorkoutPlayerEngine
import io.github.vitoosh.vitquest.core.domain.workout.WorkoutSummary
import io.github.vitoosh.vitquest.core.model.WorkoutPlan
import io.github.vitoosh.vitquest.core.time.TimeSource
import io.github.vitoosh.vitquest.feature.workouts.generator.WorkoutRoutes
import io.github.vitoosh.vitquest.ui.navigation.WorkoutPlayerRoute
import java.time.Instant
import javax.inject.Inject
import kotlin.time.TimeSource as MonotonicClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/** A finished session as shown on the summary screen. */
data class FinishedWorkout(val summary: WorkoutSummary, val saved: Boolean)

/**
 * Runs the workout player. The plan is rebuilt from the route (the generator
 * is deterministic) and the player state survives process death through
 * the saved state, so a long session is never lost to a phone call.
 */
@HiltViewModel
class WorkoutPlayerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val profileRepository: ProfileRepository,
    timeSource: TimeSource,
) : ViewModel() {

    val plan: WorkoutPlan = savedStateHandle.toRoute<WorkoutPlayerRoute>().let { route ->
        WorkoutGenerator.generate(WorkoutRoutes.request(route.minutes, route.level, route.location, route.equipment, route.focus, route.seed))
    }

    private val startedAt: Instant = savedStateHandle.get<Long>(KEY_STARTED)?.let(Instant::ofEpochMilli)
        ?: timeSource.now().also { savedStateHandle[KEY_STARTED] = it.toEpochMilli() }

    private val _state = MutableStateFlow(restoreState() ?: WorkoutPlayerEngine.start(plan))
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _finished = MutableStateFlow<FinishedWorkout?>(null)
    val finished: StateFlow<FinishedWorkout?> = _finished.asStateFlow()

    init {
        viewModelScope.launch {
            var mark = MonotonicClock.Monotonic.markNow()
            while (isActive) {
                delay(TICK_MS)
                val elapsed = mark.elapsedNow().inWholeMilliseconds
                mark = MonotonicClock.Monotonic.markNow()
                update { WorkoutPlayerEngine.tick(it, plan, elapsed) }
            }
        }
        if (_state.value.phase == PlayerPhase.FINISHED) viewModelScope.launch { saveOnce() }
    }

    fun togglePause() = update(WorkoutPlayerEngine::togglePause)
    fun skip() = update { WorkoutPlayerEngine.skip(it, plan) }
    fun complete() = update { WorkoutPlayerEngine.complete(it, plan) }
    fun previous() = update { WorkoutPlayerEngine.previous(it, plan) }
    fun extendRest() = update(WorkoutPlayerEngine::extendRest)
    fun finishEarly() = update(WorkoutPlayerEngine::finishEarly)

    /** True when leaving now would throw away exercise time. */
    fun hasProgress(): Boolean = _state.value.phase != PlayerPhase.FINISHED && _state.value.workedMs.any { it > 0 }

    private fun update(transform: (PlayerState) -> PlayerState) {
        val before = _state.value
        _state.update(transform)
        val after = _state.value
        if (after != before) savedStateHandle[KEY_STATE] = json.encodeToString(PlayerState.serializer(), after)
        if (before.phase != PlayerPhase.FINISHED && after.phase == PlayerPhase.FINISHED) viewModelScope.launch { saveOnce() }
    }

    private suspend fun saveOnce() {
        val state = _state.value
        val weight = profileRepository.profile.first()?.currentWeightKg
        val summary = WorkoutEnergy.summarize(plan, state, weight)
        if (savedStateHandle.get<Boolean>(KEY_SAVED) == true) {
            _finished.value = FinishedWorkout(summary, saved = true)
            return
        }
        // Nothing done at all (ended straight away) is not worth a history entry.
        if (summary.activeSeconds == 0) {
            _finished.value = FinishedWorkout(summary, saved = false)
            return
        }
        savedStateHandle[KEY_SAVED] = true
        workoutRepository.savePlayerSession(plan, state, startedAt, weight)
        _finished.value = FinishedWorkout(summary, saved = true)
    }

    private fun restoreState(): PlayerState? = savedStateHandle.get<String>(KEY_STATE)?.let { text ->
        runCatching { json.decodeFromString(PlayerState.serializer(), text) }.getOrNull()
            ?.takeIf { it.workedMs.size == plan.steps.size }
            // Coming back after the app was closed: wait for the user instead of running on.
            ?.let { if (it.phase == PlayerPhase.FINISHED) it else it.copy(paused = true) }
    }

    private companion object {
        const val TICK_MS = 200L
        const val KEY_STATE = "player_state"
        const val KEY_STARTED = "player_started"
        const val KEY_SAVED = "player_saved"
        val json = Json { ignoreUnknownKeys = true }
    }
}
