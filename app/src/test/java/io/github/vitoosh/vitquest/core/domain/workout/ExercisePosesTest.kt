package io.github.vitoosh.vitquest.core.domain.workout

import io.github.vitoosh.vitquest.core.model.ExerciseAnimation
import io.github.vitoosh.vitquest.core.model.FigurePoint
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExercisePosesTest {

    private val animations = ExerciseCatalog.all.mapNotNull { exercise -> exercise.animation?.let { exercise.key to it } }

    private fun distance(a: FigurePoint, b: FigurePoint) = hypot(a.x - b.x, a.y - b.y)

    @Test
    fun plantedHandsAndFeetAreWithinReachInEveryKeyframe() {
        animations.forEach { (key, animation) ->
            animation.keyframes.forEachIndexed { index, pose ->
                val figure = FigureSolver.solve(animation, pose)
                listOf(
                    "near hand" to (figure.near.hand to pose.nearHand),
                    "far hand" to (figure.far.hand to pose.farHand),
                    "near foot" to (figure.near.ankle to pose.nearFoot),
                    "far foot" to (figure.far.ankle to pose.farFoot),
                ).forEach { (limb, points) ->
                    val miss = distance(points.first, points.second)
                    assertTrue("$key frame $index: $limb misses its target by $miss", miss < 0.6f)
                }
            }
        }
    }

    @Test
    fun limbsKeepTheirLengthWhileMoving() {
        animations.forEach { (key, animation) ->
            (0 until 40).forEach { step ->
                val figure = FigureSolver.solve(animation, FigureSolver.poseAt(animation, step / 40f))
                listOf(figure.near, figure.far).forEach { side ->
                    assertEquals(key, FigureSolver.UPPER_ARM, distance(side.shoulder, side.elbow), 0.6f)
                    assertEquals(key, FigureSolver.FOREARM, distance(side.elbow, side.hand), 0.6f)
                    assertEquals(key, FigureSolver.THIGH, distance(side.hip, side.knee), 0.6f)
                    assertEquals(key, FigureSolver.SHIN, distance(side.knee, side.ankle), 0.6f)
                }
                assertEquals(key, FigureSolver.TORSO, distance(figure.hip, figure.shoulder), 0.01f)
            }
        }
    }

    @Test
    fun figuresStayInsideTheFrameAndAboveTheFloor() {
        animations.forEach { (key, animation) ->
            (0 until 40).forEach { step ->
                val figure = FigureSolver.solve(animation, FigureSolver.poseAt(animation, step / 40f))
                val points = listOf(figure.head, figure.hip, figure.shoulder) +
                    listOf(figure.near, figure.far).flatMap { listOf(it.elbow, it.hand, it.knee, it.ankle, it.toe) }
                points.forEach { point ->
                    assertTrue("$key leaves the frame at $point", point.x in 0f..ExerciseAnimation.WIDTH)
                    assertTrue("$key goes above the frame at $point", point.y >= animation.viewTop - 1f && point.y >= -20f)
                    assertTrue("$key goes through the floor at $point", point.y <= ExerciseAnimation.FLOOR_Y + 0.5f)
                }
                assertTrue("$key head is cut off", figure.head.y - FigureSolver.HEAD_RADIUS >= -20f)
            }
        }
    }

    @Test
    fun startAndEndPosesDiffer() {
        animations.forEach { (key, animation) ->
            assertTrue(key, animation.keyframes[0] != animation.keyframes[1])
        }
    }
}
