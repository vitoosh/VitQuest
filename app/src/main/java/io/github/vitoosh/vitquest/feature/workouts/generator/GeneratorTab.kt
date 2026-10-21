package io.github.vitoosh.vitquest.feature.workouts.generator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.DataSourceKind
import io.github.vitoosh.vitquest.core.designsystem.component.DataSourceTag
import io.github.vitoosh.vitquest.core.designsystem.component.MultiChoiceChips
import io.github.vitoosh.vitquest.core.designsystem.component.SectionHeader
import io.github.vitoosh.vitquest.core.designsystem.component.SingleChoiceChips
import io.github.vitoosh.vitquest.core.designsystem.icon.VqIcons
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.domain.workout.WorkoutGenerator
import io.github.vitoosh.vitquest.core.model.Equipment
import io.github.vitoosh.vitquest.core.model.FitnessExperience
import io.github.vitoosh.vitquest.core.model.WorkoutBlock
import io.github.vitoosh.vitquest.core.model.WorkoutFocus
import io.github.vitoosh.vitquest.core.model.WorkoutLocation
import io.github.vitoosh.vitquest.core.model.WorkoutRequest
import io.github.vitoosh.vitquest.core.model.WorkoutStep
import io.github.vitoosh.vitquest.core.ui.formatInteger
import io.github.vitoosh.vitquest.core.ui.labelRes
import io.github.vitoosh.vitquest.feature.workouts.ExerciseRow
import io.github.vitoosh.vitquest.feature.workouts.doseText
import io.github.vitoosh.vitquest.feature.workouts.labelRes
import io.github.vitoosh.vitquest.feature.workouts.minutesText
import kotlin.math.roundToInt

private val LOCATIONS = listOf(WorkoutLocation.HOME, WorkoutLocation.OFFICE, WorkoutLocation.OUTDOOR)

@Composable
fun GeneratorTab(
    onStart: (WorkoutRequest) -> Unit,
    onOpenExercise: (String) -> Unit,
    viewModel: WorkoutGeneratorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refreshToday()
        onPauseOrDispose { }
    }
    GeneratorContent(
        uiState = uiState,
        actions = GeneratorActions(
            setMinutes = viewModel::setMinutes,
            setLevel = viewModel::setLevel,
            setLocation = viewModel::setLocation,
            setFocus = viewModel::setFocus,
            toggleEquipment = viewModel::toggleEquipment,
            shuffle = viewModel::shuffle,
            start = onStart,
            openExercise = onOpenExercise,
        ),
    )
}

