package io.github.vitoosh.vitquest.core.model

import java.time.Instant
import java.time.LocalDate

/** Built-in text in both app languages; the UI picks one with `localized`. */
data class Bilingual(val id: String, val en: String)

/** What an exercise is mainly used for, which decides where the generator places it. */
enum class ExerciseKind { STRENGTH, CARDIO, MOBILITY, STRETCH }

/** Movement families the generator balances across a session. */
enum class MovementPattern { SQUAT, LUNGE, HINGE, PUSH, PULL, CORE, CALF, CARDIO, MOBILITY, STRETCH }

enum class Muscle {
    CHEST, SHOULDERS, TRICEPS, BICEPS, UPPER_BACK, LOWER_BACK, CORE,
    GLUTES, QUADS, HAMSTRINGS, CALVES, HIP_FLEXORS, HEART_LUNGS,
}

/** Recommended amount of one set. */
sealed interface ExerciseDose {
    data class Reps(val min: Int, val max: Int, val perSide: Boolean = false) : ExerciseDose
    data class Hold(val seconds: Int, val perSide: Boolean = false) : ExerciseDose
}

data class Exercise(
    /** Stable key stored in workout history. Never rename a key once released. */
    val key: String,
    val name: Bilingual,
    val description: Bilingual,
    val steps: List<Bilingual>,
    val modifications: List<Bilingual>,
    val safety: List<Bilingual>,
    val kind: ExerciseKind,
    val pattern: MovementPattern,
    val level: FitnessExperience,
    val muscles: List<Muscle>,
    /** Places where the exercise is practical; office excludes floor work. */
    val locations: Set<WorkoutLocation>,
    val requiredEquipment: Set<Equipment> = emptySet(),
    val recommendedEquipment: Set<Equipment> = emptySet(),
    val dose: ExerciseDose,
    /** Approximate metabolic equivalent, only used for clearly labelled estimates. */
    val met: Double,
    /** Null for everyday movements (walking) that need no illustration. */
    val animation: ExerciseAnimation?,
    /** False for activities that do not fit timed intervals, such as a walk. */
    val inGenerator: Boolean = true,
)

// ---- Workout plans (generated, not stored) ----

enum class WorkoutFocus { FULL_BODY, UPPER, LOWER, CORE, CARDIO, MOBILITY }

enum class WorkoutBlock { WARM_UP, MAIN, COOL_DOWN }

data class WorkoutRequest(
    val minutes: Int,
    val level: FitnessExperience,
    val location: WorkoutLocation,
    val equipment: Set<Equipment>,
    val focus: WorkoutFocus,
    /** Same request and seed always give the same plan, so a plan can be rebuilt from its route. */
    val seed: Long,
)

data class WorkoutStep(
    val exercise: Exercise,
    val block: WorkoutBlock,
    val workSeconds: Int,
    /** Rest after this step; 0 for the last step. */
    val restSeconds: Int,
    val round: Int,
)

data class WorkoutPlan(val request: WorkoutRequest, val steps: List<WorkoutStep>) {
    val totalSeconds: Int get() = steps.sumOf { it.workSeconds + it.restSeconds }
    val exerciseCount: Int get() = steps.map { it.exercise.key }.distinct().size
}

// ---- Stored history ----

enum class WorkoutSource { PLAYER, MANUAL }

enum class WorkoutIntensity { LIGHT, MODERATE, VIGOROUS }

enum class StepStatus { COMPLETED, SKIPPED, NOT_REACHED }

data class WorkoutSessionExercise(
    val exerciseKey: String,
    val name: Bilingual,
    val plannedSeconds: Int,
    val workedSeconds: Int,
    val status: StepStatus,
)

data class WorkoutSession(
    val id: Long,
    val date: LocalDate,
    val startedAt: Instant,
    val endedAt: Instant,
    val source: WorkoutSource,
    val focus: WorkoutFocus?,
    val level: FitnessExperience?,
    val location: WorkoutLocation?,
    val intensity: WorkoutIntensity?,
    val plannedSeconds: Int,
    val activeSeconds: Int,
    val exercisesCompleted: Int,
    val exercisesTotal: Int,
    /** True when every step was finished rather than the session being ended early. */
    val completed: Boolean,
    val estimatedKcal: Double?,
    val note: String?,
    val exercises: List<WorkoutSessionExercise> = emptyList(),
)

// ---- Habits ----

/** Habits derived from other trackers; they are corrected by fixing the underlying records. */
enum class BuiltInHabit(val targetDaysPerWeek: Int) {
    MOVEMENT(5),
    WORKOUT(3),
    HYDRATION(5),
    FOOD_LOG(5),
}

data class CustomHabit(
    val id: Long,
    val name: String,
    val targetDaysPerWeek: Int,
    val checkIns: Set<LocalDate>,
)
