package io.github.vitoosh.vitquest.core.domain.steps

import kotlin.math.max
import kotlin.math.sqrt

/**
 * Counts steps from raw accelerometer samples, for phones whose built-in step
 * counter misses most of them.
 *
 * The acceleration magnitude has gravity removed with a slow moving average and
 * is then smoothed. Each peak above a threshold that adapts to how strongly the
 * user walks is a step candidate, if it comes at a walking or running rhythm.
 * Steps only start counting after [Config.streakToStart] candidates in a row, and
 * are then credited all at once, so picking the phone up or tapping it does not
 * count, while a real walk loses nothing.
 *
 * Not thread-safe; feed it samples in timestamp order from one thread.
 */
class MotionStepDetector(private val config: Config = Config()) {

    data class Config(
        /** Fastest plausible step: about 4 steps per second, a sprint. */
        val minStepIntervalNs: Long = 250_000_000L,
        /** Slower than this and the walk is over; the next steps start a new streak. */
        val maxStepIntervalNs: Long = 2_000_000_000L,
        val streakToStart: Int = 8,
        /** Peaks must rise this far above gravity, in m/s². */
        val minPeak: Float = 1.0f,
        /** A peak must also reach this share of the user's recent average peak. */
        val relativePeak: Float = 0.4f,
        val gravityTimeConstantS: Float = 1.0f,
        val smoothingTimeConstantS: Float = 0.06f,
    )

    private var lastTimestampNs: Long? = null
    private var gravity = 0f
    private var smoothed = 0f
    private var previous = 0f
    private var rising = false
    private var averagePeak = 0f
    private var lastStepNs: Long? = null
    private var streak = 0

    /** Steps counted so far, including ones credited when a streak started. */
    var totalSteps: Long = 0
        private set

    fun reset() {
        lastTimestampNs = null
        gravity = 0f
        smoothed = 0f
        previous = 0f
        rising = false
        averagePeak = 0f
        lastStepNs = null
        streak = 0
    }

    /** Returns how many steps this sample adds (usually 0 or 1; [Config.streakToStart] when a walk is confirmed). */
    fun onSample(timestampNs: Long, x: Float, y: Float, z: Float): Int {
        val magnitude = sqrt(x * x + y * y + z * z)
        val lastNs = lastTimestampNs
        lastTimestampNs = timestampNs
        if (lastNs == null || timestampNs <= lastNs || timestampNs - lastNs > config.maxStepIntervalNs) {
            // First sample, or a gap in the data: start the filters again from here.
            gravity = magnitude
            smoothed = 0f
            previous = 0f
            rising = false
            return 0
        }
        val dt = (timestampNs - lastNs) / 1e9f
        gravity += (magnitude - gravity) * (dt / (config.gravityTimeConstantS + dt))
        smoothed += ((magnitude - gravity) - smoothed) * (dt / (config.smoothingTimeConstantS + dt))

        var added = 0
        if (smoothed < previous && rising) {
            // `previous` was a local maximum.
            added = onPeak(previous, timestampNs)
        }
        rising = smoothed > previous
        previous = smoothed
        return added
    }

    private fun onPeak(height: Float, timestampNs: Long): Int {
        val threshold = max(config.minPeak, config.relativePeak * averagePeak)
        if (height < threshold) return 0
        val last = lastStepNs
        if (last != null && timestampNs - last < config.minStepIntervalNs) return 0

        if (last == null || timestampNs - last > config.maxStepIntervalNs) streak = 0
        lastStepNs = timestampNs
        averagePeak = if (averagePeak == 0f) height else averagePeak * 0.8f + height * 0.2f
        streak++
        val added = when {
            streak < config.streakToStart -> 0
            streak == config.streakToStart -> config.streakToStart
            else -> 1
        }
        totalSteps += added
        return added
    }
}
