package io.github.vitoosh.vitquest.core.domain.workout

import io.github.vitoosh.vitquest.core.domain.workout.FigureSolver.FOREARM
import io.github.vitoosh.vitquest.core.domain.workout.FigureSolver.SHIN
import io.github.vitoosh.vitquest.core.domain.workout.FigureSolver.THIGH
import io.github.vitoosh.vitquest.core.domain.workout.FigureSolver.TORSO
import io.github.vitoosh.vitquest.core.domain.workout.FigureSolver.UPPER_ARM
import io.github.vitoosh.vitquest.core.model.ExerciseAnimation
import io.github.vitoosh.vitquest.core.model.FigurePoint
import io.github.vitoosh.vitquest.core.model.FigurePose
import io.github.vitoosh.vitquest.core.model.FigureProp
import io.github.vitoosh.vitquest.core.model.FigureRoot
import io.github.vitoosh.vitquest.core.model.FigureView
import io.github.vitoosh.vitquest.core.model.LimbBends
import kotlin.math.acos

/**
 * Keyframes for every illustrated exercise. Side views face right. Positions
 * are built from a few helpers so planted hands and feet sit exactly where
 * the limbs can reach (checked by ExercisePosesTest).
 */
internal object ExercisePoses {

    private const val ANKLE_Y = 88.5f
    private const val FLOOR_JOINT_Y = 88f
    private const val LEG = THIGH + SHIN - 0.15f
    private const val ARM = UPPER_ARM + FOREARM - 0.15f
    private const val STAND_HIP_Y = ANKLE_Y - LEG
    private const val BODY = LEG + TORSO

    private fun p(x: Float, y: Float) = FigurePoint(x, y)

    private fun FigurePoint.along(degrees: Float, length: Float): FigurePoint {
        val d = FigureSolver.unit(degrees)
        return FigurePoint(x + d.x * length, y + d.y * length)
    }

    private fun FigurePoint.shift(dx: Float, dy: Float) = FigurePoint(x + dx, y + dy)

    /** Angle of a straight body line of [length] from a planted point so its far end sits at height [y]. */
    private fun angleForHeight(from: FigurePoint, length: Float, y: Float): Float =
        Math.toDegrees(acos(((from.y - y) / length).toDouble())).toFloat()

    private fun FigurePose.lifted(dy: Float) = copy(
        root = root.shift(0f, -dy),
        nearHand = nearHand.shift(0f, -dy),
        farHand = farHand.shift(0f, -dy),
        nearFoot = nearFoot.shift(0f, -dy),
        farFoot = farFoot.shift(0f, -dy),
    )

    // ---- Standing ----

    private fun standing(x: Float = 60f): FigurePose {
        val hip = p(x, STAND_HIP_Y)
        val shoulder = hip.along(0f, TORSO)
        return FigurePose(
            root = hip,
            torso = 0f,
            nearHand = shoulder.shift(1.5f, 28.6f),
            farHand = shoulder.shift(-1f, 28.6f),
            nearFoot = p(x, ANKLE_Y),
            farFoot = p(x + 3f, ANKLE_Y),
        )
    }

    private fun standingFront(x: Float = 60f): FigurePose {
        val hip = p(x, STAND_HIP_Y)
        val shoulder = hip.along(0f, TORSO)
        return FigurePose(
            root = hip,
            torso = 0f,
            nearHand = shoulder.shift(-9.5f, 28.8f),
            farHand = shoulder.shift(9.5f, 28.8f),
            nearFoot = p(x - 5f, ANKLE_Y),
            farFoot = p(x + 5f, ANKLE_Y),
            nearToe = 160f,
            farToe = 20f,
        )
    }

    private fun squatPose(x: Float = 60f): FigurePose {
        val hip = p(x - 13f, 66f)
        val shoulder = hip.along(38f, TORSO)
        return FigurePose(
            root = hip,
            torso = 38f,
            head = -25f,
            nearHand = shoulder.along(90f, ARM - 0.5f),
            farHand = shoulder.along(92f, ARM - 0.5f),
            nearFoot = p(x, ANKLE_Y),
            farFoot = p(x + 3f, ANKLE_Y),
        )
    }

