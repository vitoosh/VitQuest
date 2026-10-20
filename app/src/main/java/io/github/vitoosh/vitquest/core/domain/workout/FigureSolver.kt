package io.github.vitoosh.vitquest.core.domain.workout

import io.github.vitoosh.vitquest.core.model.ExerciseAnimation
import io.github.vitoosh.vitquest.core.model.FigurePoint
import io.github.vitoosh.vitquest.core.model.FigurePose
import io.github.vitoosh.vitquest.core.model.FigureRoot
import io.github.vitoosh.vitquest.core.model.FigureView
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin

/** One side of the body after solving elbows and knees. */
data class SolvedLimbs(
    val shoulder: FigurePoint,
    val elbow: FigurePoint,
    val hand: FigurePoint,
    val hip: FigurePoint,
    val knee: FigurePoint,
    val ankle: FigurePoint,
    val toe: FigurePoint,
)

data class SolvedFigure(
    val hip: FigurePoint,
    val shoulder: FigurePoint,
    /** Control point of the curved spine between hip and shoulder. */
    val spineControl: FigurePoint,
    val head: FigurePoint,
    val near: SolvedLimbs,
    val far: SolvedLimbs,
)

/**
 * Turns poses into joint positions. Limb lengths are fixed, so a hand or foot
 * that stays at the same target in two keyframes stays planted while the
 * rest of the body moves (two-bone inverse kinematics).
 */
object FigureSolver {
    const val TORSO = 28f
    const val NECK = 4f
    const val HEAD_RADIUS = 6.5f
    const val UPPER_ARM = 15f
    const val FOREARM = 14.5f
    const val THIGH = 21f
    const val SHIN = 21f
    const val FOOT = 6f
    const val SHOULDER_HALF_WIDTH = 8f
    const val HIP_HALF_WIDTH = 5f
    private const val STRAIGHT_TOLERANCE = 1f

    /** The pose at [phase] (0 until 1) of the animation's loop. */
    fun poseAt(animation: ExerciseAnimation, phase: Float): FigurePose {
        val frames = animation.keyframes
        val scaled = (phase - floor(phase)) * frames.size
        val index = scaled.toInt().coerceIn(0, frames.lastIndex)
        val local = scaled - index
        val hold = animation.holdFraction.coerceIn(0f, 0.9f)
        val move = ((local - hold) / (1f - hold)).coerceIn(0f, 1f)
        val t = if (animation.linear) move else smoothStep(move)
        return lerp(frames[index], frames[(index + 1) % frames.size], t)
    }

    fun solve(animation: ExerciseAnimation, pose: FigurePose): SolvedFigure {
        val direction = unit(pose.torso)
        val (hip, shoulder) = when (animation.root) {
            FigureRoot.HIP -> pose.root to pose.root.plus(direction, TORSO)
            FigureRoot.SHOULDER -> pose.root.plus(direction, -TORSO) to pose.root
        }
        // Perpendicular to the torso, pointing to the belly side in side view.
        val across = FigurePoint(cos(rad(pose.torso)), sin(rad(pose.torso)))
        val middle = FigurePoint((hip.x + shoulder.x) / 2f, (hip.y + shoulder.y) / 2f)
        val spineControl = middle.plus(across, pose.spine)
        val head = shoulder.plus(unit(pose.torso + pose.head), NECK + HEAD_RADIUS)

        val front = animation.view == FigureView.FRONT
        val nearShoulder = if (front) shoulder.plus(across, -SHOULDER_HALF_WIDTH) else shoulder
        val farShoulder = if (front) shoulder.plus(across, SHOULDER_HALF_WIDTH) else shoulder
        val nearHip = if (front) FigurePoint(hip.x - HIP_HALF_WIDTH, hip.y) else hip
        val farHip = if (front) FigurePoint(hip.x + HIP_HALF_WIDTH, hip.y) else hip
        val bends = animation.bends
        return SolvedFigure(
            hip = hip,
            shoulder = shoulder,
            spineControl = spineControl,
            head = head,
            near = limbs(nearShoulder, pose.nearHand, bends.nearElbow, nearHip, pose.nearFoot, bends.nearKnee, pose.nearToe),
            far = limbs(farShoulder, pose.farHand, bends.farElbow, farHip, pose.farFoot, bends.farKnee, pose.farToe),
        )
    }

