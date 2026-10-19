package io.github.vitoosh.vitquest.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.MultiChoiceChips
import io.github.vitoosh.vitquest.core.designsystem.component.NumberField
import io.github.vitoosh.vitquest.core.designsystem.component.SingleChoiceChips
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.domain.GoalSuggestions
import io.github.vitoosh.vitquest.core.domain.InputLimits
import io.github.vitoosh.vitquest.core.domain.NumberInput
import io.github.vitoosh.vitquest.core.domain.FieldResult
import io.github.vitoosh.vitquest.core.model.ActivityLevel
import io.github.vitoosh.vitquest.core.model.DietaryPreference
import io.github.vitoosh.vitquest.core.model.Equipment
import io.github.vitoosh.vitquest.core.model.FitnessExperience
import io.github.vitoosh.vitquest.core.model.WorkoutLocation
import io.github.vitoosh.vitquest.core.ui.descriptionRes
import io.github.vitoosh.vitquest.core.ui.formatInteger
import io.github.vitoosh.vitquest.core.ui.labelRes
import io.github.vitoosh.vitquest.core.ui.messageRes

/** Updates the form with a transformation of its current value. */
typealias FormUpdate = ((ProfileFormState) -> ProfileFormState) -> Unit

private val WATER_SERVING_OPTIONS = listOf(150, 200, 250, 330, 500)

@Composable
fun BodyMetricsSection(
    form: ProfileFormState,
    errors: ProfileFormErrors,
    onUpdate: FormUpdate,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
        NumberField(
            value = form.age,
            onValueChange = { value -> onUpdate { it.copy(age = value) } },
            label = stringResource(R.string.field_age),
            unit = stringResource(R.string.unit_years),
            errorText = errors.age?.let { errorMessage(it) },
        )
        NumberField(
            value = form.heightCm,
            onValueChange = { value -> onUpdate { it.copy(heightCm = value) } },
            label = stringResource(R.string.field_height),
            unit = stringResource(R.string.unit_cm),
            allowDecimal = true,
            errorText = errors.heightCm?.let { errorMessage(it) },
        )
        NumberField(
            value = form.currentWeightKg,
            onValueChange = { value -> onUpdate { it.copy(currentWeightKg = value) } },
            label = stringResource(R.string.field_current_weight),
            unit = stringResource(R.string.unit_kg),
            allowDecimal = true,
            errorText = errors.currentWeightKg?.let { errorMessage(it) },
        )
        NumberField(
            value = form.targetWeightKg,
            onValueChange = { value -> onUpdate { it.copy(targetWeightKg = value) } },
            label = stringResource(R.string.field_target_weight),
            unit = stringResource(R.string.unit_kg),
            allowDecimal = true,
            supportingText = stringResource(R.string.field_target_weight_help),
            errorText = errors.targetWeightKg?.let { errorMessage(it) },
            imeAction = ImeAction.Done,
        )
    }
}

@Composable
fun ExperienceSection(
    form: ProfileFormState,
    onUpdate: FormUpdate,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(VqSpacing.xl)) {
        SingleChoiceChips(
            title = stringResource(R.string.field_fitness_experience),
            options = FitnessExperience.entries,
            selected = form.fitnessExperience,
            onSelect = { value -> onUpdate { it.copy(fitnessExperience = value) } },
            optionLabel = { stringResource(it.labelRes) },
        )
        ActivityLevelPicker(
            selected = form.activityLevel,
            onSelect = { value -> onUpdate { it.copy(activityLevel = value) } },
        )
    }
}

