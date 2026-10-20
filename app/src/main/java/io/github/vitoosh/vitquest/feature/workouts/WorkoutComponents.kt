package io.github.vitoosh.vitquest.feature.workouts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.IconBubble
import io.github.vitoosh.vitquest.core.designsystem.icon.VqIcons
import io.github.vitoosh.vitquest.core.domain.workout.MuscleHighlight
import io.github.vitoosh.vitquest.core.model.Exercise
import io.github.vitoosh.vitquest.core.ui.labelRes
import io.github.vitoosh.vitquest.feature.workouts.animation.ExerciseFigure

/** A still start pose used as a list thumbnail; walking uses an icon instead. */
@Composable
fun ExerciseThumbnail(exercise: Exercise, modifier: Modifier = Modifier) {
    val animation = exercise.animation
    if (animation == null) {
        IconBubble(icon = VqIcons.Walk, size = 56, modifier = modifier)
        return
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.size(width = 64.dp, height = 56.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            ExerciseFigure(
                animation = animation,
                highlight = MuscleHighlight.segments(exercise.muscles),
                // The row text names the exercise; the thumbnail is decorative.
                contentDescription = "",
                frame = 0,
                modifier = Modifier.size(width = 60.dp, height = 52.dp),
            )
        }
    }
}

@Composable
fun ExerciseRow(
    exercise: Exercise,
    supporting: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    ListItem(
        headlineContent = { Text(exercise.name.text()) },
        supportingContent = { Text(supporting) },
        leadingContent = { ExerciseThumbnail(exercise) },
        trailingContent = trailing?.let { { Text(it, style = MaterialTheme.typography.labelLarge) } },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier.clickable(onClick = onClick),
    )
}

@Composable
fun exerciseSummary(exercise: Exercise): String =
    stringResource(R.string.exercise_summary, stringResource(exercise.level.labelRes), doseText(exercise.dose))
