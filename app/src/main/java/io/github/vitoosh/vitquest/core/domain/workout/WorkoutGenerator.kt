package io.github.vitoosh.vitquest.core.domain.workout

import io.github.vitoosh.vitquest.core.model.Equipment
import io.github.vitoosh.vitquest.core.model.Exercise
import io.github.vitoosh.vitquest.core.model.ExerciseKind
import io.github.vitoosh.vitquest.core.model.FitnessExperience
import io.github.vitoosh.vitquest.core.model.MovementPattern
import io.github.vitoosh.vitquest.core.model.WorkoutBlock
import io.github.vitoosh.vitquest.core.model.WorkoutFocus
import io.github.vitoosh.vitquest.core.model.WorkoutLocation
import io.github.vitoosh.vitquest.core.model.WorkoutPlan
import io.github.vitoosh.vitquest.core.model.WorkoutRequest
import io.github.vitoosh.vitquest.core.model.WorkoutStep
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Builds a session of warm-up, a circuit, and cool-down that fits the chosen
 * duration, level, place and equipment. Deterministic for a given request,
 * so the player can rebuild the same plan from its route.
 */
object WorkoutGenerator {

    val DURATIONS = listOf(5, 10, 15, 20, 30)

    private const val WARM_UP_WORK = 30
    private const val COOL_DOWN_WORK = 40
    private const val TRANSITION_REST = 5
    private const val MOBILITY_MAIN_WORK = 40
    private const val MOBILITY_MAIN_REST = 10

    private val PATTERNS = mapOf(
        WorkoutFocus.FULL_BODY to listOf(
            MovementPattern.SQUAT, MovementPattern.PUSH, MovementPattern.CORE, MovementPattern.HINGE,
            MovementPattern.CARDIO, MovementPattern.LUNGE, MovementPattern.PULL, MovementPattern.CALF,
        ),
        WorkoutFocus.UPPER to listOf(MovementPattern.PUSH, MovementPattern.PULL, MovementPattern.CORE, MovementPattern.PUSH, MovementPattern.PULL, MovementPattern.CORE),
        WorkoutFocus.LOWER to listOf(MovementPattern.SQUAT, MovementPattern.HINGE, MovementPattern.LUNGE, MovementPattern.CALF, MovementPattern.SQUAT, MovementPattern.HINGE),
        WorkoutFocus.CORE to listOf(MovementPattern.CORE, MovementPattern.HINGE, MovementPattern.CORE, MovementPattern.CORE, MovementPattern.HINGE, MovementPattern.CORE),
        WorkoutFocus.CARDIO to listOf(MovementPattern.CARDIO, MovementPattern.SQUAT, MovementPattern.CARDIO, MovementPattern.CORE, MovementPattern.CARDIO, MovementPattern.LUNGE),
        WorkoutFocus.MOBILITY to listOf(MovementPattern.MOBILITY, MovementPattern.STRETCH, MovementPattern.MOBILITY, MovementPattern.STRETCH, MovementPattern.STRETCH, MovementPattern.STRETCH),
    )

    /** Work and rest seconds of one circuit slot for each level. */
    fun interval(level: FitnessExperience): Pair<Int, Int> = when (level) {
        FitnessExperience.BEGINNER -> 30 to 30
        FitnessExperience.INTERMEDIATE -> 40 to 20
        FitnessExperience.ADVANCED -> 45 to 15
    }

    /** Equipment counted as available; an office is assumed to have a chair. */
    fun availableEquipment(request: WorkoutRequest): Set<Equipment> =
        if (request.location == WorkoutLocation.OFFICE) request.equipment + Equipment.CHAIR_OR_BENCH else request.equipment

    fun eligible(request: WorkoutRequest, catalog: List<Exercise> = ExerciseCatalog.all): List<Exercise> {
        val location = if (request.location == WorkoutLocation.MIXED) WorkoutLocation.HOME else request.location
        val equipment = availableEquipment(request)
        return catalog.filter { exercise ->
            exercise.inGenerator &&
                location in exercise.locations &&
                equipment.containsAll(exercise.requiredEquipment) &&
                exercise.level.ordinal <= request.level.ordinal
        }
    }

