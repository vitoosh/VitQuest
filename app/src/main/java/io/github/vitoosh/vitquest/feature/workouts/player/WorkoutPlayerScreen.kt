package io.github.vitoosh.vitquest.feature.workouts.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.DataSourceKind
import io.github.vitoosh.vitquest.core.designsystem.component.DataSourceTag
import io.github.vitoosh.vitquest.core.designsystem.component.VqTopAppBar
import io.github.vitoosh.vitquest.core.designsystem.icon.VqIcons
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.domain.workout.MuscleHighlight
import io.github.vitoosh.vitquest.core.domain.workout.PlayerPhase
import io.github.vitoosh.vitquest.core.domain.workout.PlayerState
import io.github.vitoosh.vitquest.core.domain.workout.WorkoutPlayerEngine
import io.github.vitoosh.vitquest.core.model.ExerciseDose
import io.github.vitoosh.vitquest.core.model.WorkoutPlan
import io.github.vitoosh.vitquest.core.ui.formatInteger
import io.github.vitoosh.vitquest.feature.workouts.animation.ExerciseFigure
import io.github.vitoosh.vitquest.feature.workouts.animation.systemAnimationsEnabled
import io.github.vitoosh.vitquest.feature.workouts.clockText
import io.github.vitoosh.vitquest.feature.workouts.doseText
import io.github.vitoosh.vitquest.feature.workouts.labelRes
import io.github.vitoosh.vitquest.feature.workouts.minutesText
import io.github.vitoosh.vitquest.feature.workouts.text
import kotlin.math.roundToInt

data class PlayerActions(
    val togglePause: () -> Unit = {},
    val skip: () -> Unit = {},
    val complete: () -> Unit = {},
    val previous: () -> Unit = {},
    val extendRest: () -> Unit = {},
    val finishEarly: () -> Unit = {},
    val close: () -> Unit = {},
)

@Composable
fun WorkoutPlayerRoute(onClose: () -> Unit, viewModel: WorkoutPlayerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val finished by viewModel.finished.collectAsStateWithLifecycle()
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    val requestClose = { if (viewModel.hasProgress()) confirmExit = true else onClose() }
    BackHandler(onBack = requestClose)
    KeepScreenOn()

    WorkoutPlayerScreen(
        plan = viewModel.plan,
        state = state,
        finished = finished,
        actions = PlayerActions(
            togglePause = viewModel::togglePause,
            skip = viewModel::skip,
            complete = viewModel::complete,
            previous = viewModel::previous,
            extendRest = viewModel::extendRest,
            finishEarly = viewModel::finishEarly,
            close = requestClose,
        ),
        onDone = onClose,
    )
    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text(stringResource(R.string.player_exit_title)) },
            text = { Text(stringResource(R.string.player_exit_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmExit = false
                    viewModel.finishEarly()
                }) { Text(stringResource(R.string.player_exit_save)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onClose) { Text(stringResource(R.string.player_exit_discard)) }
                    TextButton(onClick = { confirmExit = false }) { Text(stringResource(R.string.player_exit_continue)) }
                }
            },
        )
    }
}

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
fun WorkoutPlayerScreen(
    plan: WorkoutPlan,
    state: PlayerState,
    finished: FinishedWorkout?,
    actions: PlayerActions,
    onDone: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var lastStep by remember { mutableStateOf(state.phase to state.index) }
    LaunchedEffect(state.phase, state.index) {
        if (lastStep != (state.phase to state.index)) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            lastStep = state.phase to state.index
        }
    }
    val title = when (state.phase) {
        PlayerPhase.FINISHED -> stringResource(R.string.player_summary_title)
        else -> plan.steps.getOrNull(state.index)?.let { stringResource(it.block.labelRes) } ?: ""
    }
    Scaffold(topBar = { VqTopAppBar(title = title, onNavigateBack = if (state.phase == PlayerPhase.FINISHED) onDone else actions.close) }) { padding ->
        CenteredContent(modifier = Modifier.padding(padding)) {
            if (state.phase == PlayerPhase.FINISHED) {
                SummaryContent(finished, state.endedEarly, onDone)
            } else {
                PlayingContent(plan, state, actions)
            }
        }
    }
}

