package io.github.vitoosh.vitquest.feature.workouts.generator

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.core.data.repository.ProfileRepository
import io.github.vitoosh.vitquest.core.data.repository.WorkoutRepository
import io.github.vitoosh.vitquest.core.domain.habits.HabitStats
import io.github.vitoosh.vitquest.core.domain.workout.WorkoutEnergy
import io.github.vitoosh.vitquest.core.domain.workout.WorkoutGenerator
import io.github.vitoosh.vitquest.core.model.Equipment
import io.github.vitoosh.vitquest.core.model.FitnessExperience
import io.github.vitoosh.vitquest.core.model.UserProfile
import io.github.vitoosh.vitquest.core.model.WorkoutFocus
import io.github.vitoosh.vitquest.core.model.WorkoutLocation
import io.github.vitoosh.vitquest.core.model.WorkoutPlan
import io.github.vitoosh.vitquest.core.model.WorkoutRequest
import io.github.vitoosh.vitquest.core.time.TimeSource
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GeneratorUiState(
    val request: WorkoutRequest,
    val plan: WorkoutPlan,
    val todaySessions: Int,
    val todayActiveSeconds: Int,
    val weekWorkoutDays: Int,
    /** Rough energy estimate for the whole plan; null without a body weight. */
    val estimatedKcal: Double?,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkoutGeneratorViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    workoutRepository: WorkoutRepository,
    private val timeSource: TimeSource,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val request = MutableStateFlow(restoreRequest())
    private val today = MutableStateFlow(timeSource.today())

    init {
        if (request.value == null) {
            viewModelScope.launch {
                val profile = profileRepository.profile.first()
                if (request.value == null) setRequest(defaultRequest(profile))
            }
        }
    }

    val uiState: StateFlow<GeneratorUiState?> = today.flatMapLatest { date ->
        combine(
            request.filterNotNull(),
            workoutRepository.sessions(HabitStats.weekStart(date), date),
            profileRepository.profile,
        ) { req, sessions, profile ->
            val plan = WorkoutGenerator.generate(req)
            val todaySessions = sessions.filter { it.date == date }
            GeneratorUiState(
                request = req,
                plan = plan,
                todaySessions = todaySessions.size,
                todayActiveSeconds = todaySessions.sumOf { it.activeSeconds },
                weekWorkoutDays = sessions.map { it.date }.distinct().size,
                estimatedKcal = profile?.currentWeightKg?.let { kg ->
                    plan.steps.sumOf { WorkoutEnergy.kcal(it.exercise.met, kg, it.workSeconds) }
                },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun refreshToday() {
        today.value = timeSource.today()
    }

    fun setMinutes(minutes: Int) = change { it.copy(minutes = minutes) }
    fun setLevel(level: FitnessExperience) = change { it.copy(level = level) }
    fun setLocation(location: WorkoutLocation) = change { it.copy(location = location) }
    fun setFocus(focus: WorkoutFocus) = change { it.copy(focus = focus) }
    fun toggleEquipment(item: Equipment) = change {
        it.copy(equipment = if (item in it.equipment) it.equipment - item else it.equipment + item)
    }

    /** A different selection of exercises for the same settings. */
    fun shuffle() = change { it.copy(seed = Random.nextLong()) }

    private fun change(transform: (WorkoutRequest) -> WorkoutRequest) {
        request.update { current -> current?.let(transform)?.also(::save) }
    }

    private fun setRequest(value: WorkoutRequest) {
        request.value = value
        save(value)
    }

    private fun save(value: WorkoutRequest) {
        savedStateHandle[KEY_MINUTES] = value.minutes
        savedStateHandle[KEY_LEVEL] = value.level.name
        savedStateHandle[KEY_LOCATION] = value.location.name
        savedStateHandle[KEY_EQUIPMENT] = value.equipment.joinToString(",") { it.name }
        savedStateHandle[KEY_FOCUS] = value.focus.name
        savedStateHandle[KEY_SEED] = value.seed
    }

    private fun restoreRequest(): WorkoutRequest? {
        val minutes = savedStateHandle.get<Int>(KEY_MINUTES) ?: return null
        return WorkoutRoutes.request(
            minutes = minutes,
            level = savedStateHandle.get<String>(KEY_LEVEL),
            location = savedStateHandle.get<String>(KEY_LOCATION),
            equipment = savedStateHandle.get<String>(KEY_EQUIPMENT),
            focus = savedStateHandle.get<String>(KEY_FOCUS),
            seed = savedStateHandle.get<Long>(KEY_SEED) ?: 0L,
        )
    }

    private companion object {
        const val KEY_MINUTES = "minutes"
        const val KEY_LEVEL = "level"
        const val KEY_LOCATION = "location"
        const val KEY_EQUIPMENT = "equipment"
        const val KEY_FOCUS = "focus"
        const val KEY_SEED = "seed"

        fun defaultRequest(profile: UserProfile?): WorkoutRequest {
            val level = profile?.fitnessExperience ?: FitnessExperience.BEGINNER
            return WorkoutRequest(
                minutes = if (level == FitnessExperience.BEGINNER) 10 else 15,
                level = level,
                location = profile?.workoutLocation?.takeIf { it != WorkoutLocation.MIXED } ?: WorkoutLocation.HOME,
                equipment = profile?.equipment.orEmpty(),
                focus = WorkoutFocus.FULL_BODY,
                seed = Random.nextLong(),
            )
        }
    }
}

/** Converts a workout request to and from the player route's plain values. */
object WorkoutRoutes {
    fun equipmentText(equipment: Set<Equipment>): String = equipment.sortedBy { it.ordinal }.joinToString(",") { it.name }

    fun request(minutes: Int, level: String?, location: String?, equipment: String?, focus: String?, seed: Long) = WorkoutRequest(
        minutes = minutes.coerceIn(WorkoutGenerator.DURATIONS.first(), WorkoutGenerator.DURATIONS.last()),
        level = FitnessExperience.entries.firstOrNull { it.name == level } ?: FitnessExperience.BEGINNER,
        location = WorkoutLocation.entries.firstOrNull { it.name == location } ?: WorkoutLocation.HOME,
        equipment = equipment.orEmpty().split(",").mapNotNull { name -> Equipment.entries.firstOrNull { it.name == name } }.toSet(),
        focus = WorkoutFocus.entries.firstOrNull { it.name == focus } ?: WorkoutFocus.FULL_BODY,
        seed = seed,
    )
}
