package io.github.vitoosh.vitquest.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing

@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    supportingText: String? = null,
    errorText: String? = null,
    allowDecimal: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = unit?.let { { Text(it) } },
        supportingText = (errorText ?: supportingText)?.let { { Text(it) } },
        isError = errorText != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (allowDecimal) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = imeAction,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** A labelled group of chips where exactly one (or none) can be selected. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> SingleChoiceChips(
    title: String,
    options: List<T>,
    selected: T?,
    onSelect: (T?) -> Unit,
    optionLabel: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
            options.forEach { option ->
                val isSelected = option == selected
                FilterChip(
                    selected = isSelected,
                    // Tapping the selected chip again clears the optional answer.
                    onClick = { onSelect(if (isSelected) null else option) },
                    label = { Text(optionLabel(option)) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.semantics { role = Role.RadioButton },
                )
            }
        }
    }
}

/** A labelled group of chips where any number can be selected. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> MultiChoiceChips(
    title: String,
    options: List<T>,
    selected: Set<T>,
    onToggle: (T) -> Unit,
    optionLabel: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
            options.forEach { option ->
                val isSelected = option in selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggle(option) },
                    label = { Text(optionLabel(option)) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
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