data class GeneratorActions(
    val setMinutes: (Int) -> Unit = {},
    val setLevel: (FitnessExperience) -> Unit = {},
    val setLocation: (WorkoutLocation) -> Unit = {},
    val setFocus: (WorkoutFocus) -> Unit = {},
    val toggleEquipment: (Equipment) -> Unit = {},
    val shuffle: () -> Unit = {},
    val start: (WorkoutRequest) -> Unit = {},
    val openExercise: (String) -> Unit = {},
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneratorContent(uiState: GeneratorUiState?, actions: GeneratorActions) {
    if (uiState == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val request = uiState.request
    val plan = uiState.plan
    CenteredContent {
        LazyColumn(
            contentPadding = PaddingValues(VqSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VqSpacing.md),
        ) {
            item { TodayCard(uiState) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                        Text(stringResource(R.string.generator_plan_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(
                                R.string.generator_plan_summary,
                                minutesText(plan.totalSeconds),
                                pluralStringResource(R.plurals.exercise_count, plan.exerciseCount, plan.exerciseCount),
                                pluralStringResource(R.plurals.round_count, rounds(plan.steps), rounds(plan.steps)),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        uiState.estimatedKcal?.let { kcal ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                                Text(
                                    text = stringResource(R.string.generator_plan_kcal, formatInteger(kcal.roundToInt())),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                DataSourceTag(DataSourceKind.ESTIMATE)
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                            Button(onClick = { actions.start(request) }, enabled = plan.steps.isNotEmpty()) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Text(stringResource(R.string.generator_start), modifier = Modifier.padding(start = VqSpacing.xs))
                            }
                            OutlinedButton(onClick = actions.shuffle) {
                                Icon(VqIcons.Shuffle, contentDescription = null)
                                Text(stringResource(R.string.generator_shuffle), modifier = Modifier.padding(start = VqSpacing.xs))
                            }
                        }
                    }
                }
            }
            item {
                SingleChoiceChips(
                    title = stringResource(R.string.generator_duration),
                    options = WorkoutGenerator.DURATIONS,
                    selected = request.minutes,
                    onSelect = { it?.let(actions.setMinutes) },
                    optionLabel = { stringResource(R.string.value_minutes, formatInteger(it)) },
                )
            }
            item {
                SingleChoiceChips(
                    title = stringResource(R.string.generator_level),
                    options = FitnessExperience.entries,
                    selected = request.level,
                    onSelect = { it?.let(actions.setLevel) },
                    optionLabel = { stringResource(it.labelRes) },
                )
            }
            item {
                SingleChoiceChips(
                    title = stringResource(R.string.generator_location),
                    options = LOCATIONS,
                    selected = request.location,
                    onSelect = { it?.let(actions.setLocation) },
                    optionLabel = { stringResource(it.labelRes) },
                )
            }
            item {
                SingleChoiceChips(
                    title = stringResource(R.string.generator_focus),
                    options = WorkoutFocus.entries,
                    selected = request.focus,
                    onSelect = { it?.let(actions.setFocus) },
                    optionLabel = { stringResource(it.labelRes) },
                )
            }
            item {
                MultiChoiceChips(
                    title = stringResource(R.string.generator_equipment),
                    options = Equipment.entries,
                    selected = request.equipment,
                    onToggle = actions.toggleEquipment,
                    optionLabel = { stringResource(it.labelRes) },
                    supportingText = stringResource(
                        if (request.location == WorkoutLocation.OFFICE) R.string.generator_equipment_office else R.string.generator_equipment_help,
                    ),
                )
            }
            WorkoutBlock.entries.forEach { block ->
                val steps = plan.steps.withIndex().filter { it.value.block == block }
                if (steps.isEmpty()) return@forEach
                item(key = "header-$block") { SectionHeader(stringResource(block.labelRes)) }
                if (block == WorkoutBlock.MAIN) {
                    item(key = "main-note") {
                        Text(
                            text = stringResource(R.string.generator_main_note, formatInteger(steps.first().value.workSeconds), formatInteger(steps.first().value.restSeconds)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                // Each circuit exercise is listed once; rounds repeat it.
                val unique = steps.distinctBy { it.value.exercise.key }
                itemsIndexed(unique, key = { _, step -> "$block-${step.index}" }) { _, step ->
                    PlanStepRow(step.value, rounds = steps.count { it.value.exercise.key == step.value.exercise.key }, onOpen = actions.openExercise)
                }
            }
            item {
                Text(
                    text = stringResource(R.string.workout_safety_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun rounds(steps: List<WorkoutStep>): Int = steps.filter { it.block == WorkoutBlock.MAIN }.maxOfOrNull { it.round } ?: 0

@Composable
private fun TodayCard(state: GeneratorUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.xs)) {
            Text(
                text = if (state.todaySessions > 0) {
                    stringResource(R.string.workout_today_done, minutesText(state.todayActiveSeconds))
                } else {
                    stringResource(R.string.workout_today_none)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = pluralStringResource(R.plurals.workout_week_days, state.weekWorkoutDays, state.weekWorkoutDays),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlanStepRow(step: WorkoutStep, rounds: Int, onOpen: (String) -> Unit) {
    val time = stringResource(R.string.value_seconds, formatInteger(step.workSeconds))
    ExerciseRow(
        exercise = step.exercise,
        supporting = if (step.block == WorkoutBlock.MAIN) {
            stringResource(R.string.plan_step_main, time, doseText(step.exercise.dose))
        } else {
            time
        },
        trailing = if (rounds > 1) stringResource(R.string.plan_step_rounds, formatInteger(rounds)) else null,
        onClick = { onOpen(step.exercise.key) },
    )
}