    private fun limbs(
        shoulder: FigurePoint,
        hand: FigurePoint,
        elbowBend: Int,
        hip: FigurePoint,
        foot: FigurePoint,
        kneeBend: Int,
        toeAngle: Float,
    ): SolvedLimbs {
        val (elbow, reachedHand) = twoBone(shoulder, hand, UPPER_ARM, FOREARM, elbowBend)
        // A knee never goes through the floor: while the feet travel (as in a burpee) it bends the other way.
        val (knee, ankle) = twoBone(hip, foot, THIGH, SHIN, kneeBend).let { solved ->
            if (solved.first.y > ExerciseAnimation.FLOOR_Y - 0.5f) twoBone(hip, foot, THIGH, SHIN, -kneeBend) else solved
        }
        val toe = FigurePoint(ankle.x + FOOT * cos(rad(toeAngle)), ankle.y + FOOT * sin(rad(toeAngle)))
        return SolvedLimbs(shoulder, elbow, reachedHand, hip, knee, ankle, toe)
    }

    /**
     * Places the middle joint so both bones keep their length. A target out of
     * reach is approached as far as the straightened limb allows.
     */
    fun twoBone(
        origin: FigurePoint,
        target: FigurePoint,
        first: Float,
        second: Float,
        bend: Int,
    ): Pair<FigurePoint, FigurePoint> {
        val dx = target.x - origin.x
        val dy = target.y - origin.y
        val length = hypot(dx, dy)
        if (length < 1e-4f) return origin.plus(FigurePoint(0f, 1f), first) to origin
        val ux = dx / length
        val uy = dy / length
        // A nearly straight limb is drawn straight; solving it exactly would show a small kink.
        val reach = first + second
        if (length >= reach - STRAIGHT_TOLERANCE) {
            val scale = minOf(length, reach) / reach
            return FigurePoint(origin.x + ux * first * scale, origin.y + uy * first * scale) to
                FigurePoint(origin.x + ux * reach * scale, origin.y + uy * reach * scale)
        }
        val distance = length.coerceIn(kotlin.math.abs(first - second) + 0.01f, first + second - 0.01f)
        val cosAngle = ((first * first + distance * distance - second * second) / (2f * first * distance)).coerceIn(-1f, 1f)
        val sinAngle = sin(acos(cosAngle))
        val px = -uy * bend
        val py = ux * bend
        val middle = FigurePoint(
            origin.x + first * (ux * cosAngle + px * sinAngle),
            origin.y + first * (uy * cosAngle + py * sinAngle),
        )
        return middle to FigurePoint(origin.x + ux * distance, origin.y + uy * distance)
    }

    private fun lerp(a: FigurePose, b: FigurePose, t: Float) = FigurePose(
        root = lerp(a.root, b.root, t),
        torso = lerp(a.torso, b.torso, t),
        nearHand = lerp(a.nearHand, b.nearHand, t),
        farHand = lerp(a.farHand, b.farHand, t),
        nearFoot = lerp(a.nearFoot, b.nearFoot, t),
        farFoot = lerp(a.farFoot, b.farFoot, t),
        head = lerp(a.head, b.head, t),
        spine = lerp(a.spine, b.spine, t),
        nearToe = lerp(a.nearToe, b.nearToe, t),
        farToe = lerp(a.farToe, b.farToe, t),
    )

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    private fun lerp(a: FigurePoint, b: FigurePoint, t: Float) = FigurePoint(lerp(a.x, b.x, t), lerp(a.y, b.y, t))

    private fun smoothStep(t: Float) = t * t * (3f - 2f * t)

    private fun rad(degrees: Float) = degrees * PI.toFloat() / 180f

    /** Direction of an angle measured from straight up, clockwise (towards +x). */
    fun unit(degrees: Float) = FigurePoint(sin(rad(degrees)), -cos(rad(degrees)))

    private fun FigurePoint.plus(direction: FigurePoint, length: Float) =
        FigurePoint(x + direction.x * length, y + direction.y * length)
}
