package io.github.vitoosh.vitquest.feature.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.NumberField
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.model.DailySteps
import io.github.vitoosh.vitquest.core.model.StepSource
import io.github.vitoosh.vitquest.core.model.StepSourcePreference
import io.github.vitoosh.vitquest.core.ui.dayLabel
import io.github.vitoosh.vitquest.core.ui.formatInteger
import io.github.vitoosh.vitquest.core.ui.messageRes
import java.time.LocalDate

@Composable
fun ManualStepsDialog(
    date: LocalDate,
    today: LocalDate,
    current: DailySteps?,
    onSave: (String) -> FieldError?,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var input by rememberSaveable(date) {
        mutableStateOf(current?.takeIf { it.source == StepSource.MANUAL }?.steps?.toString().orEmpty())
    }
    var error by rememberSaveable(date) { mutableStateOf<FieldError?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manual_steps_title, dayLabel(date, today))) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
                if (current != null && current.source != StepSource.MANUAL) {
                    Text(
                        text = stringResource(
                            R.string.manual_steps_current_auto,
                            formatInteger(current.steps),
                            stringResource(current.source.labelRes),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = stringResource(R.string.manual_steps_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NumberField(
                    value = input,
                    onValueChange = {
                        input = it
                        error = null
                    },
                    label = stringResource(R.string.manual_steps_field),
                    unit = stringResource(R.string.unit_steps),
                    errorText = error?.let { stringResource(it.messageRes) },
                    imeAction = ImeAction.Done,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { error = onSave(input) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (current?.source == StepSource.MANUAL) {
                    TextButton(onClick = onClear) {
                        Text(stringResource(R.string.manual_steps_remove), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}

@Composable
fun StepGoalDialog(
    currentGoal: Int?,
    onSave: (String) -> FieldError?,
    onDismiss: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf(currentGoal?.toString().orEmpty()) }
    var error by rememberSaveable { mutableStateOf<FieldError?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.field_daily_step_goal)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
                Text(
                    text = stringResource(R.string.step_goal_dialog_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NumberField(
                    value = input,
                    onValueChange = {
                        input = it
                        error = null
                    },
                    label = stringResource(R.string.field_daily_step_goal),
                    unit = stringResource(R.string.unit_steps),
                    errorText = error?.let { stringResource(it.messageRes) },
                    imeAction = ImeAction.Done,
                )
            }
        },
        confirmButton = { TextButton(onClick = { error = onSave(input) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun SourcePreferenceDialog(
    selected: StepSourcePreference,
    onSelect: (StepSourcePreference) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.steps_preference_title)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                StepSourcePreference.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = option == selected, onClick = { onSelect(option) }, role = Role.RadioButton)
                            .padding(vertical = VqSpacing.sm),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm),
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Column {
                            Text(stringResource(option.labelRes), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = stringResource(option.descriptionRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}

/** Explains why a permission is needed before Android shows its own prompt. */
@Composable
fun PermissionRationaleDialog(
    title: String,
    message: String,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onContinue) { Text(stringResource(R.string.action_continue)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_not_now)) } },
    )
}
