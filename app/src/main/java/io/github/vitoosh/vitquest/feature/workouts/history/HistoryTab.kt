package io.github.vitoosh.vitquest.feature.workouts.history

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.DataSourceKind
import io.github.vitoosh.vitquest.core.designsystem.component.DataSourceTag
import io.github.vitoosh.vitquest.core.designsystem.component.NumberField
import io.github.vitoosh.vitquest.core.designsystem.component.SectionHeader
import io.github.vitoosh.vitquest.core.designsystem.component.VqBadge
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.model.StepStatus
import io.github.vitoosh.vitquest.core.model.WorkoutIntensity
import io.github.vitoosh.vitquest.core.model.WorkoutSession
import io.github.vitoosh.vitquest.core.model.WorkoutSource
import io.github.vitoosh.vitquest.core.ui.dayLabel
import io.github.vitoosh.vitquest.core.ui.formatInteger
import io.github.vitoosh.vitquest.core.ui.messageRes
import io.github.vitoosh.vitquest.feature.workouts.labelRes
import io.github.vitoosh.vitquest.feature.workouts.minutesText
import io.github.vitoosh.vitquest.feature.workouts.text
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun HistoryTab(viewModel: HistoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refreshToday()
        onPauseOrDispose { }
    }
    HistoryContent(uiState, viewModel::logManual, viewModel::delete)
}

@Composable
fun HistoryContent(
    uiState: HistoryUiState?,
    onLogManual: (LocalDate, String, WorkoutIntensity, String) -> FieldError?,
    onDelete: (WorkoutSession) -> Unit,
) {
    var logging by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf<Long?>(null) }
    if (uiState == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    CenteredContent {
        LazyColumn(contentPadding = PaddingValues(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                        Text(stringResource(R.string.history_this_week), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(
                                R.string.history_week_summary,
                                pluralStringResource(R.plurals.session_count, uiState.weekSessions, uiState.weekSessions),
                                minutesText(uiState.weekActiveSeconds),
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        FilledTonalButton(onClick = { logging = true }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Text(stringResource(R.string.history_log_manual), modifier = Modifier.padding(start = VqSpacing.xs))
                        }
                    }
                }
            }
            if (uiState.sessions.isEmpty()) {
                item { Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.bodyMedium) }
            }
            uiState.sessions.groupBy { it.date }.forEach { (date, sessions) ->
                item(key = "date-$date") { SectionHeader(dayLabel(date, uiState.today)) }
                items(sessions, key = { it.id }) { session -> SessionCard(session, onDelete = { deleting = session.id }) }
            }
            item {
                Text(
                    text = stringResource(R.string.history_correction_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (logging) {
        ManualWorkoutDialog(
            today = uiState.today,
            onSave = { date, minutes, intensity, note -> onLogManual(date, minutes, intensity, note).also { if (it == null) logging = false } },
            onDismiss = { logging = false },
        )
    }
    deleting?.let { id ->
        val session = uiState.sessions.firstOrNull { it.id == id }
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.history_delete_title)) },
            text = { Text(stringResource(R.string.history_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    session?.let(onDelete)
                    deleting = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun SessionCard(session: WorkoutSession, onDelete: () -> Unit) {
    var expanded by rememberSaveable(session.id) { mutableStateOf(false) }
    val title = when {
        session.source == WorkoutSource.MANUAL -> session.note ?: stringResource(R.string.history_manual_title)
        session.focus != null -> stringResource(R.string.history_session_title, stringResource(session.focus.labelRes))
        else -> stringResource(R.string.history_manual_title)
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(VqSpacing.md), verticalArrangement = Arrangement.spacedBy(VqSpacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VqSpacing.xs)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(sessionDetails(session), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (session.exercises.isNotEmpty()) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(if (expanded) R.string.history_hide_exercises else R.string.history_show_exercises),
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                if (session.source == WorkoutSource.MANUAL) DataSourceTag(DataSourceKind.MANUAL)
                if (!session.completed) {
                    VqBadge(
                        text = stringResource(R.string.history_ended_early),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                session.estimatedKcal?.let {
                    VqBadge(
                        text = stringResource(R.string.history_kcal_estimate, formatInteger(it.roundToInt())),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            if (expanded) {
                session.exercises.forEach { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                        Text(item.name.text(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(
                            text = when (item.status) {
                                StepStatus.COMPLETED -> stringResource(R.string.value_seconds, formatInteger(item.workedSeconds))
                                StepStatus.SKIPPED -> stringResource(R.string.history_step_skipped)
                                StepStatus.NOT_REACHED -> stringResource(R.string.history_step_not_reached)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun sessionDetails(session: WorkoutSession): String = when (session.source) {
    WorkoutSource.MANUAL -> stringResource(
        R.string.history_manual_details,
        minutesText(session.activeSeconds),
        session.intensity?.let { stringResource(it.labelRes) } ?: "",
    )
    WorkoutSource.PLAYER -> stringResource(
        R.string.history_player_details,
        minutesText(session.activeSeconds),
        formatInteger(session.exercisesCompleted),
        formatInteger(session.exercisesTotal),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManualWorkoutDialog(
    today: LocalDate,
    onSave: (LocalDate, String, WorkoutIntensity, String) -> FieldError?,
    onDismiss: () -> Unit,
) {
    var dayOffset by rememberSaveable { mutableLongStateOf(0L) }
    var minutes by rememberSaveable { mutableStateOf("") }
    var intensity by rememberSaveable { mutableStateOf(WorkoutIntensity.MODERATE) }
    var note by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<FieldError?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history_log_manual)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                    (0L..2L).forEach { offset ->
                        FilterChip(selected = dayOffset == offset, onClick = { dayOffset = offset }, label = { Text(dayLabel(today.minusDays(offset), today)) })
                    }
                }
                NumberField(
                    value = minutes,
                    onValueChange = {
                        minutes = it
                        error = null
                    },
                    label = stringResource(R.string.history_minutes),
                    unit = stringResource(R.string.unit_minutes),
                    errorText = error?.let { stringResource(it.messageRes) },
                    imeAction = ImeAction.Next,
                )
                Text(stringResource(R.string.history_intensity), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                    WorkoutIntensity.entries.forEach { option ->
                        FilterChip(selected = intensity == option, onClick = { intensity = option }, label = { Text(stringResource(option.labelRes)) })
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(60) },
                    label = { Text(stringResource(R.string.history_note)) },
                    placeholder = { Text(stringResource(R.string.history_note_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { error = onSave(today.minusDays(dayOffset), minutes, intensity, note) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
