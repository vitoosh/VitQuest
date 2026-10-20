package io.github.vitoosh.vitquest.core.data.seed

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Validates the bundled food and recipe database that ships in the APK. */
class SeedDataTest {

    private val foods = SeedImporter.parseFoods(File("src/main/assets/seed/foods.json").readText())
    private val recipes = SeedImporter.parseRecipes(File("src/main/assets/seed/recipes.json").readText())

    @Test
    fun keysAreUniqueAndEveryFoodHasServings() {
        assertEquals(foods.foods.size, foods.foods.map { it.key }.toSet().size)
        assertEquals(recipes.recipes.size, recipes.recipes.map { it.key }.toSet().size)
        foods.foods.forEach { food ->
            assertTrue(food.key, food.servings.isNotEmpty() && food.servings.all { it.grams > 0 })
            assertTrue(food.key, food.kcal in 0.0..900.0)
            assertTrue(food.key, food.source in setOf("TKPI", "USDA", "ESTIMATE"))
        }
    }

    @Test
    fun includesTheRequiredIndonesianStarterFoods() {
        val keys = foods.foods.map { it.key }.toSet()
        listOf("nasi_putih", "telur_rebus", "ayam_panggang", "tempe", "tahu", "sayur_sop", "gado_gado").forEach {
            assertTrue("Missing $it", it in keys)
        }
        assertTrue(foods.foods.count { it.category == "FRUIT" } >= 5)
        assertTrue(foods.foods.count { it.category == "VEGETABLE" } >= 5)
    }

    @Test
    fun energyMatchesMacrosForFoodsWithLittleFibre() {
        foods.foods
            .filter { it.kcal > 50 && it.category !in setOf("VEGETABLE", "FRUIT") }
            .forEach { food ->
                val atwater = 4 * food.protein!! + 4 * food.carbs!! + 9 * food.fat!!
                assertTrue("${food.key}: ${food.kcal} vs $atwater", kotlin.math.abs(atwater - food.kcal) / food.kcal < 0.2)
            }
    }

    @Test
    fun recipesImportWithPlausibleNutrition() {
        val rows = SeedImporter.recipeRows(recipes, foods)
        assertTrue(rows.size >= 10)
        rows.forEach { (recipe, ingredients) ->
            val perServing = ingredients.sumOf { it.kcal } / recipe.servings
            assertTrue("${recipe.seedKey}: $perServing kcal", perServing in 30.0..900.0)
            assertTrue(recipe.stepsId.isNotBlank() && recipe.stepsEn.isNotBlank())
            assertEquals(recipe.stepsId.lines().size, recipe.stepsEn.lines().size)
        }
    }

    @Test
    fun foodRowsKeepSeedKeysForSafeUpdates() {
        val rows = SeedImporter.foodRows(foods, nowEpochMs = 0)
        assertEquals(foods.foods.size, rows.size)
        assertTrue(rows.all { (food, _) -> food.seedKey != null && !food.isCustom })
    }
}
