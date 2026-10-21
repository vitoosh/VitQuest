package io.github.vitoosh.vitquest.core.domain.workout

import io.github.vitoosh.vitquest.core.model.FitnessExperience
import io.github.vitoosh.vitquest.core.model.StepStatus
import io.github.vitoosh.vitquest.core.model.WorkoutBlock
import io.github.vitoosh.vitquest.core.model.WorkoutFocus
import io.github.vitoosh.vitquest.core.model.WorkoutLocation
import io.github.vitoosh.vitquest.core.model.WorkoutPlan
import io.github.vitoosh.vitquest.core.model.WorkoutRequest
import io.github.vitoosh.vitquest.core.model.WorkoutStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutPlayerEngineTest {

    private val squat = requireNotNull(ExerciseCatalog.find("bodyweight_squat"))
    private val lunge = requireNotNull(ExerciseCatalog.find("reverse_lunge"))
    private val plan = WorkoutPlan(
        WorkoutRequest(5, FitnessExperience.BEGINNER, WorkoutLocation.HOME, emptySet(), WorkoutFocus.FULL_BODY, 0),
        listOf(
            WorkoutStep(squat, WorkoutBlock.MAIN, workSeconds = 30, restSeconds = 10, round = 1),
            WorkoutStep(lunge, WorkoutBlock.MAIN, workSeconds = 40, restSeconds = 0, round = 1),
        ),
    )

    @Test
    fun runsThroughReadyWorkRestAndFinishes() {
        var state = WorkoutPlayerEngine.start(plan)
        assertEquals(PlayerPhase.READY, state.phase)
        state = WorkoutPlayerEngine.tick(state, plan, WorkoutPlayerEngine.READY_MS)
        assertEquals(PlayerPhase.WORK, state.phase)
        assertEquals(30_000L, state.remainingMs)
        // A long tick carries over: 30 s work + 10 s rest + 5 s into the next exercise.
        state = WorkoutPlayerEngine.tick(state, plan, 45_000L)
        assertEquals(PlayerPhase.WORK, state.phase)
        assertEquals(1, state.index)
        assertEquals(35_000L, state.remainingMs)
        assertEquals(StepStatus.COMPLETED, state.statuses[0])
        state = WorkoutPlayerEngine.tick(state, plan, 60_000L)
        assertEquals(PlayerPhase.FINISHED, state.phase)
        assertEquals(listOf(30_000L, 40_000L), state.workedMs)
        assertFalse(state.endedEarly)
    }

    @Test
    fun pauseStopsTheClock() {
        var state = WorkoutPlayerEngine.tick(WorkoutPlayerEngine.start(plan), plan, WorkoutPlayerEngine.READY_MS)
        state = WorkoutPlayerEngine.togglePause(state)
        val paused = WorkoutPlayerEngine.tick(state, plan, 20_000L)
        assertEquals(state, paused)
        val resumed = WorkoutPlayerEngine.tick(WorkoutPlayerEngine.togglePause(paused), plan, 1_000L)
        assertEquals(29_000L, resumed.remainingMs)
    }

    @Test
    fun skipAndCompleteRecordWhatHappened() {
        var state = WorkoutPlayerEngine.tick(WorkoutPlayerEngine.start(plan), plan, WorkoutPlayerEngine.READY_MS + 5_000L)
        state = WorkoutPlayerEngine.skip(state, plan)
        assertEquals(StepStatus.SKIPPED, state.statuses[0])
        assertEquals(PlayerPhase.REST, state.phase)
        state = WorkoutPlayerEngine.extendRest(state)
        assertEquals(25_000L, state.remainingMs)
        state = WorkoutPlayerEngine.skip(state, plan)
        assertEquals(PlayerPhase.WORK, state.phase)
        state = WorkoutPlayerEngine.complete(WorkoutPlayerEngine.tick(state, plan, 12_000L), plan)
        assertEquals(PlayerPhase.FINISHED, state.phase)
        assertEquals(listOf(StepStatus.SKIPPED, StepStatus.COMPLETED), state.statuses)
        assertEquals(listOf(5_000L, 12_000L), state.workedMs)
    }

    @Test
    fun previousRestartsOrGoesBack() {
        var state = WorkoutPlayerEngine.tick(WorkoutPlayerEngine.start(plan), plan, WorkoutPlayerEngine.READY_MS + 30_000L + 10_000L + 1_000L)
        assertEquals(1, state.index)
        // Just started: go back to the previous exercise and redo it.
        state = WorkoutPlayerEngine.previous(state, plan)
        assertEquals(0, state.index)
        assertEquals(30_000L, state.remainingMs)
        assertEquals(StepStatus.NOT_REACHED, state.statuses[0])
        // Well into it: restart the same exercise.
        state = WorkoutPlayerEngine.previous(WorkoutPlayerEngine.tick(state, plan, 10_000L), plan)
        assertEquals(0, state.index)
        assertEquals(0L, state.workedMs[0])
    }

    @Test
    fun finishingEarlyKeepsDoneWorkAndMarksTheRest() {
        var state = WorkoutPlayerEngine.tick(WorkoutPlayerEngine.start(plan), plan, WorkoutPlayerEngine.READY_MS + 12_000L)
        state = WorkoutPlayerEngine.finishEarly(state)
        assertEquals(PlayerPhase.FINISHED, state.phase)
        assertTrue(state.endedEarly)
        assertEquals(listOf(StepStatus.SKIPPED, StepStatus.NOT_REACHED), state.statuses)
        val summary = WorkoutEnergy.summarize(plan, state, weightKg = 70.0)
        assertEquals(12, summary.activeSeconds)
        assertEquals(0, summary.completedSteps)
        // 3.8 MET × 70 kg × 12 s / 3600
        assertEquals(0.8867, summary.estimatedKcal!!, 0.001)
        assertEquals(null, WorkoutEnergy.summarize(plan, state, weightKg = null).estimatedKcal)
    }

    @Test
    fun perSideExercisesAskToSwitchHalfway() {
        var state = WorkoutPlayerEngine.tick(WorkoutPlayerEngine.start(plan), plan, WorkoutPlayerEngine.READY_MS + 40_000L)
        assertEquals(1, state.index)
        assertFalse(WorkoutPlayerEngine.shouldSwitchSides(state, plan))
        state = WorkoutPlayerEngine.tick(state, plan, 20_000L)
        assertTrue(WorkoutPlayerEngine.shouldSwitchSides(state, plan))
    }
}
