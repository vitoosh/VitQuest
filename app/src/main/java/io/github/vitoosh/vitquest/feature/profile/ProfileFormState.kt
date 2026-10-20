package io.github.vitoosh.vitquest.feature.profile

import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.domain.FieldResult
import io.github.vitoosh.vitquest.core.domain.GoalSuggestions
import io.github.vitoosh.vitquest.core.domain.InputLimits
import io.github.vitoosh.vitquest.core.domain.NumberInput
import io.github.vitoosh.vitquest.core.domain.errorOrNull
import io.github.vitoosh.vitquest.core.model.ActivityLevel
import io.github.vitoosh.vitquest.core.model.BiologicalSex
import io.github.vitoosh.vitquest.core.model.DietaryPreference
import io.github.vitoosh.vitquest.core.model.Equipment
import io.github.vitoosh.vitquest.core.model.FitnessExperience
import io.github.vitoosh.vitquest.core.model.Goals
import io.github.vitoosh.vitquest.core.model.UserProfile
import io.github.vitoosh.vitquest.core.model.WorkoutLocation
import java.util.Locale

/** Form fields shared by onboarding and profile editing. Numbers are kept as typed text. */
data class ProfileFormState(
    val age: String = "",
    val sex: BiologicalSex? = null,
    val heightCm: String = "",
    val currentWeightKg: String = "",
    val targetWeightKg: String = "",
    val fitnessExperience: FitnessExperience? = null,
    val activityLevel: ActivityLevel? = null,
    val workoutLocation: WorkoutLocation? = null,
    val equipment: Set<Equipment> = emptySet(),
    val dietaryPreferences: Set<DietaryPreference> = emptySet(),
    val dailyStepGoal: String = GoalSuggestions.DEFAULT_STEP_GOAL.toString(),
    val dailyWaterTargetMl: String = GoalSuggestions.DEFAULT_WATER_TARGET_ML.toString(),
    val waterServingMl: String = GoalSuggestions.DEFAULT_WATER_SERVING_ML.toString(),
    /** Set once the user edits a goal so suggestions stop overwriting it. */
    val stepGoalEdited: Boolean = false,
    val waterTargetEdited: Boolean = false,
    /** Not edited in this form (set from the nutrition screen) but kept when saving. */
    val dailyCalorieTarget: Int? = null,
) {
    companion object {
        fun from(profile: UserProfile?, goals: Goals?): ProfileFormState = ProfileFormState(
            age = profile?.age?.toString().orEmpty(),
            sex = profile?.sex,
            heightCm = profile?.heightCm.toInputText(),
            currentWeightKg = profile?.currentWeightKg.toInputText(),
            targetWeightKg = goals?.targetWeightKg.toInputText(),
            fitnessExperience = profile?.fitnessExperience,
            activityLevel = profile?.activityLevel,
            workoutLocation = profile?.workoutLocation,
            equipment = profile?.equipment.orEmpty(),
            dietaryPreferences = profile?.dietaryPreferences.orEmpty(),
            dailyStepGoal = (goals?.dailyStepGoal ?: GoalSuggestions.DEFAULT_STEP_GOAL).toString(),
            dailyWaterTargetMl = (goals?.dailyWaterTargetMl ?: GoalSuggestions.DEFAULT_WATER_TARGET_ML).toString(),
            waterServingMl = (goals?.waterServingMl ?: GoalSuggestions.DEFAULT_WATER_SERVING_ML).toString(),
            stepGoalEdited = goals != null,
            waterTargetEdited = goals != null,
            dailyCalorieTarget = goals?.dailyCalorieTarget,
        )

        private fun Double?.toInputText(): String = when {
            this == null -> ""
            this % 1.0 == 0.0 -> toLong().toString()
            else -> String.format(Locale.ROOT, "%.1f", this)
        }
    }
}

/** Field-level errors; a null entry means the field is valid. */
data class ProfileFormErrors(
    val age: FieldError? = null,
    val heightCm: FieldError? = null,
    val currentWeightKg: FieldError? = null,
    val targetWeightKg: FieldError? = null,
    val dailyStepGoal: FieldError? = null,
    val dailyWaterTargetMl: FieldError? = null,
    val waterServingMl: FieldError? = null,
) {
    val hasBodyErrors: Boolean
        get() = age != null || heightCm != null || currentWeightKg != null || targetWeightKg != null

    val hasGoalErrors: Boolean
        get() = dailyStepGoal != null || dailyWaterTargetMl != null || waterServingMl != null

    val isEmpty: Boolean
        get() = !hasBodyErrors && !hasGoalErrors
}

