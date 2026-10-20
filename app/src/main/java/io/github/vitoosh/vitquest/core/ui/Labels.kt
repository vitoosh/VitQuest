package io.github.vitoosh.vitquest.core.ui

import androidx.annotation.StringRes
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.locale.AppLanguage
import io.github.vitoosh.vitquest.core.model.ActivityLevel
import io.github.vitoosh.vitquest.core.model.BiologicalSex
import io.github.vitoosh.vitquest.core.model.MealType
import io.github.vitoosh.vitquest.core.model.RecipeTag
import io.github.vitoosh.vitquest.core.model.DietaryPreference
import io.github.vitoosh.vitquest.core.model.Equipment
import io.github.vitoosh.vitquest.core.model.FitnessExperience
import io.github.vitoosh.vitquest.core.model.ThemeMode
import io.github.vitoosh.vitquest.core.model.WorkoutLocation

// Maps domain values to localised labels so no user-facing text lives in Kotlin.

@get:StringRes
val FitnessExperience.labelRes: Int
    get() = when (this) {
        FitnessExperience.BEGINNER -> R.string.experience_beginner
        FitnessExperience.INTERMEDIATE -> R.string.experience_intermediate
        FitnessExperience.ADVANCED -> R.string.experience_advanced
    }

@get:StringRes
val ActivityLevel.labelRes: Int
    get() = when (this) {
        ActivityLevel.SEDENTARY -> R.string.activity_sedentary
        ActivityLevel.LIGHT -> R.string.activity_light
        ActivityLevel.MODERATE -> R.string.activity_moderate
        ActivityLevel.ACTIVE -> R.string.activity_active
    }

@get:StringRes
val ActivityLevel.descriptionRes: Int
    get() = when (this) {
        ActivityLevel.SEDENTARY -> R.string.activity_sedentary_description
        ActivityLevel.LIGHT -> R.string.activity_light_description
        ActivityLevel.MODERATE -> R.string.activity_moderate_description
        ActivityLevel.ACTIVE -> R.string.activity_active_description
    }

@get:StringRes
val WorkoutLocation.labelRes: Int
    get() = when (this) {
        WorkoutLocation.HOME -> R.string.location_home
        WorkoutLocation.OFFICE -> R.string.location_office
        WorkoutLocation.OUTDOOR -> R.string.location_outdoor
        WorkoutLocation.MIXED -> R.string.location_mixed
    }

@get:StringRes
val Equipment.labelRes: Int
    get() = when (this) {
        Equipment.RESISTANCE_BAND -> R.string.equipment_resistance_band
        Equipment.DUMBBELLS -> R.string.equipment_dumbbells
        Equipment.PULL_UP_BAR -> R.string.equipment_pull_up_bar
        Equipment.JUMP_ROPE -> R.string.equipment_jump_rope
        Equipment.EXERCISE_MAT -> R.string.equipment_exercise_mat
        Equipment.CHAIR_OR_BENCH -> R.string.equipment_chair_or_bench
    }

@get:StringRes
val DietaryPreference.labelRes: Int
    get() = when (this) {
        DietaryPreference.HALAL -> R.string.diet_halal
        DietaryPreference.VEGETARIAN -> R.string.diet_vegetarian
        DietaryPreference.VEGAN -> R.string.diet_vegan
        DietaryPreference.LACTOSE_FREE -> R.string.diet_lactose_free
        DietaryPreference.GLUTEN_FREE -> R.string.diet_gluten_free
        DietaryPreference.LOW_SUGAR -> R.string.diet_low_sugar
    }

@get:StringRes
val BiologicalSex.labelRes: Int
    get() = when (this) {
        BiologicalSex.MALE -> R.string.sex_male
        BiologicalSex.FEMALE -> R.string.sex_female
    }

@get:StringRes
val MealType.labelRes: Int
    get() = when (this) {
        MealType.BREAKFAST -> R.string.meal_breakfast
        MealType.LUNCH -> R.string.meal_lunch
        MealType.DINNER -> R.string.meal_dinner
        MealType.SNACK -> R.string.meal_snack
    }

@get:StringRes
val RecipeTag.labelRes: Int
    get() = when (this) {
        RecipeTag.BREAKFAST -> R.string.recipe_tag_breakfast
        RecipeTag.MAIN -> R.string.recipe_tag_main
        RecipeTag.SIDE -> R.string.recipe_tag_side
        RecipeTag.SNACK -> R.string.recipe_tag_snack
        RecipeTag.HIGH_PROTEIN -> R.string.recipe_tag_high_protein
        RecipeTag.VEGETARIAN -> R.string.recipe_tag_vegetarian
        RecipeTag.QUICK -> R.string.recipe_tag_quick
        RecipeTag.BUDGET -> R.string.recipe_tag_budget
    }

@get:StringRes
val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }

@get:StringRes
val AppLanguage.labelRes: Int
    get() = when (this) {
        AppLanguage.SYSTEM -> R.string.language_system
        AppLanguage.INDONESIAN -> R.string.language_indonesian
        AppLanguage.ENGLISH -> R.string.language_english
    }

@get:StringRes
val FieldError.messageRes: Int
    get() = when (this) {
        FieldError.REQUIRED -> R.string.error_required
        FieldError.INVALID_NUMBER -> R.string.error_invalid_number
        FieldError.OUT_OF_RANGE -> R.string.error_out_of_range
    }