@Composable
private fun PlayingContent(plan: WorkoutPlan, state: PlayerState, actions: PlayerActions) {
    val step = plan.steps[state.index]
    val exercise = step.exercise
    val working = state.phase == PlayerPhase.WORK
    val stepTotalMs = when (state.phase) {
        PlayerPhase.READY -> WorkoutPlayerEngine.READY_MS
        PlayerPhase.REST -> maxOf(plan.steps[state.index - 1].restSeconds * 1000L, state.remainingMs)
        else -> step.workSeconds * 1000L
    }
    val animationsOn = systemAnimationsEnabled()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(VqSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VqSpacing.md),
    ) {
        Text(
            text = stringResource(R.string.player_progress, formatInteger(state.index + 1), formatInteger(plan.steps.size)),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { (state.index + if (working) 0.5f else 0f) / plan.steps.size },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = when (state.phase) {
                PlayerPhase.READY -> stringResource(R.string.player_get_ready)
                PlayerPhase.REST -> stringResource(R.string.player_rest)
                else -> exercise.name.text()
            },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        if (!working) {
            Text(
                text = stringResource(R.string.player_next, exercise.name.text()),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
        exercise.animation?.let { animation ->
            ExerciseFigure(
                animation = animation,
                highlight = MuscleHighlight.segments(exercise.muscles),
                contentDescription = stringResource(R.string.exercise_animation_description, exercise.name.text()),
                frame = if (animationsOn && !state.paused) null else 0,
                modifier = Modifier.fillMaxWidth().widthIn(max = 280.dp),
            )
        }
        Text(
            text = clockText((state.remainingMs + 999) / 1000),
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        LinearProgressIndicator(
            progress = { 1f - state.remainingMs.toFloat() / stepTotalMs.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth(),
            color = if (working) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
        )
        if (state.paused) {
            Text(stringResource(R.string.player_paused), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
        }
        if (working) {
            Text(
                text = when (exercise.dose) {
                    is ExerciseDose.Reps -> stringResource(R.string.player_reps_hint, doseText(exercise.dose))
                    is ExerciseDose.Hold -> stringResource(R.string.player_hold_hint)
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            if (WorkoutPlayerEngine.shouldSwitchSides(state, plan)) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Text(
                        text = stringResource(R.string.player_switch_sides),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = VqSpacing.lg, vertical = VqSpacing.sm).semantics { liveRegion = LiveRegionMode.Assertive },
                    )
                }
            }
            exercise.modifications.firstOrNull()?.let { tip ->
                Text(
                    text = stringResource(R.string.player_easier, tip.text()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VqSpacing.lg)) {
            IconButton(onClick = actions.previous, enabled = state.phase != PlayerPhase.READY) {
                Icon(VqIcons.SkipPrevious, contentDescription = stringResource(R.string.player_previous))
            }
            FilledIconButton(onClick = actions.togglePause, modifier = Modifier.size(72.dp)) {
                Icon(
                    imageVector = if (state.paused) Icons.Filled.PlayArrow else VqIcons.Pause,
                    contentDescription = stringResource(if (state.paused) R.string.player_resume else R.string.player_pause),
                    modifier = Modifier.size(36.dp),
                )
            }
            IconButton(onClick = actions.skip) {
                Icon(VqIcons.SkipNext, contentDescription = stringResource(if (working) R.string.player_skip_exercise else R.string.player_skip_rest))
            }
        }
        if (working && exercise.dose is ExerciseDose.Reps) {
            Button(onClick = actions.complete) { Text(stringResource(R.string.player_complete_set)) }
        }
        if (!working) {
            OutlinedButton(onClick = actions.extendRest) {
                Text(stringResource(R.string.player_extend_rest, formatInteger((WorkoutPlayerEngine.EXTRA_REST_MS / 1000).toInt())))
            }
        }
        TextButton(onClick = actions.finishEarly) { Text(stringResource(R.string.player_finish_early)) }
    }
}

@Composable
private fun SummaryContent(finished: FinishedWorkout?, endedEarly: Boolean, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(VqSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VqSpacing.md),
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        Text(
            text = stringResource(if (endedEarly) R.string.player_summary_ended else R.string.player_summary_done),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        if (finished == null) return@Column
        val summary = finished.summary
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                SummaryRow(stringResource(R.string.summary_active_time), minutesText(summary.activeSeconds) + " (" + clockText(summary.activeSeconds.toLong()) + ")")
                SummaryRow(stringResource(R.string.summary_sets), stringResource(R.string.summary_sets_value, formatInteger(summary.completedSteps), formatInteger(summary.totalSteps)))
                summary.estimatedKcal?.let { kcal ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SummaryRow(stringResource(R.string.summary_energy), stringResource(R.string.value_kcal_approx, formatInteger(kcal.roundToInt())), Modifier.weight(1f))
                        DataSourceTag(DataSourceKind.ESTIMATE)
                    }
                } ?: Text(stringResource(R.string.summary_energy_missing), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            text = stringResource(if (finished.saved) R.string.summary_saved else R.string.summary_not_saved),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.summary_energy_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        FilledTonalButton(onClick = onDone) { Text(stringResource(R.string.action_done)) }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
