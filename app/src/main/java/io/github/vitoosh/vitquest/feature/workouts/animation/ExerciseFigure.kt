package io.github.vitoosh.vitquest.feature.workouts.animation

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.vitoosh.vitquest.core.domain.workout.FigureSegment
import io.github.vitoosh.vitquest.core.domain.workout.FigureSolver
import io.github.vitoosh.vitquest.core.domain.workout.SolvedFigure
import io.github.vitoosh.vitquest.core.domain.workout.SolvedLimbs
import io.github.vitoosh.vitquest.core.model.ExerciseAnimation
import io.github.vitoosh.vitquest.core.model.FigurePoint
import io.github.vitoosh.vitquest.core.model.FigureProp
import io.github.vitoosh.vitquest.core.model.FigureView
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Every figure uses the same frame so thumbnails line up; animations with overhead arms use the extra top space. */
private const val FRAME_TOP = -20f
private const val FRAME_ASPECT = ExerciseAnimation.WIDTH / (ExerciseAnimation.HEIGHT - FRAME_TOP)

private const val LIMB_WIDTH = 4.4f
private const val TORSO_WIDTH = 5.6f

/** True when the user turned animations off in system settings. */
@Composable
fun systemAnimationsEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
}

/**
 * Draws an exercise illustration. With [frame] null the figure loops through
 * its keyframes; otherwise it shows that keyframe still (0 = start, 1 = end).
 */
@Composable
fun ExerciseFigure(
    animation: ExerciseAnimation,
    highlight: Set<FigureSegment>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    frame: Int? = null,
) {
    val phase = if (frame == null) {
        val transition = rememberInfiniteTransition(label = "exercise")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(animation.cycleMillis, easing = LinearEasing), RepeatMode.Restart),
            label = "phase",
        )
        value
    } else {
        frame.coerceIn(0, animation.keyframes.lastIndex).toFloat() / animation.keyframes.size
    }
    // In side view the far limbs are dimmed for depth; in front view both sides are equally visible.
    val front = animation.view == FigureView.FRONT
    val colors = FigureColors(
        body = MaterialTheme.colorScheme.onSurface,
        far = MaterialTheme.colorScheme.onSurface.copy(alpha = if (front) 1f else 0.42f),
        highlight = MaterialTheme.colorScheme.primary,
        farHighlight = MaterialTheme.colorScheme.primary.copy(alpha = if (front) 1f else 0.5f),
        prop = MaterialTheme.colorScheme.outline,
        floor = MaterialTheme.colorScheme.outlineVariant,
        mat = MaterialTheme.colorScheme.secondaryContainer,
    )
    Canvas(
        modifier = modifier
            .aspectRatio(FRAME_ASPECT)
            .semantics { this.contentDescription = contentDescription },
    ) {
        val pose = if (frame == null) FigureSolver.poseAt(animation, phase) else animation.keyframes[frame.coerceIn(0, animation.keyframes.lastIndex)]
        val figure = FigureSolver.solve(animation, pose)
        drawFigure(animation, figure, phase, highlight, colors)
    }
}

private data class FigureColors(
    val body: Color,
    val far: Color,
    val highlight: Color,
    val farHighlight: Color,
    val prop: Color,
    val floor: Color,
    val mat: Color,
)

private class FrameMapper(size: Size) {
    private val height = ExerciseAnimation.HEIGHT - FRAME_TOP
    val scale = minOf(size.width / ExerciseAnimation.WIDTH, size.height / height)
    private val left = (size.width - ExerciseAnimation.WIDTH * scale) / 2f
    private val top = size.height - height * scale

    fun map(point: FigurePoint) = Offset(left + point.x * scale, top + (point.y - FRAME_TOP) * scale)
    fun map(x: Float, y: Float) = map(FigurePoint(x, y))
}

