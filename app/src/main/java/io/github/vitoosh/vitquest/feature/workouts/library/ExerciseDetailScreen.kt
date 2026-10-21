package io.github.vitoosh.vitquest.feature.workouts.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.IconBubble
import io.github.vitoosh.vitquest.core.designsystem.component.SectionHeader
import io.github.vitoosh.vitquest.core.designsystem.component.VqTopAppBar
import io.github.vitoosh.vitquest.core.designsystem.icon.VqIcons
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.domain.workout.ExerciseCatalog
import io.github.vitoosh.vitquest.core.domain.workout.MuscleHighlight
import io.github.vitoosh.vitquest.core.model.Bilingual
import io.github.vitoosh.vitquest.core.model.Exercise
import io.github.vitoosh.vitquest.core.model.ExerciseAnimation
import io.github.vitoosh.vitquest.core.ui.formatInteger
import io.github.vitoosh.vitquest.core.ui.labelRes
import io.github.vitoosh.vitquest.feature.workouts.animation.ExerciseFigure
import io.github.vitoosh.vitquest.feature.workouts.animation.systemAnimationsEnabled
import io.github.vitoosh.vitquest.feature.workouts.doseText
import io.github.vitoosh.vitquest.feature.workouts.labelRes
import io.github.vitoosh.vitquest.feature.workouts.text
import io.github.vitoosh.vitquest.ui.navigation.ExerciseDetailRoute
import javax.inject.Inject

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {
    val exercise: Exercise? = ExerciseCatalog.find(savedStateHandle.toRoute<ExerciseDetailRoute>().key)
}

@Composable
fun ExerciseDetailRoute(onNavigateBack: () -> Unit, viewModel: ExerciseDetailViewModel = hiltViewModel()) {
    ExerciseDetailScreen(exercise = viewModel.exercise, onNavigateBack = onNavigateBack)
}

@Composable
fun ExerciseDetailScreen(exercise: Exercise?, onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = { VqTopAppBar(title = exercise?.name?.text() ?: stringResource(R.string.exercise_title), onNavigateBack = onNavigateBack) },
    ) { padding ->
        if (exercise == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.exercise_not_found))
            }
            return@Scaffold
        }
        CenteredContent(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(VqSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(VqSpacing.lg),
            ) {
                val animation = exercise.animation
                if (animation != null) {
                    ExerciseTutorial(exercise, animation)
                } else {
                    EverydayMovementCard()
                }
                ExerciseFacts(exercise)
                Text(exercise.description.text(), style = MaterialTheme.typography.bodyLarge)
                NumberedSection(stringResource(R.string.exercise_steps), exercise.steps)
                BulletSection(stringResource(R.string.exercise_modifications), exercise.modifications)
                BulletSection(stringResource(R.string.exercise_safety), exercise.safety)
                Text(
                    text = stringResource(R.string.workout_safety_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The animated figure with a still start/end view for when motion is off or the user wants to study a pose. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseTutorial(exercise: Exercise, animation: ExerciseAnimation, modifier: Modifier = Modifier) {
    val animationsOn = systemAnimationsEnabled()
    // null = playing; 0 = start pose; 1 = end pose.
    var frame by rememberSaveable { mutableStateOf(if (animationsOn) null else 0) }
    val highlight = MuscleHighlight.segments(exercise.muscles)
    val name = exercise.name.text()
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = modifier) {
        Column(Modifier.padding(VqSpacing.md), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
            ExerciseFigure(
                animation = animation,
                highlight = highlight,
                contentDescription = when (frame) {
                    null -> stringResource(R.string.exercise_animation_description, name)
                    0 -> stringResource(R.string.exercise_start_pose_description, name)
                    else -> stringResource(R.string.exercise_end_pose_description, name)
                },
                frame = frame,
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp).align(Alignment.CenterHorizontally),
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = buildList {
                    if (animationsOn) add(null)
                    add(0)
                    add(1)
                }
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = frame == option,
                        onClick = { frame = option },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        icon = if (option == null) {
                            { Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else {
                            { SegmentedButtonDefaults.Icon(frame == option) }
                        },
                    ) {
                        Text(
                            stringResource(
                                when (option) {
                                    null -> R.string.exercise_play
                                    0 -> R.string.exercise_start_pose
                                    else -> R.string.exercise_end_pose
                                },
                            ),
                        )
                    }
                }
            }
            Text(stringResource(R.string.exercise_muscles), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(VqSpacing.md), verticalArrangement = Arrangement.spacedBy(VqSpacing.xs)) {
                exercise.muscles.forEach { muscle ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VqSpacing.xs)) {
                        Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text(stringResource(muscle.labelRes), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (!animationsOn) {
                Text(
                    text = stringResource(R.string.exercise_animations_off),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EverydayMovementCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.padding(VqSpacing.lg), horizontalArrangement = Arrangement.spacedBy(VqSpacing.md), verticalAlignment = Alignment.CenterVertically) {
            IconBubble(icon = VqIcons.Walk, size = 48)
            Text(stringResource(R.string.exercise_no_animation), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ExerciseFacts(exercise: Exercise) {
    val none = stringResource(R.string.exercise_equipment_none)
    val required = exercise.requiredEquipment.map { stringResource(it.labelRes) }
    val recommended = exercise.recommendedEquipment.map { stringResource(it.labelRes) }
    val equipment = when {
        required.isNotEmpty() -> required.joinToString()
        recommended.isNotEmpty() -> stringResource(R.string.exercise_equipment_optional, recommended.joinToString())
        else -> none
    }
    Column(verticalArrangement = Arrangement.spacedBy(VqSpacing.xs)) {
        Fact(stringResource(R.string.exercise_type), stringResource(exercise.kind.labelRes))
        Fact(stringResource(R.string.exercise_level), stringResource(exercise.level.labelRes))
        Fact(stringResource(R.string.exercise_recommended), doseText(exercise.dose))
        Fact(stringResource(R.string.exercise_equipment), equipment)
        Fact(stringResource(R.string.exercise_locations), exercise.locations.sortedBy { it.ordinal }.map { stringResource(it.labelRes) }.joinToString())
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.4f))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.6f))
    }
}

@Composable
private fun NumberedSection(title: String, items: List<Bilingual>) {
    Column(verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
        SectionHeader(title)
        items.forEachIndexed { index, item ->
            Row(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                Text(formatInteger(index + 1) + ".", style = MaterialTheme.typography.bodyMedium)
                Text(item.text(), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun BulletSection(title: String, items: List<Bilingual>) {
    Column(verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        items.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                Text("•", style = MaterialTheme.typography.bodyMedium)
                Text(item.text(), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
