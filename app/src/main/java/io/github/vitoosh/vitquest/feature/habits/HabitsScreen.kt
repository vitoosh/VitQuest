package io.github.vitoosh.vitquest.feature.habits

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.SectionHeader
import io.github.vitoosh.vitquest.core.designsystem.component.VqTopAppBar
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.domain.habits.DayStatus
import io.github.vitoosh.vitquest.core.domain.habits.HabitWeek
import io.github.vitoosh.vitquest.core.model.BuiltInHabit
import io.github.vitoosh.vitquest.core.model.CustomHabit
import io.github.vitoosh.vitquest.core.ui.dayLabel
import io.github.vitoosh.vitquest.core.ui.formatInteger
import io.github.vitoosh.vitquest.core.ui.messageRes
import io.github.vitoosh.vitquest.core.ui.shortWeekday
import java.time.LocalDate

@get:StringRes
val BuiltInHabit.titleRes: Int
    get() = when (this) {
        BuiltInHabit.MOVEMENT -> R.string.habit_movement
        BuiltInHabit.WORKOUT -> R.string.habit_workout
        BuiltInHabit.HYDRATION -> R.string.habit_hydration
        BuiltInHabit.FOOD_LOG -> R.string.habit_food_log
    }

@get:StringRes
val BuiltInHabit.descriptionRes: Int
    get() = when (this) {
        BuiltInHabit.MOVEMENT -> R.string.habit_movement_description
        BuiltInHabit.WORKOUT -> R.string.habit_workout_description
        BuiltInHabit.HYDRATION -> R.string.habit_hydration_description
        BuiltInHabit.FOOD_LOG -> R.string.habit_food_log_description
    }

data class HabitActions(
    val toggle: (CustomHabit, LocalDate) -> Unit = { _, _ -> },
    val save: (CustomHabit?, String, Int) -> FieldError? = { _, _, _ -> null },
    val delete: (CustomHabit) -> Unit = {},
)

@Composable
fun HabitsRoute(onNavigateBack: () -> Unit, viewModel: HabitsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refreshToday()
        onPauseOrDispose { }
    }
    HabitsScreen(uiState, HabitActions(viewModel::toggle, viewModel::save, viewModel::delete), onNavigateBack)
}

