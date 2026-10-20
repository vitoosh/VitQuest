package io.github.vitoosh.vitquest.core.domain.workout

import io.github.vitoosh.vitquest.core.model.ExerciseDose
import io.github.vitoosh.vitquest.core.model.ExerciseKind
import io.github.vitoosh.vitquest.core.model.WorkoutLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseCatalogTest {

    private val all = ExerciseCatalog.all

    @Test
    fun keysAreUniqueAndStable() {
        assertEquals(all.size, all.map { it.key }.toSet().size)
        all.forEach { assertTrue(it.key, it.key.matches(Regex("[a-z_]+"))) }
        assertEquals(all.first(), ExerciseCatalog.find(all.first().key))
    }

    @Test
    fun everyExerciseHasCompleteBilingualGuidance() {
        all.forEach { exercise ->
            val texts = listOf(exercise.name, exercise.description) + exercise.steps + exercise.modifications + exercise.safety
            texts.forEach { text ->
                assertTrue("${exercise.key} has an empty Indonesian text", text.id.isNotBlank())
                assertTrue("${exercise.key} has an empty English text", text.en.isNotBlank())
            }
            assertTrue(exercise.key, exercise.steps.size >= 3)
            assertTrue(exercise.key, exercise.modifications.isNotEmpty())
            assertTrue(exercise.key, exercise.safety.isNotEmpty())
            assertTrue(exercise.key, exercise.muscles.isNotEmpty())
            assertTrue(exercise.key, exercise.locations.isNotEmpty() && WorkoutLocation.MIXED !in exercise.locations)
            assertTrue(exercise.key, exercise.met in 2.0..12.0)
            when (val dose = exercise.dose) {
                is ExerciseDose.Reps -> assertTrue(exercise.key, dose.min in 1..dose.max)
                is ExerciseDose.Hold -> assertTrue(exercise.key, dose.seconds > 0)
            }
        }
    }

    @Test
    fun everyMovementExceptWalkingHasAnAnimatedTutorial() {
        all.forEach { exercise ->
            if (exercise.key == "brisk_walk") {
                assertNull(exercise.animation)
            } else {
                assertNotNull("${exercise.key} needs an animation", exercise.animation)
            }
        }
    }

    @Test
    fun theRequestedBasicsAreIncluded() {
        val keys = all.map { it.key }.toSet()
        listOf(
            "bodyweight_squat", "wall_push_up", "knee_push_up", "push_up", "reverse_lunge", "plank",
            "glute_bridge", "calf_raise", "marching_in_place", "step_jack",
        ).forEach { assertTrue(it, it in keys) }
        assertTrue(all.any { it.kind == ExerciseKind.MOBILITY })
        assertTrue(all.any { it.kind == ExerciseKind.STRETCH })
        // Office sessions need standing options for every part of a session.
        listOf(ExerciseKind.STRENGTH, ExerciseKind.CARDIO, ExerciseKind.MOBILITY, ExerciseKind.STRETCH).forEach { kind ->
            assertTrue(kind.name, all.any { it.kind == kind && WorkoutLocation.OFFICE in it.locations && it.requiredEquipment.isEmpty() })
        }
    }
}