    val squat = ExerciseAnimation(FigureView.SIDE, listOf(standing(), squatPose()), cycleMillis = 3200)

    val chairSquat = run {
        val sit = p(41f, 60f)
        val shoulder = sit.along(22f, TORSO)
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(
                standing(60f),
                FigurePose(
                    root = sit,
                    torso = 22f,
                    head = -10f,
                    nearHand = shoulder.along(90f, ARM - 1f),
                    farHand = shoulder.along(92f, ARM - 1f),
                    nearFoot = p(60f, ANKLE_Y),
                    farFoot = p(63f, ANKLE_Y),
                ),
            ),
            cycleMillis = 3600,
            props = listOf(FigureProp.Chair(seatLeft = 18f, seatRight = 42f, seatY = 64f, backAtLeft = true)),
        )
    }

    val gobletSquat = run {
        fun chestHands(pose: FigurePose): FigurePose {
            val shoulder = pose.root.along(pose.torso, TORSO)
            val hold = shoulder.along(pose.torso + 140f, 10f)
            return pose.copy(nearHand = hold, farHand = hold.shift(1f, 0f))
        }
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(chestHands(standing()), chestHands(squatPose())),
            cycleMillis = 3400,
            props = listOf(FigureProp.Dumbbells(bothHands = false)),
        )
    }

    val squatJump = run {
        val squatArmsBack = squatPose().let { pose ->
            val shoulder = pose.root.along(pose.torso, TORSO)
            pose.copy(nearHand = shoulder.along(200f, ARM - 1f), farHand = shoulder.along(195f, ARM - 1f))
        }
        val jump = standing().lifted(9f).copy(nearToe = 60f, farToe = 60f)
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(standing(), squatArmsBack, jump),
            cycleMillis = 2400,
            holdFraction = 0.1f,
        )
    }

    // ---- Push ----

    val wallPushUp = run {
        val ankle = p(50f, ANKLE_Y)
        val hand = p(101.8f, 25.5f)
        fun lean(angle: Float, headTilt: Float) = FigurePose(
            root = ankle.along(angle, LEG),
            torso = angle,
            head = headTilt,
            nearHand = hand,
            farHand = hand.shift(0f, 1.5f),
            nearFoot = ankle,
            farFoot = ankle.shift(2.5f, 0f),
            nearToe = 0f,
            farToe = 0f,
        )
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(lean(20f, 0f), lean(30f, 5f)),
            cycleMillis = 3000,
            props = listOf(FigureProp.Wall(104f)),
        )
    }

    val inclinePushUp = run {
        val ankle = p(30f, 87.5f)
        val hand = p(102.5f, 68f)
        fun lean(angle: Float) = FigurePose(
            root = ankle.along(angle, LEG),
            torso = angle,
            nearHand = hand,
            farHand = hand.shift(1f, 0f),
            nearFoot = ankle,
            farFoot = ankle.shift(2f, 0f),
            nearToe = 30f,
            farToe = 30f,
        )
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(lean(53f), lean(63f)),
            cycleMillis = 3000,
            props = listOf(FigureProp.Chair(seatLeft = 94f, seatRight = 117f, seatY = 70f, backAtLeft = false)),
        )
    }

    /** Push-up family pivoting on [pivot] (knees or toes) with a straight body of [length]. */
    private fun pushUp(pivot: FigurePoint, pivotToHip: Float, length: Float, feet: (FigurePose) -> FigurePose): ExerciseAnimation {
        val top = angleForHeight(pivot, length, FLOOR_JOINT_Y - ARM + 0.6f)
        val bottom = angleForHeight(pivot, length, FLOOR_JOINT_Y - 13f)
        val hand = pivot.along(top, length).let { p(it.x + 2f, FLOOR_JOINT_Y) }
        fun at(angle: Float) = feet(
            FigurePose(
                root = pivot.along(angle, pivotToHip),
                torso = angle,
                nearHand = hand,
                farHand = hand.shift(1.5f, 0f),
                nearFoot = pivot,
                farFoot = pivot,
            ),
        )
        return ExerciseAnimation(FigureView.SIDE, listOf(at(top), at(bottom)), cycleMillis = 2800, props = listOf(FigureProp.Mat))
    }

    val kneePushUp = run {
        val knee = p(40f, FLOOR_JOINT_Y)
        val ankle = p(19f, 87.4f)
        pushUp(knee, THIGH, THIGH + TORSO) { it.copy(nearFoot = ankle, farFoot = ankle.shift(1.5f, 0f), nearToe = 180f, farToe = 180f) }
    }

    val pushUp = run {
        val ankle = p(14f, 84f)
        pushUp(ankle, LEG, BODY) { it.copy(nearFoot = ankle, farFoot = ankle.shift(1.5f, 0f), nearToe = 65f, farToe = 65f) }
    }

    val tricepDip = run {
        val hand = p(38f, 62f)
        fun at(hip: FigurePoint) = FigurePose(
            root = hip,
            torso = 0f,
            nearHand = hand,
            farHand = hand.shift(-1.5f, 0f),
            nearFoot = p(67f, ANKLE_Y),
            farFoot = p(70f, ANKLE_Y),
        )
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(at(p(43f, 63f)), at(p(45f, 77f))),
            cycleMillis = 3000,
            props = listOf(FigureProp.Chair(seatLeft = 14f, seatRight = 40f, seatY = 64f, backAtLeft = true)),
        )
    }

    val shoulderPress = run {
        val base = standingFront()
        val shoulder = base.root.along(0f, TORSO)
        ExerciseAnimation(
            FigureView.FRONT,
            listOf(
                base.copy(nearHand = shoulder.shift(-18f, -4f), farHand = shoulder.shift(18f, -4f)),
                base.copy(nearHand = shoulder.shift(-11f, -27f), farHand = shoulder.shift(11f, -27f)),
            ),
            cycleMillis = 3000,
            bends = LimbBends(nearElbow = -1, farElbow = 1, nearKnee = 1, farKnee = -1),
            props = listOf(FigureProp.Dumbbells(bothHands = true)),
            viewTop = -14f,
        )
    }

    // ---- Pull ----

    val bandRow = run {
        val base = standing(50f)
        val shoulder = base.root.along(0f, TORSO)
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(
                base.copy(nearHand = shoulder.shift(27f, 10f), farHand = shoulder.shift(27f, 11f)),
                base.copy(nearHand = shoulder.shift(8f, 13f), farHand = shoulder.shift(8f, 14f)),
            ),
            cycleMillis = 3000,
            props = listOf(FigureProp.Wall(116f), FigureProp.Band(p(115f, 30f))),
        )
    }

    val dumbbellRow = run {
        val hip = p(52f, 50f)
        val shoulder = hip.along(70f, TORSO)
        fun at(hand: FigurePoint) = FigurePose(
            root = hip,
            torso = 70f,
            head = -10f,
            nearHand = hand,
            farHand = shoulder.shift(1f, 27f),
            nearFoot = p(60f, ANKLE_Y),
            farFoot = p(63f, ANKLE_Y),
        )
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(at(shoulder.shift(0.5f, 27.5f)), at(shoulder.shift(-8.5f, 11.5f))),
            cycleMillis = 3000,
            props = listOf(FigureProp.Dumbbells(bothHands = false)),
        )
    }

    val deadHang = run {
        val barY = -11f
        fun at(hipY: Float): FigurePose {
            val hip = p(60f, hipY)
            return FigurePose(
                root = hip,
                torso = 0f,
                nearHand = p(45f, barY + 1f),
                farHand = p(75f, barY + 1f),
                nearFoot = p(56f, hipY + LEG),
                farFoot = p(64f, hipY + LEG),
                nearToe = 160f,
                farToe = 20f,
            )
        }
        ExerciseAnimation(
            FigureView.FRONT,
            listOf(at(46.3f), at(43f)),
            cycleMillis = 3200,
            bends = LimbBends(nearElbow = -1, farElbow = 1, nearKnee = 1, farKnee = -1),
            props = listOf(FigureProp.Bar(barY)),
            viewTop = -14f,
        )
    }

    // ---- Lower body ----

    val reverseLunge = run {
        val start = standing(58f)
        val hip = p(50f, 63f)
        val shoulder = hip.along(4f, TORSO)
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(
                start.copy(farFoot = p(61f, ANKLE_Y)),
                FigurePose(
                    root = hip,
                    torso = 4f,
                    nearHand = shoulder.shift(1f, 27f),
                    farHand = shoulder.shift(-1f, 27f),
                    nearFoot = p(22f, 85f),
                    farFoot = p(61f, ANKLE_Y),
                    nearToe = 50f,
                ),
            ),
            cycleMillis = 3400,
        )
    }

    val gluteBridge = run {
        val shoulder = p(28f, 84f)
        fun at(torso: Float, head: Float) = FigurePose(
            root = shoulder,
            torso = torso,
            head = head,
            nearHand = p(56.5f, 87f),
            farHand = p(56.8f, 86.2f),
            nearFoot = p(80f, ANKLE_Y),
            farFoot = p(82f, ANKLE_Y),
        )
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(at(-90f, 0f), at(-125f, 35f)),
            cycleMillis = 3200,
            root = FigureRoot.SHOULDER,
            props = listOf(FigureProp.Mat),
        )
    }

    val calfRaise = run {
        val start = standing()
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(start, start.lifted(5f).copy(nearFoot = p(60f, ANKLE_Y - 5f), farFoot = p(63f, ANKLE_Y - 5f), nearToe = 55f, farToe = 55f)),
            cycleMillis = 2400,
        )
    }

    // ---- Core ----

    /** Forearm plank shoulders sit over the elbows; the pose rotates around the shoulder. */
    private val plankShoulder = p(82.6f, 72.8f)
    private val plankHand = p(plankShoulder.x + FOREARM - 0.2f, FLOOR_JOINT_Y)

    private fun forearmPose(torso: Float, foot: FigurePoint, toe: Float) = FigurePose(
        root = plankShoulder,
        torso = torso,
        nearHand = plankHand,
        farHand = plankHand.shift(1.5f, 0f),
        nearFoot = foot,
        farFoot = foot.shift(1.5f, 0f),
        nearToe = toe,
        farToe = toe,
    )

    val kneePlank = ExerciseAnimation(
        FigureView.SIDE,
        listOf(forearmPose(62f, p(15.5f, 85f), 180f), forearmPose(72f, p(15.5f, 85f), 180f)),
        cycleMillis = 3600,
        root = FigureRoot.SHOULDER,
        holdFraction = 0.35f,
        props = listOf(FigureProp.Mat),
    )

    val plank = run {
        val angle = angleForHeight(p(0f, 84f), BODY, plankShoulder.y)
        val ankle = plankShoulder.along(angle + 180f, BODY)
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(forearmPose(72f, p(15.5f, 85f), 180f), forearmPose(angle, ankle, 65f)),
            cycleMillis = 3600,
            root = FigureRoot.SHOULDER,
            holdFraction = 0.35f,
            props = listOf(FigureProp.Mat),
        )
    }

    /** Hands and knees: knee under the hip, hand under the shoulder. */
    private fun tabletop(): FigurePose {
        val hip = p(50f, FLOOR_JOINT_Y - THIGH)
        val shoulderY = FLOOR_JOINT_Y - ARM + 0.5f
        val torso = angleForHeight(hip, TORSO, shoulderY)
        val shoulder = hip.along(torso, TORSO)
        return FigurePose(
            root = hip,
            torso = torso,
            nearHand = p(shoulder.x + 1f, FLOOR_JOINT_Y),
            farHand = p(shoulder.x + 2.5f, FLOOR_JOINT_Y),
            nearFoot = p(hip.x - SHIN, 88f),
            farFoot = p(hip.x - SHIN + 1.5f, 88f),
            nearToe = 180f,
            farToe = 180f,
        )
    }

    val birdDog = run {
        val start = tabletop()
        val shoulder = start.root.along(start.torso, TORSO)
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(
                start,
                start.copy(
                    farHand = shoulder.along(80f, ARM - 0.5f),
                    nearFoot = start.root.along(-90f, LEG),
                    nearToe = 90f,
                    head = -10f,
                ),
            ),
            cycleMillis = 3600,
            holdFraction = 0.3f,
            props = listOf(FigureProp.Mat),
        )
    }

    val catCow = run {
        val base = tabletop()
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(base.copy(spine = 5f, head = -30f), base.copy(spine = -7f, head = 40f)),
            cycleMillis = 4000,
            props = listOf(FigureProp.Mat),
        )
    }

    val childsPose = run {
        val base = tabletop()
        val hip = p(33f, 78f)
        val shoulder = hip.along(95f, TORSO)
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(
                base,
                base.copy(
                    root = hip,
                    torso = 95f,
                    nearHand = p(shoulder.x + ARM - 1f, 87.5f),
                    farHand = p(shoulder.x + ARM - 0.5f, 87.5f),
                ),
            ),
            cycleMillis = 4400,
            holdFraction = 0.35f,
            props = listOf(FigureProp.Mat),
        )
    }

    val deadBug = run {
        val shoulder = p(30f, 84f)
        val hip = shoulder.along(90f, TORSO)
        val start = FigurePose(
            root = shoulder,
            torso = -90f,
            nearHand = p(31f, 56f),
            farHand = p(33f, 56.5f),
            nearFoot = p(hip.x + SHIN, hip.y - THIGH),
            farFoot = p(hip.x + SHIN + 1.5f, hip.y - THIGH),
        )
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(start, start.copy(nearHand = p(3.5f, 82f), farFoot = hip.along(98f, LEG - 0.5f))),
            cycleMillis = 3600,
            root = FigureRoot.SHOULDER,
            props = listOf(FigureProp.Mat),
        )
    }

    val superman = run {
        val hip = p(50f, 85f)
        val start = FigurePose(
            root = hip,
            torso = 90f,
            nearHand = p(105.5f, 86f),
            farHand = p(106.5f, 86.5f),
            nearFoot = p(8.4f, 86f),
            farFoot = p(9.6f, 86.5f),
            nearToe = 180f,
            farToe = 180f,
        )
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(start, start.copy(torso = 80f, head = -5f, nearHand = p(104f, 73f), farHand = p(105f, 74f), nearFoot = p(9.5f, 78f), farFoot = p(10.5f, 79f))),
            cycleMillis = 3200,
            holdFraction = 0.3f,
            props = listOf(FigureProp.Mat),
        )
    }

    val standingKneeCrunch = run {
        val start = standing()
        val shoulder = start.root.along(0f, TORSO)
        val behindHead = start.copy(nearHand = shoulder.shift(1f, -11f), farHand = shoulder.shift(-2f, -11f))
        val crunchShoulder = start.root.along(18f, TORSO)
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(
                behindHead,
                behindHead.copy(
                    torso = 18f,
                    nearHand = crunchShoulder.shift(6f, 6f),
                    farHand = crunchShoulder.shift(1f, -9f),
                    nearFoot = p(70f, 60f),
                    nearToe = 30f,
                ),
            ),
            cycleMillis = 2400,
        )
    }

    val mountainClimber = run {
        val frames = pushUp.keyframes
        val top = frames.first()
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(top, top.copy(nearFoot = p(42f, 80f), nearToe = 60f), top, top.copy(farFoot = p(43.5f, 80f), farToe = 60f)),
            cycleMillis = 1800,
            holdFraction = 0.05f,
            props = listOf(FigureProp.Mat),
        )
    }

    // ---- Cardio ----

    val marching = run {
        val start = standing()
        val shoulder = start.root.along(0f, TORSO)
        val forward = shoulder.shift(12f, 21f)
        val back = shoulder.shift(-8f, 25f)
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(
                start,
                start.copy(nearFoot = p(66f, 68f), nearToe = 40f, nearHand = back, farHand = forward),
                start,
                start.copy(farFoot = p(69f, 68f), farToe = 40f, nearHand = forward, farHand = back),
            ),
            cycleMillis = 2400,
            holdFraction = 0.05f,
        )
    }

    val highKnees = run {
        val start = standing().copy(nearFoot = p(60f, 86.5f), farFoot = p(63f, 86.5f), nearToe = 30f, farToe = 30f)
        val shoulder = start.root.along(0f, TORSO)
        val forward = shoulder.shift(14f, 11f)
        val back = shoulder.shift(-9f, 19f)
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(
                start.copy(nearFoot = p(79f, 66f), nearToe = 50f, nearHand = back, farHand = forward),
                start.copy(farFoot = p(82f, 66f), farToe = 50f, nearHand = forward, farHand = back),
            ),
            cycleMillis = 1000,
            holdFraction = 0f,
        )
    }

    private fun armsUp(pose: FigurePose): FigurePose {
        val shoulder = pose.root.along(0f, TORSO)
        val near = shoulder.shift(-FigureSolver.SHOULDER_HALF_WIDTH, 0f)
        val far = shoulder.shift(FigureSolver.SHOULDER_HALF_WIDTH, 0f)
        return pose.copy(nearHand = near.along(-30f, ARM - 0.6f), farHand = far.along(30f, ARM - 0.6f))
    }

    val stepJack = run {
        val start = standingFront()
        ExerciseAnimation(
            FigureView.FRONT,
            listOf(
                start,
                armsUp(start.copy(root = p(60f, 48.5f))).copy(nearFoot = p(44f, ANKLE_Y)),
                start,
                armsUp(start.copy(root = p(60f, 48.5f))).copy(farFoot = p(76f, ANKLE_Y)),
            ),
            cycleMillis = 3200,
            holdFraction = 0.1f,
            viewTop = -14f,
        )
    }

    val jumpingJack = run {
        val start = standingFront()
        val wide = armsUp(start.copy(root = p(60f, 49f))).copy(nearFoot = p(42f, ANKLE_Y), farFoot = p(78f, ANKLE_Y))
        ExerciseAnimation(FigureView.FRONT, listOf(start, wide), cycleMillis = 1400, holdFraction = 0.05f, viewTop = -14f)
    }

    val jumpRope = run {
        val start = standingFront().let { pose ->
            val shoulder = pose.root.along(0f, TORSO)
            pose.copy(nearHand = shoulder.shift(-15f, 27f), farHand = shoulder.shift(15f, 27f))
        }
        ExerciseAnimation(
            FigureView.FRONT,
            listOf(start, start.lifted(5f).copy(nearToe = 110f, farToe = 70f)),
            cycleMillis = 1000,
            holdFraction = 0f,
            props = listOf(FigureProp.Rope(topY = -8f, bottomY = 97f)),
            viewTop = -14f,
        )
    }

    val burpee = run {
        val stand = standing()
        val crouchHip = p(50f, 74f)
        val crouchShoulder = crouchHip.along(60f, TORSO)
        val hand = p(crouchShoulder.x + 2.5f, FLOOR_JOINT_Y)
        val crouch = FigurePose(
            root = crouchHip,
            torso = 60f,
            nearHand = hand,
            farHand = hand.shift(1.5f, 0f),
            nearFoot = p(60f, ANKLE_Y),
            farFoot = p(63f, ANKLE_Y),
        )
        val plankAngle = angleForHeight(p(0f, 84f), BODY, FLOOR_JOINT_Y - ARM + 0.6f)
        val ankle = p(hand.x - 2f, FLOOR_JOINT_Y - ARM + 0.6f).along(plankAngle + 180f, BODY)
        val plankPose = FigurePose(
            root = ankle.along(plankAngle, LEG),
            torso = plankAngle,
            nearHand = hand,
            farHand = hand.shift(1.5f, 0f),
            nearFoot = ankle,
            farFoot = ankle.shift(1.5f, 0f),
            nearToe = 65f,
            farToe = 65f,
        )
        val jump = stand.lifted(8f).let { pose ->
            val shoulder = pose.root.along(0f, TORSO)
            pose.copy(nearHand = shoulder.along(5f, ARM - 0.6f), farHand = shoulder.along(-5f, ARM - 0.6f), nearToe = 60f, farToe = 60f)
        }
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(stand, crouch, plankPose, crouch, jump),
            cycleMillis = 4500,
            holdFraction = 0.12f,
            viewTop = -20f,
        )
    }

    // ---- Mobility and stretching ----

    val armCircles = run {
        val base = standingFront()
        val shoulder = base.root.along(0f, TORSO)
        val near = shoulder.shift(-FigureSolver.SHOULDER_HALF_WIDTH, 0f)
        val far = shoulder.shift(FigureSolver.SHOULDER_HALF_WIDTH, 0f)
        fun at(lift: Float) = base.copy(nearHand = near.along(-90f + lift, ARM - 0.3f), farHand = far.along(90f - lift, ARM - 0.3f))
        ExerciseAnimation(FigureView.FRONT, listOf(at(9f), at(-9f)), cycleMillis = 1000, holdFraction = 0f)
    }

    val legSwing = run {
        val start = standing(58f)
        val shoulder = start.root.along(0f, TORSO)
        val wallHand = shoulder.shift(26f, 8f)
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(
                start.copy(farHand = wallHand, nearFoot = start.root.along(150f, LEG), nearToe = -20f),
                start.copy(farHand = wallHand, nearFoot = start.root.along(208f, LEG), nearToe = 40f),
            ),
            cycleMillis = 2000,
            holdFraction = 0.05f,
            props = listOf(FigureProp.Wall(87f)),
        )
    }

    val hamstringStretch = run {
        val start = standing(58f).copy(nearFoot = p(60f, ANKLE_Y), farFoot = p(63f, ANKLE_Y))
        val hip = p(51f, 48f)
        val shoulder = hip.along(75f, TORSO)
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(
                start,
                start.copy(root = hip, torso = 75f, nearHand = shoulder.shift(-11f, 25f), farHand = shoulder.shift(-12f, 25.5f)),
            ),
            cycleMillis = 4400,
            holdFraction = 0.4f,
        )
    }

    val quadStretch = run {
        val start = standing()
        val shoulder = start.root.along(0f, TORSO)
        val ankle = p(54f, 47f)
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(start, start.copy(nearFoot = ankle, nearHand = ankle.shift(0.5f, -0.5f), farHand = shoulder.shift(26f, 5f), nearToe = 200f)),
            cycleMillis = 4400,
            holdFraction = 0.4f,
        )
    }

    val chestOpener = run {
        val start = standing()
        val shoulder = start.root.along(-6f, TORSO)
        ExerciseAnimation(
            FigureView.SIDE,
            listOf(start, start.copy(torso = -6f, head = -8f, nearHand = shoulder.shift(-10f, 26f), farHand = shoulder.shift(-11f, 26.5f))),
            cycleMillis = 4000,
            holdFraction = 0.4f,
        )
    }

    val sideBend = run {
        val start = standingFront()
        val bend = -15f
        val shoulder = start.root.along(bend, TORSO)
        val across = FigurePoint(kotlin.math.cos(Math.toRadians(bend.toDouble())).toFloat(), kotlin.math.sin(Math.toRadians(bend.toDouble())).toFloat())
        val farShoulder = FigurePoint(shoulder.x + across.x * FigureSolver.SHOULDER_HALF_WIDTH, shoulder.y + across.y * FigureSolver.SHOULDER_HALF_WIDTH)
        ExerciseAnimation(
            FigureView.FRONT,
            listOf(
                start,
                start.copy(torso = bend, head = -5f, farHand = farShoulder.along(-25f, ARM - 0.6f), nearHand = p(44f, 50f)),
            ),
            cycleMillis = 4000,
            holdFraction = 0.4f,
            viewTop = -14f,
        )
    }
}