private fun DrawScope.drawFigure(
    animation: ExerciseAnimation,
    figure: SolvedFigure,
    phase: Float,
    highlight: Set<FigureSegment>,
    colors: FigureColors,
) {
    val m = FrameMapper(size)
    val floorY = ExerciseAnimation.FLOOR_Y
    val hasBar = animation.props.any { it is FigureProp.Bar }

    // Background props.
    if (!hasBar) drawLine(colors.floor, m.map(2f, floorY), m.map(118f, floorY), strokeWidth = 1.2f * m.scale, cap = StrokeCap.Round)
    animation.props.forEach { prop ->
        when (prop) {
            FigureProp.Mat -> drawRoundRect(
                color = colors.mat,
                topLeft = m.map(4f, floorY - 1.6f),
                size = Size(112f * m.scale, 1.6f * m.scale),
                cornerRadius = CornerRadius(0.8f * m.scale),
            )
            is FigureProp.Wall -> drawLine(colors.prop, m.map(prop.x, FRAME_TOP), m.map(prop.x, floorY), strokeWidth = 2.4f * m.scale)
            is FigureProp.Chair -> drawChair(m, prop, colors.prop)
            is FigureProp.Bar -> drawLine(colors.prop, m.map(30f, prop.y), m.map(90f, prop.y), strokeWidth = 2.4f * m.scale, cap = StrokeCap.Round)
            else -> Unit
        }
    }
    val rope = animation.props.filterIsInstance<FigureProp.Rope>().firstOrNull()
    val ropeAngle = phase * 2f * PI.toFloat()
    val ropeBehind = sin(ropeAngle) < 0f
    if (rope != null && ropeBehind) drawRope(m, figure, rope, ropeAngle, colors.prop)

    fun color(segment: FigureSegment, far: Boolean) = when {
        segment in highlight && far -> colors.farHighlight
        segment in highlight -> colors.highlight
        far -> colors.far
        else -> colors.body
    }

    fun limbs(side: SolvedLimbs, far: Boolean) {
        segment(m, side.hip, side.knee, color(FigureSegment.THIGHS, far))
        segment(m, side.knee, side.ankle, color(FigureSegment.SHINS, far))
        segment(m, side.ankle, side.toe, if (far) colors.far else colors.body)
    }

    fun arm(side: SolvedLimbs, far: Boolean) {
        segment(m, side.shoulder, side.elbow, color(FigureSegment.UPPER_ARMS, far))
        segment(m, side.elbow, side.hand, color(FigureSegment.FOREARMS, far))
    }

    // Far side first, so the near side reads as being in front.
    arm(figure.far, far = true)
    limbs(figure.far, far = true)

    val torsoColor = color(FigureSegment.TORSO, far = false)
    val spine = Path().apply {
        val hip = m.map(figure.hip)
        val control = m.map(figure.spineControl)
        val shoulder = m.map(figure.shoulder)
        moveTo(hip.x, hip.y)
        quadraticTo(control.x, control.y, shoulder.x, shoulder.y)
    }
    drawPath(spine, torsoColor, style = Stroke(width = TORSO_WIDTH * m.scale, cap = StrokeCap.Round))
    if (figure.near.shoulder != figure.far.shoulder) {
        segment(m, figure.near.shoulder, figure.far.shoulder, torsoColor, TORSO_WIDTH)
        segment(m, figure.near.hip, figure.far.hip, if (FigureSegment.HIPS in highlight) colors.highlight else torsoColor, TORSO_WIDTH)
    }
    if (FigureSegment.HIPS in highlight) {
        drawCircle(colors.highlight, radius = 3.6f * m.scale, center = m.map(figure.hip))
    }
    drawCircle(colors.body, radius = FigureSolver.HEAD_RADIUS * m.scale, center = m.map(figure.head))

    limbs(figure.near, far = false)
    arm(figure.near, far = false)

    // Props held in the hands.
    animation.props.forEach { prop ->
        when (prop) {
            is FigureProp.Dumbbells -> {
                drawDumbbell(m, figure.near.hand, colors.prop)
                if (prop.bothHands) drawDumbbell(m, figure.far.hand, colors.prop)
            }
            is FigureProp.Band -> {
                listOf(figure.near.hand, figure.far.hand).forEach { hand ->
                    drawLine(colors.highlight.copy(alpha = 0.7f), m.map(hand), m.map(prop.anchor), strokeWidth = 1.2f * m.scale)
                }
                drawCircle(colors.prop, radius = 1.8f * m.scale, center = m.map(prop.anchor))
            }
            else -> Unit
        }
    }
    if (rope != null && !ropeBehind) drawRope(m, figure, rope, ropeAngle, colors.prop)
}

private fun DrawScope.segment(m: FrameMapper, from: FigurePoint, to: FigurePoint, color: Color, width: Float = LIMB_WIDTH) {
    drawLine(color, m.map(from), m.map(to), strokeWidth = width * m.scale, cap = StrokeCap.Round)
}

private fun DrawScope.drawChair(m: FrameMapper, chair: FigureProp.Chair, color: Color) {
    val stroke = 2f * m.scale
    val floor = ExerciseAnimation.FLOOR_Y
    drawLine(color, m.map(chair.seatLeft, chair.seatY), m.map(chair.seatRight, chair.seatY), strokeWidth = stroke * 1.4f, cap = StrokeCap.Round)
    drawLine(color, m.map(chair.seatLeft + 2f, chair.seatY), m.map(chair.seatLeft + 2f, floor), strokeWidth = stroke)
    drawLine(color, m.map(chair.seatRight - 2f, chair.seatY), m.map(chair.seatRight - 2f, floor), strokeWidth = stroke)
    val backX = if (chair.backAtLeft) chair.seatLeft + 1f else chair.seatRight - 1f
    drawLine(color, m.map(backX, chair.seatY), m.map(backX, chair.seatY - 26f), strokeWidth = stroke, cap = StrokeCap.Round)
}

private fun DrawScope.drawDumbbell(m: FrameMapper, hand: FigurePoint, color: Color) {
    drawLine(color, m.map(hand.x, hand.y - 3.5f), m.map(hand.x, hand.y + 3.5f), strokeWidth = 1.6f * m.scale)
    listOf(-3.5f, 3.5f).forEach { dy ->
        drawRoundRect(
            color = color,
            topLeft = m.map(hand.x - 2.6f, hand.y + dy - 1.2f),
            size = Size(5.2f * m.scale, 2.4f * m.scale),
            cornerRadius = CornerRadius(0.8f * m.scale),
        )
    }
}

/** The rope turns once per loop: overhead at the start, under the feet halfway. */
private fun DrawScope.drawRope(m: FrameMapper, figure: SolvedFigure, rope: FigureProp.Rope, angle: Float, color: Color) {
    val left = figure.near.hand
    val right = figure.far.hand
    val middleY = rope.topY + (rope.bottomY - rope.topY) * (1f - cos(angle)) / 2f
    val handY = (left.y + right.y) / 2f
    val control = FigurePoint((left.x + right.x) / 2f, 2f * middleY - handY)
    val path = Path().apply {
        val a = m.map(left)
        val c = m.map(control)
        val b = m.map(right)
        moveTo(a.x, a.y)
        quadraticTo(c.x, c.y, b.x, b.y)
    }
    drawPath(path, color, style = Stroke(width = 1.4f * m.scale, cap = StrokeCap.Round))
}
