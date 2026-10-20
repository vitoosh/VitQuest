package io.github.vitoosh.vitquest.feature.workouts

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.model.Bilingual
import io.github.vitoosh.vitquest.core.model.ExerciseDose
import io.github.vitoosh.vitquest.core.model.ExerciseKind
import io.github.vitoosh.vitquest.core.model.Muscle
import io.github.vitoosh.vitquest.core.model.WorkoutBlock
import io.github.vitoosh.vitquest.core.model.WorkoutFocus
import io.github.vitoosh.vitquest.core.model.WorkoutIntensity
import io.github.vitoosh.vitquest.core.ui.formatInteger
import io.github.vitoosh.vitquest.core.ui.localized
import java.util.Locale

@Composable
@ReadOnlyComposable
fun Bilingual.text(): String = localized(id, en)

@get:StringRes
val WorkoutFocus.labelRes: Int
    get() = when (this) {
        WorkoutFocus.FULL_BODY -> R.string.focus_full_body
        WorkoutFocus.UPPER -> R.string.focus_upper
        WorkoutFocus.LOWER -> R.string.focus_lower
        WorkoutFocus.CORE -> R.string.focus_core
        WorkoutFocus.CARDIO -> R.string.focus_cardio
        WorkoutFocus.MOBILITY -> R.string.focus_mobility
    }

@get:StringRes
val ExerciseKind.labelRes: Int
    get() = when (this) {
        ExerciseKind.STRENGTH -> R.string.kind_strength
        ExerciseKind.CARDIO -> R.string.kind_cardio
        ExerciseKind.MOBILITY -> R.string.kind_mobility
        ExerciseKind.STRETCH -> R.string.kind_stretch
    }

@get:StringRes
val WorkoutBlock.labelRes: Int
    get() = when (this) {
        WorkoutBlock.WARM_UP -> R.string.block_warm_up
        WorkoutBlock.MAIN -> R.string.block_main
        WorkoutBlock.COOL_DOWN -> R.string.block_cool_down
    }

@get:StringRes
val WorkoutIntensity.labelRes: Int
    get() = when (this) {
        WorkoutIntensity.LIGHT -> R.string.intensity_light
        WorkoutIntensity.MODERATE -> R.string.intensity_moderate
        WorkoutIntensity.VIGOROUS -> R.string.intensity_vigorous
    }

@get:StringRes
val Muscle.labelRes: Int
    get() = when (this) {
        Muscle.CHEST -> R.string.muscle_chest
        Muscle.SHOULDERS -> R.string.muscle_shoulders
        Muscle.TRICEPS -> R.string.muscle_triceps
        Muscle.BICEPS -> R.string.muscle_biceps
        Muscle.UPPER_BACK -> R.string.muscle_upper_back
        Muscle.LOWER_BACK -> R.string.muscle_lower_back
        Muscle.CORE -> R.string.muscle_core
        Muscle.GLUTES -> R.string.muscle_glutes
        Muscle.QUADS -> R.string.muscle_quads
        Muscle.HAMSTRINGS -> R.string.muscle_hamstrings
        Muscle.CALVES -> R.string.muscle_calves
        Muscle.HIP_FLEXORS -> R.string.muscle_hip_flexors
        Muscle.HEART_LUNGS -> R.string.muscle_heart_lungs
    }

/** "8–12 reps", "30 s per side" and so on. */
@Composable
@ReadOnlyComposable
fun doseText(dose: ExerciseDose): String = when (dose) {
    is ExerciseDose.Reps -> stringResource(
        if (dose.perSide) R.string.dose_reps_per_side else R.string.dose_reps,
        formatInteger(dose.min),
        formatInteger(dose.max),
    )
    is ExerciseDose.Hold -> if (dose.seconds >= 120) {
        stringResource(R.string.dose_minutes, formatInteger(dose.seconds / 60))
    } else {
        stringResource(if (dose.perSide) R.string.dose_hold_per_side else R.string.dose_hold, formatInteger(dose.seconds))
    }
}

/** Timer text such as 0:45 or 12:05. */
fun clockText(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return String.format(Locale.ROOT, "%d:%02d", safe / 60, safe % 60)
}

@Composable
@ReadOnlyComposable
fun minutesText(seconds: Int): String = stringResource(R.string.value_minutes, formatInteger((seconds + 30) / 60))