@Composable
fun HabitsScreen(uiState: HabitsUiState?, actions: HabitActions, onNavigateBack: () -> Unit) {
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleting by rememberSaveable { mutableStateOf<Long?>(null) }
    Scaffold(topBar = { VqTopAppBar(title = stringResource(R.string.habits_title), onNavigateBack = onNavigateBack) }) { padding ->
        if (uiState == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        CenteredContent(modifier = Modifier.padding(padding)) {
            LazyColumn(contentPadding = PaddingValues(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.xs)) {
                            Text(
                                text = stringResource(R.string.habits_today, formatInteger(uiState.todayDone), formatInteger(uiState.todayTotal)),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(stringResource(R.string.habits_no_streaks), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                item { SectionHeader(stringResource(R.string.habits_automatic)) }
                items(uiState.builtIns, key = { it.habit.name }) { item ->
                    HabitCard(
                        title = stringResource(item.habit.titleRes),
                        description = stringResource(item.habit.descriptionRes),
                        week = item.week,
                        weeksOnTarget = item.weeksOnTarget,
                        today = uiState.today,
                        missingGoal = item.doneToday == null,
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.habits_automatic_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item { SectionHeader(stringResource(R.string.habits_custom)) }
                if (uiState.customs.isEmpty()) {
                    item { Text(stringResource(R.string.habits_custom_empty), style = MaterialTheme.typography.bodyMedium) }
                }
                items(uiState.customs, key = { it.habit.id }) { item ->
                    HabitCard(
                        title = item.habit.name,
                        description = stringResource(R.string.habits_tap_to_check),
                        week = item.week,
                        weeksOnTarget = item.weeksOnTarget,
                        today = uiState.today,
                        onToggle = { date -> actions.toggle(item.habit, date) },
                        onEdit = { editing = item.habit.id },
                        onDelete = { deleting = item.habit.id },
                    )
                }
                item {
                    FilledTonalButton(onClick = { editing = NEW_HABIT }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(stringResource(R.string.habits_add), modifier = Modifier.padding(start = VqSpacing.xs))
                    }
                }
            }
        }
        editing?.let { id ->
            val existing = uiState.customs.firstOrNull { it.habit.id == id }?.habit
            HabitDialog(
                existing = existing,
                onSave = { name, days -> actions.save(existing, name, days).also { if (it == null) editing = null } },
                onDismiss = { editing = null },
            )
        }
        deleting?.let { id ->
            val habit = uiState.customs.firstOrNull { it.habit.id == id }?.habit
            AlertDialog(
                onDismissRequest = { deleting = null },
                title = { Text(stringResource(R.string.habits_delete_title)) },
                text = { Text(stringResource(R.string.habits_delete_message, habit?.name.orEmpty())) },
                confirmButton = {
                    TextButton(onClick = {
                        habit?.let(actions.delete)
                        deleting = null
                    }) { Text(stringResource(R.string.action_delete)) }
                },
                dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) } },
            )
        }
    }
}

private const val NEW_HABIT = -1L

@Composable
private fun HabitCard(
    title: String,
    description: String,
    week: HabitWeek,
    weeksOnTarget: Int,
    today: LocalDate,
    missingGoal: Boolean = false,
    onToggle: ((LocalDate) -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(VqSpacing.md), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                onEdit?.let {
                    IconButton(onClick = it) { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.habits_edit)) }
                }
                onDelete?.let {
                    IconButton(onClick = it) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete)) }
                }
            }
            if (missingGoal) {
                Text(stringResource(R.string.habits_missing_goal), style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            WeekDots(week, today, onToggle)
            Text(
                text = stringResource(
                    R.string.habits_week_progress,
                    formatInteger(week.doneCount),
                    pluralStringResource(R.plurals.habit_target_days, week.targetDays, week.targetDays),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = if (week.targetMet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.habits_weeks_on_target, formatInteger(weeksOnTarget), formatInteger(HabitsOverview.WEEKS_BACK.toInt())),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeekDots(week: HabitWeek, today: LocalDate, onToggle: ((LocalDate) -> Unit)?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        week.days.forEach { (date, status) ->
            val label = dayLabel(date, today)
            val statusText = stringResource(
                when (status) {
                    DayStatus.DONE -> R.string.habit_day_done
                    DayStatus.NOT_DONE -> R.string.habit_day_not_done
                    DayStatus.FUTURE -> R.string.habit_day_future
                    DayStatus.NO_TARGET -> R.string.habit_day_no_target
                },
            )
            val canToggle = onToggle != null && status != DayStatus.FUTURE
            val modifier = if (canToggle) {
                Modifier.toggleable(value = status == DayStatus.DONE, role = Role.Checkbox) { onToggle(date) }
            } else {
                Modifier
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(VqSpacing.xs),
                modifier = modifier
                    .padding(VqSpacing.xs)
                    .clearAndSetSemantics { contentDescription = "$label: $statusText" },
            ) {
                Text(
                    text = shortWeekday(date),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (date == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DayDot(status)
            }
        }
    }
}

@Composable
private fun DayDot(status: DayStatus) {
    val (container, border) = when (status) {
        DayStatus.DONE -> MaterialTheme.colorScheme.primary to null
        DayStatus.NOT_DONE -> Color.Transparent to BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
        DayStatus.FUTURE -> Color.Transparent to BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        DayStatus.NO_TARGET -> MaterialTheme.colorScheme.surfaceContainerHighest to null
    }
    Surface(shape = CircleShape, color = container, border = border, modifier = Modifier.size(28.dp)) {
        if (status == DayStatus.DONE) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitDialog(existing: CustomHabit?, onSave: (String, Int) -> FieldError?, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var days by rememberSaveable { mutableIntStateOf(existing?.targetDaysPerWeek ?: DEFAULT_TARGET_DAYS) }
    var error by rememberSaveable { mutableStateOf<FieldError?>(null) }
    val suggestions = listOf(R.string.habit_suggestion_sleep, R.string.habit_suggestion_stretch, R.string.habit_suggestion_vegetables, R.string.habit_suggestion_no_sugary_drinks)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.habits_add else R.string.habits_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it.take(40)
                        error = null
                    },
                    label = { Text(stringResource(R.string.habits_name)) },
                    isError = error != null,
                    supportingText = error?.let { { Text(stringResource(it.messageRes)) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (existing == null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                        suggestions.forEach { res ->
                            val text = stringResource(res)
                            SuggestionChip(onClick = { name = text }, label = { Text(text) })
                        }
                    }
                }
                Text(stringResource(R.string.habits_target_days), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                    (1..7).forEach { option ->
                        FilterChip(selected = days == option, onClick = { days = option }, label = { Text(formatInteger(option)) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { error = onSave(name, days) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private const val DEFAULT_TARGET_DAYS = 5
