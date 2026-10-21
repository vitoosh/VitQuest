package io.github.vitoosh.vitquest.feature.hydration

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import io.github.vitoosh.vitquest.core.designsystem.component.VqTopAppBar
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.model.WaterLog
import io.github.vitoosh.vitquest.core.ui.currentLocale
import io.github.vitoosh.vitquest.core.ui.dayLabel
import io.github.vitoosh.vitquest.core.ui.formatInteger
import io.github.vitoosh.vitquest.core.ui.messageRes
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HydrationRoute(
    onNavigateBack: () -> Unit,
    viewModel: HydrationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refreshToday()
        onPauseOrDispose { }
    }
    HydrationScreen(uiState, viewModel::add, viewModel::addCustom, viewModel::delete, onNavigateBack)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HydrationScreen(
    uiState: HydrationUiState?,
    onAdd: (Int) -> Unit,
    onAddCustom: (String) -> FieldError?,
    onDelete: (WaterLog) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var custom by rememberSaveable { mutableStateOf(false) }
    Scaffold(topBar = { VqTopAppBar(title = stringResource(R.string.hydration_title), onNavigateBack = onNavigateBack) }) { padding ->
        if (uiState == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        CenteredContent(modifier = Modifier.padding(padding)) {
            LazyColumn(
                contentPadding = PaddingValues(VqSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(VqSpacing.md),
            ) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(modifier = Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                            Text(
                                text = uiState.targetMl?.let {
                                    stringResource(R.string.water_of_target, formatInteger(uiState.todayMl), formatInteger(it))
                                } ?: stringResource(R.string.value_ml, formatInteger(uiState.todayMl)),
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            uiState.targetMl?.takeIf { it > 0 }?.let { target ->
                                LinearProgressIndicator(
                                    progress = { (uiState.todayMl.toFloat() / target).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                                    Text(
                                        text = if (uiState.goalReached) {
                                            stringResource(R.string.water_goal_reached)
                                        } else {
                                            stringResource(R.string.water_remaining, formatInteger(target - uiState.todayMl))
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    DataSourceTag(DataSourceKind.ESTIMATE)
                                }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                                listOf(uiState.servingMl, QUICK_BOTTLE_ML).distinct().forEach { amount ->
                                    FilledTonalButton(onClick = { onAdd(amount) }) {
                                        Text(stringResource(R.string.water_add_amount, formatInteger(amount)))
                                    }
                                }
                                OutlinedButton(onClick = { custom = true }) { Text(stringResource(R.string.water_add_custom)) }
                            }
                        }
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.water_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item { SectionHeader(stringResource(R.string.water_today_logs)) }
                if (uiState.todayLogs.isEmpty()) {
                    item { Text(stringResource(R.string.water_no_logs), style = MaterialTheme.typography.bodySmall) }
                }
                items(uiState.todayLogs, key = { it.id }) { log ->
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.value_ml, formatInteger(log.amountMl))) },
                        supportingContent = { Text(timeText(log)) },
                        trailingContent = {
                            IconButton(onClick = { onDelete(log) }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                            }
                        },
                    )
                }
                item { SectionHeader(stringResource(R.string.water_history)) }
                items(uiState.week, key = { "d-${it.date}" }) { day ->
                    ListItem(
                        headlineContent = { Text(dayLabel(day.date, uiState.today)) },
                        supportingContent = { Text(stringResource(R.string.value_ml, formatInteger(day.totalMl))) },
                        trailingContent = if (uiState.targetMl != null && day.totalMl >= uiState.targetMl) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = stringResource(R.string.water_goal_reached),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
    if (custom) {
        CustomWaterDialog(
            onSave = { input -> onAddCustom(input).also { if (it == null) custom = false } },
            onDismiss = { custom = false },
        )
    }
}

private const val QUICK_BOTTLE_ML = 500

@Composable
private fun timeText(log: WaterLog): String =
    DateTimeFormatter.ofPattern("HH:mm", currentLocale()).format(log.loggedAt.atZone(ZoneId.systemDefault()))

@Composable
private fun CustomWaterDialog(onSave: (String) -> FieldError?, onDismiss: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<FieldError?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.water_add_custom)) },
        text = {
            NumberField(
                value = input,
                onValueChange = {
                    input = it
                    error = null
                },
                label = stringResource(R.string.water_amount),
                unit = stringResource(R.string.unit_ml),
                errorText = error?.let { stringResource(it.messageRes) },
                imeAction = ImeAction.Done,
            )
        },
        confirmButton = { TextButton(onClick = { error = onSave(input) }) { Text(stringResource(R.string.action_add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
