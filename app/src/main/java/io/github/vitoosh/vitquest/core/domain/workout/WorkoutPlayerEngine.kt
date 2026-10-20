package io.github.vitoosh.vitquest.core.domain.workout

import io.github.vitoosh.vitquest.core.model.ExerciseDose
import io.github.vitoosh.vitquest.core.model.StepStatus
import io.github.vitoosh.vitquest.core.model.WorkoutIntensity
import io.github.vitoosh.vitquest.core.model.WorkoutPlan
import kotlinx.serialization.Serializable

enum class PlayerPhase { READY, WORK, REST, FINISHED }

/**
 * Everything the workout player needs to resume. In READY and REST, [index]
 * is the step coming next; in WORK it is the step being done.
 */
@Serializable
data class PlayerState(
    val phase: PlayerPhase,
    val index: Int,
    val remainingMs: Long,
    val paused: Boolean,
    val workedMs: List<Long>,
    val statuses: List<StepStatus>,
    val endedEarly: Boolean = false,
)

/** Pure state machine for the workout player, driven by elapsed time. */
object WorkoutPlayerEngine {
    const val READY_MS = 10_000L
    const val EXTRA_REST_MS = 15_000L
    private const val RESTART_WINDOW_MS = 3_000L

    fun start(plan: WorkoutPlan): PlayerState {
        val count = plan.steps.size
        return PlayerState(
            phase = if (count == 0) PlayerPhase.FINISHED else PlayerPhase.READY,
            index = 0,
            remainingMs = READY_MS,
            paused = false,
            workedMs = List(count) { 0L },
            statuses = List(count) { StepStatus.NOT_REACHED },
        )
    }

    /** Moves time forward; leftover time flows into the next step, so a late tick loses nothing. */
    fun tick(state: PlayerState, plan: WorkoutPlan, deltaMs: Long): PlayerState {
        if (state.paused || state.phase == PlayerPhase.FINISHED || deltaMs <= 0) return state
        var current = state
        var left = deltaMs
        while (left > 0 && current.phase != PlayerPhase.FINISHED) {
            val used = minOf(left, current.remainingMs)
            current = current.copy(
                remainingMs = current.remainingMs - used,
                workedMs = if (current.phase == PlayerPhase.WORK) current.workedMs.plusAt(current.index, used) else current.workedMs,
            )
            left -= used
            if (current.remainingMs <= 0) current = advance(current, plan, StepStatus.COMPLETED)
        }
        return current
    }

    fun togglePause(state: PlayerState): PlayerState =
        if (state.phase == PlayerPhase.FINISHED) state else state.copy(paused = !state.paused)

    /** Skips the current exercise (marked skipped) or the remaining rest. */
    fun skip(state: PlayerState, plan: WorkoutPlan): PlayerState = when (state.phase) {
        PlayerPhase.WORK -> advance(state, plan, StepStatus.SKIPPED)
        PlayerPhase.READY, PlayerPhase.REST -> advance(state, plan, StepStatus.COMPLETED)
        PlayerPhase.FINISHED -> state
    }

    /** "Done" for rep-based sets finished before the timer runs out. */
    fun complete(state: PlayerState, plan: WorkoutPlan): PlayerState =
        if (state.phase == PlayerPhase.WORK) advance(state, plan, StepStatus.COMPLETED) else state

    /** Restarts the current exercise, or goes back one if it has only just started. */
    fun previous(state: PlayerState, plan: WorkoutPlan): PlayerState = when (state.phase) {
        PlayerPhase.WORK -> if (state.workedMs[state.index] < RESTART_WINDOW_MS && state.index > 0) {
            restartAt(state, plan, state.index - 1)
        } else {
            restartAt(state, plan, state.index)
        }
        PlayerPhase.REST -> restartAt(state, plan, state.index - 1)
        PlayerPhase.READY, PlayerPhase.FINISHED -> state
    }

    fun extendRest(state: PlayerState): PlayerState =
        if (state.phase == PlayerPhase.REST || state.phase == PlayerPhase.READY) state.copy(remainingMs = state.remainingMs + EXTRA_REST_MS) else state