@Composable
private fun ActivityLevelPicker(
    selected: ActivityLevel?,
    onSelect: (ActivityLevel?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(VqSpacing.xs)) {
        Text(text = stringResource(R.string.field_activity_level), style = MaterialTheme.typography.titleSmall)
        Column(modifier = Modifier.selectableGroup()) {
            ActivityLevel.entries.forEach { level ->
                val isSelected = level == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            onClick = { onSelect(if (isSelected) null else level) },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = VqSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm),
                ) {
                    RadioButton(selected = isSelected, onClick = null)
                    Column {
                        Text(text = stringResource(level.labelRes), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = stringResource(level.descriptionRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutPreferencesSection(
    form: ProfileFormState,
    onUpdate: FormUpdate,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(VqSpacing.xl)) {
        SingleChoiceChips(
            title = stringResource(R.string.field_workout_location),
            options = WorkoutLocation.entries,
            selected = form.workoutLocation,
            onSelect = { value -> onUpdate { it.copy(workoutLocation = value) } },
            optionLabel = { stringResource(it.labelRes) },
        )
        MultiChoiceChips(
            title = stringResource(R.string.field_equipment),
            supportingText = stringResource(R.string.field_equipment_help),
            options = Equipment.entries,
            selected = form.equipment,
            onToggle = { item -> onUpdate { it.copy(equipment = it.equipment.toggle(item)) } },
            optionLabel = { stringResource(it.labelRes) },
        )
        MultiChoiceChips(
            title = stringResource(R.string.field_dietary_preferences),
            supportingText = stringResource(R.string.field_dietary_preferences_help),
            options = DietaryPreference.entries,
            selected = form.dietaryPreferences,
            onToggle = { item -> onUpdate { it.copy(dietaryPreferences = it.dietaryPreferences.toggle(item)) } },
            optionLabel = { stringResource(it.labelRes) },
        )
    }
}

@Composable
fun GoalsSection(
    form: ProfileFormState,
    errors: ProfileFormErrors,
    onUpdate: FormUpdate,
    modifier: Modifier = Modifier,
) {
    val suggestedSteps = GoalSuggestions.dailySteps(form.activityLevel)
    val weight = (NumberInput.parseDecimal(form.currentWeightKg, InputLimits.WEIGHT_KG) as? FieldResult.Valid)?.value
    val suggestedWater = GoalSuggestions.dailyWaterMl(weight)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
        NumberField(
            value = form.dailyStepGoal,
            onValueChange = { value -> onUpdate { it.copy(dailyStepGoal = value, stepGoalEdited = true) } },
            label = stringResource(R.string.field_daily_step_goal),
            unit = stringResource(R.string.unit_steps),
            supportingText = stringResource(R.string.field_daily_step_goal_help, formatInteger(suggestedSteps)),
            errorText = errors.dailyStepGoal?.let { errorMessage(it) },
        )
        NumberField(
            value = form.dailyWaterTargetMl,
            onValueChange = { value -> onUpdate { it.copy(dailyWaterTargetMl = value, waterTargetEdited = true) } },
            label = stringResource(R.string.field_daily_water_target),
            unit = stringResource(R.string.unit_ml),
            supportingText = stringResource(R.string.field_daily_water_target_help, formatInteger(suggestedWater)),
            errorText = errors.dailyWaterTargetMl?.let { errorMessage(it) },
        )
        SingleChoiceChips(
            title = stringResource(R.string.field_water_serving),
            options = WATER_SERVING_OPTIONS,
            selected = form.waterServingMl.toIntOrNull()?.takeIf { it in WATER_SERVING_OPTIONS },
            onSelect = { value ->
                if (value != null) onUpdate { it.copy(waterServingMl = value.toString()) }
            },
            optionLabel = { stringResource(R.string.value_ml, formatInteger(it)) },
        )
        NumberField(
            value = form.waterServingMl,
            onValueChange = { value -> onUpdate { it.copy(waterServingMl = value) } },
            label = stringResource(R.string.field_water_serving_custom),
            unit = stringResource(R.string.unit_ml),
            errorText = errors.waterServingMl?.let { errorMessage(it) },
            imeAction = ImeAction.Done,
        )
    }
}

@Composable
private fun errorMessage(error: FieldError): String = stringResource(error.messageRes)

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item
