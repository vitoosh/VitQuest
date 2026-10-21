package io.github.vitoosh.vitquest.core.data.local

import io.github.vitoosh.vitquest.core.model.BodyMeasurement
import io.github.vitoosh.vitquest.core.model.Food
import io.github.vitoosh.vitquest.core.model.FoodCategory
import io.github.vitoosh.vitquest.core.model.FoodLogEntry
import io.github.vitoosh.vitquest.core.model.FoodServing
import io.github.vitoosh.vitquest.core.model.MealPlanEntry
import io.github.vitoosh.vitquest.core.model.MealType
import io.github.vitoosh.vitquest.core.model.Nutrients
import io.github.vitoosh.vitquest.core.model.Recipe
import io.github.vitoosh.vitquest.core.model.RecipeIngredient
import io.github.vitoosh.vitquest.core.model.RecipeTag
import io.github.vitoosh.vitquest.core.model.ShoppingItem
import io.github.vitoosh.vitquest.core.model.WaterLog
import io.github.vitoosh.vitquest.core.model.WeightEntry
import java.time.Instant
import java.time.LocalDate

fun FoodWithServings.toModel(): Food = Food(
    id = food.id,
    nameId = food.nameId,
    nameEn = food.nameEn,
    category = food.category.toEnumOrNull<FoodCategory>() ?: FoodCategory.OTHER,
    per100g = Nutrients(food.kcalPer100g, food.proteinPer100g, food.carbsPer100g, food.fatPer100g),
    servings = servings.sortedBy { it.position }.map { FoodServing(it.labelId, it.labelEn, it.grams) },
    isCustom = food.isCustom,
    isFavorite = food.isFavorite,
    source = food.source,
)

fun FoodLogEntity.toModel(): FoodLogEntry = FoodLogEntry(
    id = id,
    date = LocalDate.parse(date),
    meal = meal.toEnumOrNull<MealType>() ?: MealType.SNACK,
    foodId = foodId,
    nameId = nameId,
    nameEn = nameEn,
    grams = grams,
    quantity = quantity,
    servingLabelId = servingLabelId,
    servingLabelEn = servingLabelEn,
    nutrients = Nutrients(kcal, proteinG, carbsG, fatG),
    createdAt = Instant.ofEpochMilli(createdAtEpochMs),
    photoId = photoId,
)

fun RecipeWithIngredients.toModel(): Recipe = Recipe(
    id = recipe.id,
    nameId = recipe.nameId,
    nameEn = recipe.nameEn,
    descriptionId = recipe.descriptionId,
    descriptionEn = recipe.descriptionEn,
    servings = recipe.servings,
    prepMinutes = recipe.prepMinutes,
    cookMinutes = recipe.cookMinutes,
    tags = recipe.tags.toEnumSet<RecipeTag>(),
    ingredients = ingredients.sortedBy { it.position }.map {
        RecipeIngredient(
            nameId = it.nameId,
            nameEn = it.nameEn,
            quantity = it.quantity,
            unitId = it.unitId,
            unitEn = it.unitEn,
            grams = it.grams,
            substituteId = it.substituteId,
            substituteEn = it.substituteEn,
            nutrients = Nutrients(it.kcal, it.proteinG, it.carbsG, it.fatG),
        )
    },
    stepsId = recipe.stepsId.split("\n").filter { it.isNotBlank() },
    stepsEn = recipe.stepsEn.split("\n").filter { it.isNotBlank() },
)

fun MealPlanWithRecipe.toModel(): MealPlanEntry {
    val model = recipe.toModel()
    return MealPlanEntry(
        id = entry.id,
        date = LocalDate.parse(entry.date),
        meal = entry.meal.toEnumOrNull<MealType>() ?: MealType.LUNCH,
        recipeId = model.id,
        recipeNameId = model.nameId,
        recipeNameEn = model.nameEn,
        servings = entry.servings,
        nutrientsPerServing = model.nutrientsPerServing,
    )
}

fun ShoppingItemEntity.toModel() = ShoppingItem(id, nameId, nameEn, quantity, unitId, unitEn, checked, fromPlan)

fun WeightEntryEntity.toModel() = WeightEntry(id, LocalDate.parse(date), weightKg, note)

fun BodyMeasurementEntity.toModel() = BodyMeasurement(id, LocalDate.parse(date), waistCm, hipCm, chestCm, armCm, thighCm)

fun WaterLogEntity.toModel() = WaterLog(id, LocalDate.parse(date), amountMl, Instant.ofEpochMilli(loggedAtEpochMs))
