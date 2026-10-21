package io.github.vitoosh.vitquest.core.domain.steps

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionStepDetectorTest {

    private val hz = 50
    private val stepNs = 1_000_000_000L / hz

    /** Gravity on the z axis plus a vertical bounce once per step, with sensor noise. */
    private fun walk(
        detector: MotionStepDetector,
        seconds: Double,
        stepsPerSecond: Double,
        amplitude: Double,
        noise: Double = 0.3,
        startNs: Long = 0L,
        seed: Int = 1,
    ): Int {
        val random = Random(seed)
        var counted = 0
        val samples = (seconds * hz).toInt()
        for (i in 0 until samples) {
            val t = i.toDouble() / hz
            val bounce = amplitude * sin(2 * PI * stepsPerSecond * t)
            fun jitter() = ((random.nextDouble() * 2 - 1) * noise).toFloat()
            counted += detector.onSample(startNs + i * stepNs, jitter(), jitter(), (9.81 + bounce).toFloat() + jitter())
        }
        return counted
    }

    private fun assertNear(expected: Int, actual: Int, tolerance: Double = 0.05) {
        assertTrue("expected about $expected, got $actual", abs(actual - expected) <= expected * tolerance)
    }

    @Test
    fun countsAMinuteOfWalking() {
        val detector = MotionStepDetector()
        val counted = walk(detector, 60.0, 1.8, amplitude = 2.5)
        assertNear(108, counted)
        assertEquals(counted.toLong(), detector.totalSteps)
    }

    @Test
    fun countsGentleWalkingWithThePhoneInHand() {
        assertNear(96, walk(MotionStepDetector(), 60.0, 1.6, amplitude = 1.4, noise = 0.2))
    }

    @Test
    fun countsRunning() {
        assertNear(168, walk(MotionStepDetector(), 60.0, 2.8, amplitude = 6.0, noise = 0.6))
    }

    @Test
    fun ignoresAPhoneLyingStill() {
        assertEquals(0, walk(MotionStepDetector(), 120.0, 1.8, amplitude = 0.0, noise = 0.15))
    }

    @Test
    fun aFewStepsAreNotEnoughToStartCounting() {
        // Four seconds at 1.5 steps/s is six steps: below the eight-step streak.
        assertEquals(0, walk(MotionStepDetector(), 4.0, 1.5, amplitude = 2.5))
    }

    @Test
    fun isolatedBumpsDoNotCount() {
        val detector = MotionStepDetector()
        var counted = 0
        var t = 0L
        repeat(20) {
            // A sharp jolt every 3 seconds, like the phone being set down.
            for (i in 0 until 3 * hz) {
                val z = if (i in 10..14) 14f else 9.81f
                counted += detector.onSample(t, 0f, 0f, z)
                t += stepNs
            }
        }
        assertEquals(0, counted)
    }

    @Test
    fun aPauseStartsANewStreak() {
        val detector = MotionStepDetector()
        val first = walk(detector, 20.0, 2.0, amplitude = 2.5)
        // Ten seconds standing still, then walking again.
        walk(detector, 10.0, 2.0, amplitude = 0.0, noise = 0.1, startNs = 20L * 1_000_000_000L, seed = 2)
        val second = walk(detector, 20.0, 2.0, amplitude = 2.5, startNs = 30L * 1_000_000_000L, seed = 3)
        assertNear(40, first)
        assertNear(40, second)
    }

    @Test
    fun batchedSamplesWithAGapRestartTheFilters() {
        val detector = MotionStepDetector()
        walk(detector, 10.0, 2.0, amplitude = 2.5)
        // The phone slept for a minute: the next sample is far later and must not count as a step.
        assertEquals(0, detector.onSample(80L * 1_000_000_000L, 0f, 0f, 20f))
    }
}
