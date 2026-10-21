package io.github.vitoosh.vitquest.core.model

import java.time.Instant
import java.time.LocalDate

/** Used only for the BMR formula, which differs by biological sex. Optional. */
enum class BiologicalSex { MALE, FEMALE }

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }

enum class FoodCategory { STAPLE, PROTEIN, VEGETABLE, FRUIT, DISH, SNACK, DRINK, OTHER }

/** Energy and macronutrients. Macros are null when unknown (e.g. a custom food without them). */
data class Nutrients(
    val kcal: Double,
    val proteinG: Double? = null,
    val carbsG: Double? = null,
    val fatG: Double? = null,
) {
    operator fun plus(other: Nutrients) = Nutrients(
        kcal = kcal + other.kcal,
        proteinG = sumOrNull(proteinG, other.proteinG),
        carbsG = sumOrNull(carbsG, other.carbsG),
        fatG = sumOrNull(fatG, other.fatG),
    )

    operator fun times(factor: Double) = Nutrients(
        kcal = kcal * factor,
        proteinG = proteinG?.times(factor),
        carbsG = carbsG?.times(factor),
        fatG = fatG?.times(factor),
    )

    companion object {
        val ZERO = Nutrients(0.0, 0.0, 0.0, 0.0)

        // A total is only "known" if every part is known; otherwise it would understate the value.
        private fun sumOrNull(a: Double?, b: Double?): Double? = if (a == null || b == null) null else a + b
    }
}

data class FoodServing(val labelId: String, val labelEn: String, val grams: Double)

data class Food(
    val id: Long,
    val nameId: String,
    val nameEn: String,
    val category: FoodCategory,
    val per100g: Nutrients,
    val servings: List<FoodServing>,
    val isCustom: Boolean,
    val isFavorite: Boolean,
    /** Where the reference values come from; null for custom foods. */
    val source: String?,
)

/**
 * A logged food. Name and nutrients are copied at logging time so history
 * stays correct if the food is edited or deleted later.
 */
data class FoodLogEntry(
    val id: Long,
    val date: LocalDate,
    val meal: MealType,
    val foodId: Long?,
    val nameId: String,
    val nameEn: String,
    val grams: Double,
    val quantity: Double,
    val servingLabelId: String?,
    val servingLabelEn: String?,
    val nutrients: Nutrients,
    val createdAt: Instant,
    /** The food photo this entry was estimated from, if any. */
    val photoId: Long? = null,
)

data class WeightEntry(val id: Long, val date: LocalDate, val weightKg: Double, val note: String?)

data class BodyMeasurement(
    val id: Long,
    val date: LocalDate,
    val waistCm: Double?,
    val hipCm: Double?,
    val chestCm: Double?,
    val armCm: Double?,
    val thighCm: Double?,
)

data class WaterLog(val id: Long, val date: LocalDate, val amountMl: Int, val loggedAt: Instant)

enum class RecipeTag { BREAKFAST, MAIN, SIDE, SNACK, HIGH_PROTEIN, VEGETARIAN, QUICK, BUDGET }

data class RecipeIngredient(
    val nameId: String,
    val nameEn: String,
    val quantity: Double,
    val unitId: String,
    val unitEn: String,
    val grams: Double,
    val substituteId: String?,
    val substituteEn: String?,
    /** Nutrients for the full amount in the base recipe. */
    val nutrients: Nutrients,
)

data class Recipe(
    val id: Long,
    val nameId: String,
    val nameEn: String,
    val descriptionId: String,
    val descriptionEn: String,
    val servings: Int,
    val prepMinutes: Int,
    val cookMinutes: Int,
    val tags: Set<RecipeTag>,
    val ingredients: List<RecipeIngredient>,
    val stepsId: List<String>,
    val stepsEn: List<String>,
) {
    val totalMinutes: Int get() = prepMinutes + cookMinutes

    val nutrientsPerServing: Nutrients
        get() = ingredients.fold(Nutrients.ZERO) { acc, item -> acc + item.nutrients } * (1.0 / servings)
}

data class MealPlanEntry(
    val id: Long,
    val date: LocalDate,
    val meal: MealType,
    val recipeId: Long,
    val recipeNameId: String,
    val recipeNameEn: String,
    val servings: Double,
    val nutrientsPerServing: Nutrients,
)

data class ShoppingItem(
    val id: Long,
    val nameId: String,
    val nameEn: String,
    val quantity: Double?,
    val unitId: String?,
    val unitEn: String?,
    val checked: Boolean,
    /** True for items generated from the meal plan; false for items the user added. */
    val fromPlan: Boolean,
)