    /** Ends the session now; a partly done exercise counts as skipped, the rest as not reached. */
    fun finishEarly(state: PlayerState): PlayerState {
        if (state.phase == PlayerPhase.FINISHED) return state
        val statuses = if (state.phase == PlayerPhase.WORK && state.workedMs[state.index] > 0) {
            state.statuses.setAt(state.index, StepStatus.SKIPPED)
        } else {
            state.statuses
        }
        return state.copy(phase = PlayerPhase.FINISHED, remainingMs = 0, paused = false, statuses = statuses, endedEarly = true)
    }

    /** Halfway through a per-side exercise the player tells the user to switch sides. */
    fun shouldSwitchSides(state: PlayerState, plan: WorkoutPlan): Boolean {
        if (state.phase != PlayerPhase.WORK) return false
        val step = plan.steps[state.index]
        val perSide = when (val dose = step.exercise.dose) {
            is ExerciseDose.Reps -> dose.perSide
            is ExerciseDose.Hold -> dose.perSide
        }
        return perSide && state.remainingMs <= step.workSeconds * 500L
    }

    private fun restartAt(state: PlayerState, plan: WorkoutPlan, index: Int) = state.copy(
        phase = PlayerPhase.WORK,
        index = index,
        remainingMs = plan.steps[index].workSeconds * 1000L,
        workedMs = state.workedMs.setAt(index, 0L),
        statuses = state.statuses.setAt(index, StepStatus.NOT_REACHED),
    )

    private fun advance(state: PlayerState, plan: WorkoutPlan, status: StepStatus): PlayerState = when (state.phase) {
        PlayerPhase.WORK -> {
            val statuses = state.statuses.setAt(state.index, status)
            val step = plan.steps[state.index]
            when {
                state.index == plan.steps.lastIndex -> state.copy(phase = PlayerPhase.FINISHED, remainingMs = 0, statuses = statuses)
                step.restSeconds > 0 -> state.copy(phase = PlayerPhase.REST, index = state.index + 1, remainingMs = step.restSeconds * 1000L, statuses = statuses)
                else -> state.copy(
                    phase = PlayerPhase.WORK,
                    index = state.index + 1,
                    remainingMs = plan.steps[state.index + 1].workSeconds * 1000L,
                    statuses = statuses,
                )
            }
        }
        PlayerPhase.READY, PlayerPhase.REST -> state.copy(phase = PlayerPhase.WORK, remainingMs = plan.steps[state.index].workSeconds * 1000L)
        PlayerPhase.FINISHED -> state
    }

    private fun List<Long>.plusAt(index: Int, value: Long) = mapIndexed { i, v -> if (i == index) v + value else v }

    private fun <T> List<T>.setAt(index: Int, value: T) = mapIndexed { i, v -> if (i == index) value else v }
}

data class WorkoutSummary(
    val activeSeconds: Int,
    val completedSteps: Int,
    val totalSteps: Int,
    /** Null when the body weight needed for the estimate is unknown. */
    val estimatedKcal: Double?,
)

object WorkoutEnergy {
    /** Gross energy from MET × body weight × time. A rough estimate, never a measurement. */
    fun kcal(met: Double, weightKg: Double, seconds: Int): Double = met * weightKg * seconds / 3600.0

    fun intensityMet(intensity: WorkoutIntensity): Double = when (intensity) {
        WorkoutIntensity.LIGHT -> 2.8
        WorkoutIntensity.MODERATE -> 3.8
        WorkoutIntensity.VIGOROUS -> 8.0
    }

    fun summarize(plan: WorkoutPlan, state: PlayerState, weightKg: Double?): WorkoutSummary {
        val seconds = state.workedMs.map { (it / 1000L).toInt() }
        return WorkoutSummary(
            activeSeconds = seconds.sum(),
            completedSteps = state.statuses.count { it == StepStatus.COMPLETED },
            totalSteps = plan.steps.size,
            estimatedKcal = weightKg?.let { kg ->
                plan.steps.indices.sumOf { kcal(plan.steps[it].exercise.met, kg, seconds[it]) }
            },
        )
    }
}