    fun generate(request: WorkoutRequest, catalog: List<Exercise> = ExerciseCatalog.all): WorkoutPlan {
        val random = Random(request.seed)
        val pool = eligible(request, catalog)
        val totalSeconds = request.minutes * 60

        val warmUpPool = pool.filter { it.kind == ExerciseKind.MOBILITY || (it.kind == ExerciseKind.CARDIO && it.level == FitnessExperience.BEGINNER) }
        val coolDownPool = pool.filter { it.kind == ExerciseKind.STRETCH }
        val warmUpCount = (totalSeconds * 0.12 / (WARM_UP_WORK + TRANSITION_REST)).roundToInt().coerceIn(1, 4)
        val coolDownCount = (totalSeconds * 0.10 / (COOL_DOWN_WORK + TRANSITION_REST)).roundToInt().coerceIn(1, 3)
        val warmUp = pickDistinct(warmUpPool, warmUpCount, random)
        val coolDown = pickDistinct(coolDownPool, coolDownCount, random)

        val mobility = request.focus == WorkoutFocus.MOBILITY
        val mainPool = if (mobility) {
            pool.filter { it.kind == ExerciseKind.MOBILITY || it.kind == ExerciseKind.STRETCH }
        } else {
            pool.filter { it.kind == ExerciseKind.STRENGTH || it.kind == ExerciseKind.CARDIO }
        }.ifEmpty { warmUpPool }
        val (work, rest) = if (mobility) MOBILITY_MAIN_WORK to MOBILITY_MAIN_REST else interval(request.level)
        val circuit = pickCircuit(mainPool, request, circuitSize(request.minutes), random)

        val warmUpSeconds = warmUp.size * (WARM_UP_WORK + TRANSITION_REST)
        val coolDownSeconds = coolDown.size * (COOL_DOWN_WORK + TRANSITION_REST)
        val mainSeconds = totalSeconds - warmUpSeconds - coolDownSeconds
        val slots = if (circuit.isEmpty()) 0 else maxOf(circuit.size, (mainSeconds.toDouble() / (work + rest)).roundToInt())

        val steps = buildList {
            warmUp.forEach { add(WorkoutStep(it, WorkoutBlock.WARM_UP, WARM_UP_WORK, TRANSITION_REST, round = 1)) }
            repeat(slots) { slot ->
                add(WorkoutStep(circuit[slot % circuit.size], WorkoutBlock.MAIN, work, rest, round = slot / circuit.size + 1))
            }
            coolDown.forEach { add(WorkoutStep(it, WorkoutBlock.COOL_DOWN, COOL_DOWN_WORK, TRANSITION_REST, round = 1)) }
        }
        val finished = if (steps.isEmpty()) steps else steps.dropLast(1) + steps.last().copy(restSeconds = 0)
        return WorkoutPlan(request, finished)
    }

    private fun circuitSize(minutes: Int): Int = when {
        minutes <= 5 -> 3
        minutes <= 10 -> 4
        minutes <= 15 -> 5
        else -> 6
    }

    /**
     * Follows the focus's pattern order, preferring exercises at the chosen
     * level (then the closest easier one), shuffled within each level.
     */
    private fun pickCircuit(pool: List<Exercise>, request: WorkoutRequest, size: Int, random: Random): List<Exercise> {
        val ranked = pool.shuffled(random).sortedBy { abs(request.level.ordinal - it.level.ordinal) }
        val picked = mutableListOf<Exercise>()
        PATTERNS.getValue(request.focus).forEach { pattern ->
            if (picked.size >= size) return@forEach
            ranked.firstOrNull { it.pattern == pattern && it !in picked }?.let(picked::add)
        }
        ranked.filter { it !in picked }.take(size - picked.size).let(picked::addAll)
        return picked
    }

    private fun pickDistinct(pool: List<Exercise>, count: Int, random: Random): List<Exercise> {
        if (pool.isEmpty()) return emptyList()
        val shuffled = pool.shuffled(random)
        return List(count) { shuffled[it % shuffled.size] }.distinct()
    }
}
