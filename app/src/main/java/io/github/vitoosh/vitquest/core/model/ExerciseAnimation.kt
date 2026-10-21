package io.github.vitoosh.vitquest.core.model

/**
 * A looping stick-figure illustration of an exercise, drawn in a 120 × 100
 * unit box (y grows downwards, the floor is at [FLOOR_Y]). Poses only name
 * the hip (or shoulder), the torso angle and where hands and feet are; elbows
 * and knees are solved so limbs keep their length while the figure moves.
 */
data class ExerciseAnimation(
    val view: FigureView,
    val keyframes: List<FigurePose>,
    val cycleMillis: Int,
    val root: FigureRoot = FigureRoot.HIP,
    val bends: LimbBends = if (view == FigureView.SIDE) LimbBends.SIDE else LimbBends.FRONT,
    val props: List<FigureProp> = emptyList(),
    /** Share of each transition spent holding the pose before moving on. */
    val holdFraction: Float = 0.2f,
    /** Linear motion for continuous movements such as circles; eased otherwise. */
    val linear: Boolean = false,
    /** Extra space above the box for overhead arms (negative). */
    val viewTop: Float = 0f,
) {
    init {
        require(keyframes.size >= 2) { "An animation needs at least a start and an end pose" }
    }

    companion object {
        const val WIDTH = 120f
        const val HEIGHT = 100f
        const val FLOOR_Y = 91f
    }
}

/** Side view faces right (+x); front view faces the viewer. */
enum class FigureView { SIDE, FRONT }

/** Which joint a pose positions directly; the other end of the torso follows from the angle. */
enum class FigureRoot { HIP, SHOULDER }

data class FigurePoint(val x: Float, val y: Float)

/**
 * Bend direction of each limb's middle joint: +1 puts the elbow or knee on the
 * left of the line from shoulder (or hip) to hand (or foot), −1 on the right.
 */
data class LimbBends(val nearElbow: Int, val farElbow: Int, val nearKnee: Int, val farKnee: Int) {
    companion object {
        /** Elbows point back, knees point forward. */
        val SIDE = LimbBends(nearElbow = 1, farElbow = 1, nearKnee = -1, farKnee = -1)

        /** Elbows and knees point outwards (near = left side of the screen). */
        val FRONT = LimbBends(nearElbow = 1, farElbow = -1, nearKnee = 1, farKnee = -1)
    }
}

data class FigurePose(
    /** Hip centre, or shoulder centre when the animation's root is [FigureRoot.SHOULDER]. */
    val root: FigurePoint,
    /** Direction from hip to shoulder in degrees: 0 is upright, 90 points forward (+x). */
    val torso: Float,
    val nearHand: FigurePoint,
    val farHand: FigurePoint,
    val nearFoot: FigurePoint,
    val farFoot: FigurePoint,
    /** Head tilt relative to the torso; negative looks up or forward. */
    val head: Float = 0f,
    /** Spine curve: positive bows the belly side out, negative rounds the back. */
    val spine: Float = 0f,
    /** Foot directions in degrees (0 points +x, 90 points down). */
    val nearToe: Float = 0f,
    val farToe: Float = 0f,
)

sealed interface FigureProp {
    data class Wall(val x: Float) : FigureProp
    data class Chair(val seatLeft: Float, val seatRight: Float, val seatY: Float, val backAtLeft: Boolean) : FigureProp
    data object Mat : FigureProp
    data class Bar(val y: Float) : FigureProp
    data class Band(val anchor: FigurePoint) : FigureProp
    data class Dumbbells(val bothHands: Boolean) : FigureProp

    /** A rope turning once per cycle; it passes under the feet halfway through. */
    data class Rope(val topY: Float, val bottomY: Float) : FigureProp
}
