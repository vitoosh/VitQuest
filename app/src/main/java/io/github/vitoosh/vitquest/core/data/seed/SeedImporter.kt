package io.github.vitoosh.vitquest.core.data.seed

import io.github.vitoosh.vitquest.core.data.local.FoodEntity
import io.github.vitoosh.vitquest.core.data.local.FoodServingEntity
import io.github.vitoosh.vitquest.core.data.local.RecipeEntity
import io.github.vitoosh.vitquest.core.data.local.RecipeIngredientEntity
import io.github.vitoosh.vitquest.core.model.FoodCategory
import io.github.vitoosh.vitquest.core.model.RecipeTag
import kotlinx.serialization.json.Json

/** Converts seed JSON into database rows. Pure, so the bundled data can be validated in unit tests. */
object SeedImporter {

    private val json = Json { ignoreUnknownKeys = true }

    fun parseFoods(text: String): FoodSeedFile = json.decodeFromString(FoodSeedFile.serializer(), text)

    fun parseRecipes(text: String): RecipeSeedFile = json.decodeFromString(RecipeSeedFile.serializer(), text)

    fun foodRows(file: FoodSeedFile, nowEpochMs: Long): List<Pair<FoodEntity, List<FoodServingEntity>>> =
        file.foods.map { seed ->
            require(FoodCategory.entries.any { it.name == seed.category }) { "Unknown category ${seed.category} for ${seed.key}" }
            require(seed.servings.isNotEmpty()) { "Food ${seed.key} needs at least one serving" }
            FoodEntity(
                seedKey = seed.key,
                nameId = seed.nameId,
                nameEn = seed.nameEn,
                category = seed.category,
                kcalPer100g = seed.kcal,
                proteinPer100g = seed.protein,
                carbsPer100g = seed.carbs,
                fatPer100g = seed.fat,
                isCustom = false,
                isFavorite = false,
                source = seed.source,
                createdAtEpochMs = nowEpochMs,
            ) to seed.servings.mapIndexed { index, serving ->
                FoodServingEntity(foodId = 0, position = index, labelId = serving.labelId, labelEn = serving.labelEn, grams = serving.grams)
            }
        }

    /** Ingredient nutrients are derived from the referenced foods, so recipes and foods stay consistent. */
    fun recipeRows(file: RecipeSeedFile, foods: FoodSeedFile): List<Pair<RecipeEntity, List<RecipeIngredientEntity>>> {
        val byKey = foods.foods.associateBy { it.key }
        return file.recipes.map { seed ->
            require(seed.servings > 0) { "Recipe ${seed.key} needs servings" }
            seed.tags.forEach { tag -> require(RecipeTag.entries.any { it.name == tag }) { "Unknown tag $tag in ${seed.key}" } }
            RecipeEntity(
                seedKey = seed.key,
                nameId = seed.nameId,
                nameEn = seed.nameEn,
                descriptionId = seed.descriptionId,
                descriptionEn = seed.descriptionEn,
                servings = seed.servings,
                prepMinutes = seed.prep,
                cookMinutes = seed.cook,
                tags = seed.tags.joinToString(","),
                stepsId = seed.stepsId.joinToString("\n"),
                stepsEn = seed.stepsEn.joinToString("\n"),
            ) to seed.ingredients.mapIndexed { index, item ->
                val food = requireNotNull(byKey[item.food]) { "Recipe ${seed.key} uses unknown food ${item.food}" }
                val factor = item.grams / 100.0
                RecipeIngredientEntity(
                    recipeId = 0,
                    position = index,
                    nameId = item.nameId,
                    nameEn = item.nameEn,
                    quantity = item.quantity,
                    unitId = item.unitId,
                    unitEn = item.unitEn,
                    grams = item.grams,
                    substituteId = item.substituteId,
                    substituteEn = item.substituteEn,
                    kcal = food.kcal * factor,
                    proteinG = food.protein?.times(factor),
                    carbsG = food.carbs?.times(factor),
                    fatG = food.fat?.times(factor),
                )
            }
        }
    }
}