sealed interface ProfileFormResult {
    data class Valid(val profile: UserProfile, val goals: Goals) : ProfileFormResult
    data class Invalid(val errors: ProfileFormErrors) : ProfileFormResult
}

object ProfileFormValidator {

    fun validate(form: ProfileFormState): ProfileFormResult {
        val age = NumberInput.parseInt(form.age, InputLimits.AGE_YEARS)
        val height = NumberInput.parseDecimal(form.heightCm, InputLimits.HEIGHT_CM)
        val weight = NumberInput.parseDecimal(form.currentWeightKg, InputLimits.WEIGHT_KG)
        val target = NumberInput.parseDecimal(form.targetWeightKg, InputLimits.WEIGHT_KG)
        val steps = NumberInput.parseInt(form.dailyStepGoal, InputLimits.DAILY_STEPS, required = true)
        val water = NumberInput.parseInt(form.dailyWaterTargetMl, InputLimits.DAILY_WATER_ML, required = true)
        val serving = NumberInput.parseInt(form.waterServingMl, InputLimits.WATER_SERVING_ML, required = true)

        val errors = ProfileFormErrors(
            age = age.errorOrNull,
            heightCm = height.errorOrNull,
            currentWeightKg = weight.errorOrNull,
            targetWeightKg = target.errorOrNull,
            dailyStepGoal = steps.errorOrNull,
            dailyWaterTargetMl = water.errorOrNull,
            waterServingMl = serving.errorOrNull,
        )
        if (!errors.isEmpty) return ProfileFormResult.Invalid(errors)

        val profile = UserProfile(
            age = age.valueOrNull(),
            sex = form.sex,
            heightCm = height.valueOrNull(),
            currentWeightKg = weight.valueOrNull(),
            fitnessExperience = form.fitnessExperience,
            activityLevel = form.activityLevel,
            workoutLocation = form.workoutLocation,
            equipment = form.equipment,
            dietaryPreferences = form.dietaryPreferences,
        )
        val goals = Goals(
            targetWeightKg = target.valueOrNull(),
            dailyStepGoal = requireNotNull(steps.valueOrNull()),
            dailyWaterTargetMl = requireNotNull(water.valueOrNull()),
            waterServingMl = requireNotNull(serving.valueOrNull()),
            dailyCalorieTarget = form.dailyCalorieTarget,
        )
        return ProfileFormResult.Valid(profile, goals)
    }

    /** Errors for the body-metrics fields only, used to gate the onboarding step. */
    fun validateBodyFields(form: ProfileFormState): ProfileFormErrors = ProfileFormErrors(
        age = NumberInput.parseInt(form.age, InputLimits.AGE_YEARS).errorOrNull,
        heightCm = NumberInput.parseDecimal(form.heightCm, InputLimits.HEIGHT_CM).errorOrNull,
        currentWeightKg = NumberInput.parseDecimal(form.currentWeightKg, InputLimits.WEIGHT_KG).errorOrNull,
        targetWeightKg = NumberInput.parseDecimal(form.targetWeightKg, InputLimits.WEIGHT_KG).errorOrNull,
    )

    /**
     * Refreshes goal suggestions from the current answers, leaving any goal
     * the user already edited untouched.
     */
    fun applySuggestions(form: ProfileFormState): ProfileFormState {
        val weight = NumberInput.parseDecimal(form.currentWeightKg, InputLimits.WEIGHT_KG).valueOrNull()
        return form.copy(
            dailyStepGoal = if (form.stepGoalEdited) {
                form.dailyStepGoal
            } else {
                GoalSuggestions.dailySteps(form.activityLevel).toString()
            },
            dailyWaterTargetMl = if (form.waterTargetEdited) {
                form.dailyWaterTargetMl
            } else {
                GoalSuggestions.dailyWaterMl(weight).toString()
            },
        )
    }

    private fun <T> FieldResult<T>.valueOrNull(): T? = (this as? FieldResult.Valid<T>)?.value
}
