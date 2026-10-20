package io.github.vitoosh.vitquest.feature.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.DataSourceKind
import io.github.vitoosh.vitquest.core.designsystem.component.DataSourceTag
import io.github.vitoosh.vitquest.core.designsystem.component.NumberField
import io.github.vitoosh.vitquest.core.designsystem.component.SectionHeader
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.domain.nutrition.CalorieTarget
import io.github.vitoosh.vitquest.core.domain.nutrition.DiaryTotals
import io.github.vitoosh.vitquest.core.domain.nutrition.EnergyEstimate
import io.github.vitoosh.vitquest.core.domain.nutrition.MissingEnergyInput
import io.github.vitoosh.vitquest.core.domain.nutrition.WeightDirection
import io.github.vitoosh.vitquest.core.model.Nutrients
import io.github.vitoosh.vitquest.core.ui.dayLabel
import io.github.vitoosh.vitquest.core.ui.formatGrams
import io.github.vitoosh.vitquest.core.ui.formatInteger
import io.github.vitoosh.vitquest.core.ui.formatKcal
import io.github.vitoosh.vitquest.core.ui.messageRes
import io.github.vitoosh.vitquest.feature.nutrition.diary.EnergySummary
import java.time.LocalDate
import kotlin.math.abs

/** Previous / next day controls; the future is never selectable. */
@Composable
fun DateNavigator(
    date: LocalDate,
    today: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.action_previous_day))
        }
        Text(
            text = dayLabel(date, today),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (date != today) {
            AssistChip(onClick = onToday, label = { Text(stringResource(R.string.day_today)) })
        }
        IconButton(onClick = onNext, enabled = date.isBefore(today)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.action_next_day))
        }
    }
}

/** Eaten vs target, remaining, macros, and the informational walking estimate. */
@Composable
fun CalorieSummaryCard(
    totals: DiaryTotals,
    energy: EnergySummary,
    onOpenTarget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val target = energy.target
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader(stringResource(R.string.calories_title), modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenTarget) {
                    Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.calories_target_info))
                }
            }
            if (target != null) {
                Text(
                    text = stringResource(R.string.calories_eaten_of_target, formatInteger(totals.kcal), formatInteger(target.kcal)),
                    style = MaterialTheme.typography.headlineSmall,
                )
                LinearProgressIndicator(
                    progress = { (totals.kcal.toFloat() / target.kcal).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                val remaining = totals.remainingKcal(target.kcal)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                    Text(
                        text = if (remaining >= 0) {
                            stringResource(R.string.calories_remaining, formatInteger(remaining))
                        } else {
                            stringResource(R.string.calories_over, formatInteger(abs(remaining)))
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TargetKindTag(target)
                }
            } else {
                Text(
                    text = stringResource(R.string.calories_eaten_only, formatInteger(totals.kcal)),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.calories_no_target),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onOpenTarget) { Text(stringResource(R.string.calories_set_target)) }
            }
            HorizontalDivider()
            MacroRow(totals.total)
            energy.walkingKcal?.takeIf { it > 0 }?.let { walking ->
                Text(
                    text = stringResource(R.string.calories_walking_estimate, formatInteger(walking)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TargetKindTag(target: CalorieTarget) {
    DataSourceTag(kind = if (target is CalorieTarget.Manual) DataSourceKind.MANUAL else DataSourceKind.ESTIMATE)
}

@Composable
fun MacroRow(nutrients: Nutrients, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
        MacroItem(stringResource(R.string.macro_protein), formatGrams(nutrients.proteinG), Modifier.weight(1f))
        MacroItem(stringResource(R.string.macro_carbs), formatGrams(nutrients.carbsG), Modifier.weight(1f))
        MacroItem(stringResource(R.string.macro_fat), formatGrams(nutrients.fatG), Modifier.weight(1f))
    }
}

@Composable
private fun MacroItem(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Text(text = value, style = MaterialTheme.typography.titleSmall)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Explains BMR, TDEE and the recommendation, and lets the user set a manual target. */
@Composable
fun CalorieTargetDialog(
    energy: EnergySummary,
    onSaveManual: (String) -> FieldError?,
    onUseRecommended: () -> Unit,
    onEditProfile: () -> Unit,
    onDismiss: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf((energy.target as? CalorieTarget.Manual)?.kcal?.toString().orEmpty()) }
    var error by rememberSaveable { mutableStateOf<FieldError?>(null) }
    val estimate = energy.estimate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.calories_target_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(VqSpacing.md),
            ) {
                when (estimate) {
                    is EnergyEstimate.Available -> {
                        EnergyLine(stringResource(R.string.energy_bmr), formatKcal(estimate.bmrKcal), stringResource(R.string.energy_bmr_help))
                        EnergyLine(stringResource(R.string.energy_tdee), formatKcal(estimate.tdeeKcal), stringResource(R.string.energy_tdee_help))
                        EnergyLine(
                            stringResource(R.string.energy_recommended),
                            formatKcal(estimate.recommendedKcal),
                            stringResource(
                                when (estimate.direction) {
                                    WeightDirection.LOSE -> R.string.energy_recommended_lose
                                    WeightDirection.MAINTAIN -> R.string.energy_recommended_maintain
                                    WeightDirection.GAIN -> R.string.energy_recommended_gain
                                },
                            ),
                        )
                        if (estimate.limitedByFloor) {
                            Text(stringResource(R.string.energy_floor_note), style = MaterialTheme.typography.bodySmall)
                        }
                        if (estimate.activityAssumed) {
                            Text(stringResource(R.string.energy_activity_assumed), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    is EnergyEstimate.Missing -> {
                        Text(
                            text = stringResource(
                                R.string.energy_missing,
                                estimate.inputs.map { stringResource(it.labelRes) }.joinToString(", "),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = onEditProfile) { Text(stringResource(R.string.action_edit_profile)) }
                    }
                }
                Text(
                    text = stringResource(R.string.energy_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider()
                NumberField(
                    value = input,
                    onValueChange = {
                        input = it
                        error = null
                    },
                    label = stringResource(R.string.calories_manual_target),
                    unit = stringResource(R.string.unit_kcal),
                    errorText = error?.let { stringResource(it.messageRes) },
                    imeAction = ImeAction.Done,
                )
                if (energy.target is CalorieTarget.Manual && estimate is EnergyEstimate.Available) {
                    TextButton(onClick = onUseRecommended) {
                        Text(stringResource(R.string.calories_use_recommended, formatInteger(estimate.recommendedKcal)))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { error = onSaveManual(input) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}

@Composable
private fun EnergyLine(label: String, value: String, help: String) {
    Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Row {
            Text(text = label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(text = value, style = MaterialTheme.typography.titleSmall)
        }
        Text(text = help, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val MissingEnergyInput.labelRes: Int
    get() = when (this) {
        MissingEnergyInput.AGE -> R.string.field_age
        MissingEnergyInput.HEIGHT -> R.string.field_height
        MissingEnergyInput.WEIGHT -> R.string.field_current_weight
        MissingEnergyInput.SEX -> R.string.field_sex
    }
