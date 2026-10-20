package io.github.vitoosh.vitquest.feature.weight

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.listSaver
import kotlin.math.abs
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.DataSourceKind
import io.github.vitoosh.vitquest.core.designsystem.component.DataSourceTag
import io.github.vitoosh.vitquest.core.designsystem.component.NumberField
import io.github.vitoosh.vitquest.core.designsystem.component.SectionHeader
import io.github.vitoosh.vitquest.core.designsystem.component.VqTopAppBar
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.model.BodyMeasurement
import io.github.vitoosh.vitquest.core.model.WeightEntry
import io.github.vitoosh.vitquest.core.ui.dayLabel
import io.github.vitoosh.vitquest.core.ui.formatDecimal
import io.github.vitoosh.vitquest.core.ui.messageRes
import java.time.LocalDate

private enum class WeightRange(val days: Long?, val labelRes: Int) {
    DAYS_30(30, R.string.weight_range_30),
    DAYS_90(90, R.string.weight_range_90),
    ALL(null, R.string.weight_range_all),
}

@Composable
fun WeightRoute(
    onNavigateBack: () -> Unit,
    viewModel: WeightViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WeightScreen(
        uiState = uiState,
        onSaveWeight = viewModel::saveWeight,
        onDeleteWeight = viewModel::deleteWeight,
        onSaveMeasurement = viewModel::saveMeasurement,
        onDeleteMeasurement = viewModel::deleteMeasurement,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun WeightScreen(
    uiState: WeightUiState?,
    onSaveWeight: (LocalDate, String, String) -> FieldError?,
    onDeleteWeight: (WeightEntry) -> Unit,
    onSaveMeasurement: (MeasurementForm) -> MeasurementErrors,
    onDeleteMeasurement: (BodyMeasurement) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var range by rememberSaveable { mutableIntStateOf(WeightRange.DAYS_90.ordinal) }
    // Date being edited as ISO text; "new" opens the dialog for today.
    var editingDate by rememberSaveable { mutableStateOf<String?>(null) }
    var addingMeasurement by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { VqTopAppBar(title = stringResource(R.string.weight_title), onNavigateBack = onNavigateBack) },
        floatingActionButton = {
            if (uiState != null) {
                ExtendedFloatingActionButton(
                    onClick = { editingDate = uiState.today.toString() },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.weight_log)) },
                )
            }
        },
    ) { padding ->
        if (uiState == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val summary = uiState.summary
        val selectedRange = WeightRange.entries[range]
        val chartEntries = selectedRange.days?.let { days ->
            uiState.entries.filter { !it.date.isBefore(uiState.today.minusDays(days)) }
        } ?: uiState.entries

        CenteredContent(modifier = Modifier.padding(padding)) {
            LazyColumn(
                contentPadding = PaddingValues(start = VqSpacing.lg, end = VqSpacing.lg, top = VqSpacing.lg, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(VqSpacing.md),
            ) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(modifier = Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                            if (summary.latest == null) {
                                Text(stringResource(R.string.weight_empty), style = MaterialTheme.typography.bodyMedium)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.value_kg, formatDecimal(summary.latest.weightKg)),
                                        style = MaterialTheme.typography.headlineMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    DataSourceTag(DataSourceKind.MANUAL)
                                }
                                Text(
                                    text = dayLabel(summary.latest.date, uiState.today),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                uiState.targetKg?.let { target ->
                                    Text(stringResource(R.string.weight_target_value, formatDecimal(target)), style = MaterialTheme.typography.bodyMedium)
                                }
                                summary.progressToTarget?.let { progress ->
                                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                                    Text(
                                        text = stringResource(R.string.weight_progress, (progress * 100).toInt()),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(modifier = Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
                            SectionHeader(stringResource(R.string.weight_trend))
                            Row(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                                WeightRange.entries.forEach { option ->
                                    FilterChip(
                                        selected = option.ordinal == range,
                                        onClick = { range = option.ordinal },
                                        label = { Text(stringResource(option.labelRes)) },
                                    )
                                }
                            }
                            WeightChart(entries = chartEntries, targetKg = uiState.targetKg)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
                                Stat(stringResource(R.string.weight_this_week), summary.thisWeekAverageKg?.let { kg(it) }, Modifier.weight(1f))
                                Stat(stringResource(R.string.weight_last_week), summary.lastWeekAverageKg?.let { kg(it) }, Modifier.weight(1f))
                                Stat(stringResource(R.string.weight_30_days), summary.last30DaysChangeKg?.let { signedKg(it) }, Modifier.weight(1f))
                            }
                            summary.weeklyPaceKg?.let { pace ->
                                Text(stringResource(R.string.weight_pace, signedKg(pace)), style = MaterialTheme.typography.bodyMedium)
                            }
                            if (summary.fastLossWarning) {
                                Row(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Text(stringResource(R.string.weight_fast_loss_warning), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Text(
                                text = stringResource(R.string.weight_guidance),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionHeader(stringResource(R.string.measurement_title), modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { addingMeasurement = true }) { Text(stringResource(R.string.measurement_log)) }
                    }
                }
                if (uiState.measurements.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.measurement_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(uiState.measurements.sortedByDescending { it.date }.take(MEASUREMENTS_SHOWN), key = { "m-${it.id}" }) { item ->
                    ListItem(
                        headlineContent = { Text(dayLabel(item.date, uiState.today)) },
                        supportingContent = { Text(measurementText(item)) },
                        trailingContent = {
                            IconButton(onClick = { onDeleteMeasurement(item) }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                            }
                        },
                    )
                }
                item { SectionHeader(stringResource(R.string.weight_history)) }
                val newestFirst = uiState.entries.sortedByDescending { it.date }
                items(newestFirst, key = { "w-${it.id}" }) { entry ->
                    val index = newestFirst.indexOf(entry)
                    val previous = newestFirst.getOrNull(index + 1)
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.value_kg, formatDecimal(entry.weightKg))) },
                        supportingContent = {
                            Text(listOfNotNull(dayLabel(entry.date, uiState.today), entry.note).joinToString(" · "))
                        },
                        trailingContent = previous?.let { { Text(signedKg(entry.weightKg - it.weightKg)) } },
                        modifier = Modifier.clickable(role = Role.Button) { editingDate = entry.date.toString() },
                    )
                }
            }
        }

        editingDate?.let { LocalDate.parse(it) }?.let { date ->
            WeightDialog(
                date = date,
                today = uiState.today,
                existing = uiState.entries.firstOrNull { it.date == date },
                onSave = { weight, note -> onSaveWeight(date, weight, note).also { if (it == null) editingDate = null } },
                onDelete = { entry ->
                    onDeleteWeight(entry)
                    editingDate = null
                },
                onDismiss = { editingDate = null },
            )
        }
        if (addingMeasurement) {
            MeasurementDialog(
                onSave = { form -> onSaveMeasurement(form).also { if (it.isEmpty) addingMeasurement = false } },
                onDismiss = { addingMeasurement = false },
            )
        }
    }
}

private const val MEASUREMENTS_SHOWN = 5

@Composable
private fun Stat(label: String, value: String?, modifier: Modifier) {
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Text(text = value ?: "–", style = MaterialTheme.typography.titleSmall)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun kg(value: Double) = stringResource(R.string.value_kg, formatDecimal(value))

@Composable
private fun signedKg(value: Double): String {
    val sign = when {
        value > 0.05 -> "+"
        value < -0.05 -> "−"
        else -> "±"
    }
    return sign + stringResource(R.string.value_kg, formatDecimal(abs(value)))
}

@Composable
private fun measurementText(item: BodyMeasurement): String = listOfNotNull(
    item.waistCm?.let { stringResource(R.string.measurement_waist_short, formatDecimal(it)) },
    item.hipCm?.let { stringResource(R.string.measurement_hip_short, formatDecimal(it)) },
    item.chestCm?.let { stringResource(R.string.measurement_chest_short, formatDecimal(it)) },
    item.armCm?.let { stringResource(R.string.measurement_arm_short, formatDecimal(it)) },
    item.thighCm?.let { stringResource(R.string.measurement_thigh_short, formatDecimal(it)) },
).joinToString(" · ")

@Composable
private fun WeightDialog(
    date: LocalDate,
    today: LocalDate,
    existing: WeightEntry?,
    onSave: (String, String) -> FieldError?,
    onDelete: (WeightEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    var weight by rememberSaveable(date) { mutableStateOf(existing?.weightKg?.let { plain(it) }.orEmpty()) }
    var note by rememberSaveable(date) { mutableStateOf(existing?.note.orEmpty()) }
    var error by rememberSaveable(date) { mutableStateOf<FieldError?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.weight_dialog_title, dayLabel(date, today))) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).imePadding(),
                verticalArrangement = Arrangement.spacedBy(VqSpacing.md),
            ) {
                NumberField(
                    value = weight,
                    onValueChange = {
                        weight = it
                        error = null
                    },
                    label = stringResource(R.string.field_current_weight),
                    unit = stringResource(R.string.unit_kg),
                    allowDecimal = true,
                    errorText = error?.let { stringResource(it.messageRes) },
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(NOTE_MAX_LENGTH) },
                    label = { Text(stringResource(R.string.weight_note)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.weight_dialog_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = { error = onSave(weight, note) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = {
            Row {
                if (existing != null) {
                    TextButton(onClick = { onDelete(existing) }) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}

private const val NOTE_MAX_LENGTH = 120

private fun plain(value: Double) = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

@Composable
private fun MeasurementDialog(onSave: (MeasurementForm) -> MeasurementErrors, onDismiss: () -> Unit) {
    var form by rememberSaveable(stateSaver = measurementSaver) { mutableStateOf(MeasurementForm()) }
    var errors by remember { mutableStateOf(MeasurementErrors()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.measurement_log)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(VqSpacing.sm),
            ) {
                Text(stringResource(R.string.measurement_help), style = MaterialTheme.typography.bodySmall)
                MeasureField(form.waist, R.string.measurement_waist, errors.waist) { form = form.copy(waist = it) }
                MeasureField(form.hip, R.string.measurement_hip, errors.hip) { form = form.copy(hip = it) }
                MeasureField(form.chest, R.string.measurement_chest, errors.chest) { form = form.copy(chest = it) }
                MeasureField(form.arm, R.string.measurement_arm, errors.arm) { form = form.copy(arm = it) }
                MeasureField(form.thigh, R.string.measurement_thigh, errors.thigh, ImeAction.Done) { form = form.copy(thigh = it) }
                if (errors.allEmpty) {
                    Text(stringResource(R.string.measurement_need_one), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = { errors = onSave(form) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun MeasureField(value: String, labelRes: Int, error: FieldError?, imeAction: ImeAction = ImeAction.Next, onChange: (String) -> Unit) {
    NumberField(
        value = value,
        onValueChange = onChange,
        label = stringResource(labelRes),
        unit = stringResource(R.string.unit_cm),
        allowDecimal = true,
        errorText = error?.let { stringResource(it.messageRes) },
        imeAction = imeAction,
    )
}

private val measurementSaver = listSaver<MeasurementForm, String>(
    save = { listOf(it.waist, it.hip, it.chest, it.arm, it.thigh) },
    restore = { MeasurementForm(it[0], it[1], it[2], it[3], it[4]) },
)
