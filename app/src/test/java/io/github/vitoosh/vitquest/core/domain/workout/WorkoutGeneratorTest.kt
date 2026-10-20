package io.github.vitoosh.vitquest.core.domain.workout

import io.github.vitoosh.vitquest.core.model.Equipment
import io.github.vitoosh.vitquest.core.model.ExerciseKind
import io.github.vitoosh.vitquest.core.model.FitnessExperience
import io.github.vitoosh.vitquest.core.model.WorkoutBlock
import io.github.vitoosh.vitquest.core.model.WorkoutFocus
import io.github.vitoosh.vitquest.core.model.WorkoutLocation
import io.github.vitoosh.vitquest.core.model.WorkoutRequest
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutGeneratorTest {

    private fun request(
        minutes: Int = 15,
        level: FitnessExperience = FitnessExperience.BEGINNER,
        location: WorkoutLocation = WorkoutLocation.HOME,
        equipment: Set<Equipment> = emptySet(),
        focus: WorkoutFocus = WorkoutFocus.FULL_BODY,
        seed: Long = 7L,
    ) = WorkoutRequest(minutes, level, location, equipment, focus, seed)

    @Test
    fun everyCombinationFitsTheChosenDurationAndRules() {
        WorkoutGenerator.DURATIONS.forEach { minutes ->
            FitnessExperience.entries.forEach { level ->
                listOf(WorkoutLocation.HOME, WorkoutLocation.OFFICE, WorkoutLocation.OUTDOOR).forEach { location ->
                    WorkoutFocus.entries.forEach { focus ->
                        val req = request(minutes, level, location, emptySet(), focus)
                        val plan = WorkoutGenerator.generate(req)
                        val label = "$minutes min $level $location $focus"
                        assertTrue(label, plan.steps.isNotEmpty())
                        // Within 20 % (or a minute for short sessions) of the requested time.
                        val target = minutes * 60
                        assertTrue("$label took ${plan.totalSeconds}s", abs(plan.totalSeconds - target) <= maxOf(60, target / 5))
                        plan.steps.forEach { step ->
                            assertTrue(label, location in step.exercise.locations)
                            assertTrue(label, step.exercise.level.ordinal <= level.ordinal)
                            assertTrue(label, step.exercise.inGenerator)
                            assertTrue(label, WorkoutGenerator.availableEquipment(req).containsAll(step.exercise.requiredEquipment))
                        }
                        assertEquals(label, 0, plan.steps.last().restSeconds)
                        assertEquals(label, WorkoutBlock.WARM_UP, plan.steps.first().block)
                        assertEquals(label, WorkoutBlock.COOL_DOWN, plan.steps.last().block)
                    }
                }
            }
        }
    }

    @Test
    fun samePlanForTheSameSeedAndAnotherForANewSeed() {
        val a = WorkoutGenerator.generate(request(seed = 1))
        val b = WorkoutGenerator.generate(request(seed = 1))
        assertEquals(a.steps.map { it.exercise.key }, b.steps.map { it.exercise.key })
        val variety = (1L..10L).map { seed -> WorkoutGenerator.generate(request(seed = seed)).steps.map { it.exercise.key } }.toSet()
        assertTrue(variety.size > 1)
    }

    @Test
    fun officeSessionsAvoidFloorWorkAndJumping() {
        val plan = WorkoutGenerator.generate(request(minutes = 20, level = FitnessExperience.ADVANCED, location = WorkoutLocation.OFFICE))
        val keys = plan.steps.map { it.exercise.key }
        listOf("push_up", "plank", "burpee", "jumping_jack", "glute_bridge").forEach { assertFalse(it, it in keys) }
    }

    @Test
    fun equipmentUnlocksMatchingExercises() {
        val without = WorkoutGenerator.eligible(request(level = FitnessExperience.INTERMEDIATE)).map { it.key }
        val with = WorkoutGenerator.eligible(request(level = FitnessExperience.INTERMEDIATE, equipment = setOf(Equipment.DUMBBELLS))).map { it.key }
        assertFalse("goblet_squat" in without)
        assertTrue("goblet_squat" in with)
    }

    @Test
    fun levelsUseTheirIntervalsAndMainBlockCyclesTheCircuit() {
        val plan = WorkoutGenerator.generate(request(minutes = 20, level = FitnessExperience.INTERMEDIATE))
        val main = plan.steps.filter { it.block == WorkoutBlock.MAIN }
        assertTrue(main.all { it.workSeconds == 40 })
        assertTrue(main.dropLast(1).all { it.restSeconds == 20 })
        assertTrue(main.maxOf { it.round } >= 2)
        assertTrue(main.all { it.exercise.kind == ExerciseKind.STRENGTH || it.exercise.kind == ExerciseKind.CARDIO })
    }

    @Test
    fun mobilityFocusUsesOnlyGentleMoves() {
        val plan = WorkoutGenerator.generate(request(minutes = 10, focus = WorkoutFocus.MOBILITY))
        assertTrue(plan.steps.all { it.exercise.kind == ExerciseKind.MOBILITY || it.exercise.kind == ExerciseKind.STRETCH || it.block == WorkoutBlock.WARM_UP })
    }
}
