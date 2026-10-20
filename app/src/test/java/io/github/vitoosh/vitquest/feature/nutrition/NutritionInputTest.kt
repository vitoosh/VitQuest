package io.github.vitoosh.vitquest.feature.nutrition

import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.domain.FieldResult
import io.github.vitoosh.vitquest.core.model.FoodServing
import io.github.vitoosh.vitquest.core.model.Nutrients
import io.github.vitoosh.vitquest.core.model.Recipe
import io.github.vitoosh.vitquest.core.model.RecipeIngredient
import io.github.vitoosh.vitquest.core.model.RecipeTag
import io.github.vitoosh.vitquest.feature.nutrition.food.AddFoodViewModel
import io.github.vitoosh.vitquest.feature.nutrition.food.CustomFoodForm
import io.github.vitoosh.vitquest.feature.nutrition.recipes.RecipesViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionInputTest {

    @Test
    fun portionUsesServingTimesQuantityOrPlainGrams() {
        val plate = FoodServing("1 piring", "1 plate", 150.0)
        val byServing = AddFoodViewModel.parsePortion(plate, "1,5") as FieldResult.Valid
        assertEquals(225.0, byServing.value!!.grams, 0.001)

        val byGrams = AddFoodViewModel.parsePortion(null, "80") as FieldResult.Valid
        assertEquals(80.0, byGrams.value!!.grams, 0.001)

        assertEquals(FieldResult.Invalid(FieldError.OUT_OF_RANGE), AddFoodViewModel.parsePortion(plate, "50"))
        assertEquals(FieldResult.Invalid(FieldError.REQUIRED), AddFoodViewModel.parsePortion(null, ""))
    }

    @Test
    fun customFoodPerServingIsConvertedToPer100g() {
        val (input, errors) = AddFoodViewModel.parseCustomFood(
            CustomFoodForm(name = "Roti isi", perServing = true, servingLabel = "1 buah", servingGrams = "80", kcal = "200", protein = "8"),
        )
        assertTrue(errors.isEmpty)
        assertEquals(250.0, input!!.per100g.kcal, 0.001)
        assertEquals(10.0, input.per100g.proteinG!!, 0.001)
        assertNull(input.per100g.fatG)
        assertEquals(80.0, input.servingGrams!!, 0.001)
    }

    @Test
    fun customFoodValidation() {
        val (input, errors) = AddFoodViewModel.parseCustomFood(CustomFoodForm(perServing = true, servingGrams = "", kcal = "abc"))
        assertNull(input)
        assertTrue(errors.name)
        assertEquals(FieldError.REQUIRED, errors.servingGrams)
        assertEquals(FieldError.INVALID_NUMBER, errors.kcal)

        val (per100, okErrors) = AddFoodViewModel.parseCustomFood(CustomFoodForm(name = "Sup", perServing = false, kcal = "45"))
        assertTrue(okErrors.isEmpty)
        assertNull(per100!!.servingGrams)
        assertEquals(45.0, per100.per100g.kcal, 0.001)
    }

    @Test
    fun recipeFilterMatchesNamesIngredientsAndTags() {
        fun recipe(id: Long, name: String, ingredient: String, tags: Set<RecipeTag>) = Recipe(
            id, name, name, "", "", 2, 5, 5, tags,
            listOf(RecipeIngredient(ingredient, ingredient, 1.0, "g", "g", 100.0, null, null, Nutrients.ZERO)),
            emptyList(), emptyList(),
        )
        val list = listOf(
            recipe(1, "Tumis Tempe", "Tempe", setOf(RecipeTag.VEGETARIAN)),
            recipe(2, "Pepes Ikan", "Ikan nila", setOf(RecipeTag.HIGH_PROTEIN)),
        )
        assertEquals(listOf(2L), RecipesViewModel.filter(list, "nila", null).map { it.id })
        assertEquals(listOf(1L), RecipesViewModel.filter(list, "", RecipeTag.VEGETARIAN).map { it.id })
        assertEquals(2, RecipesViewModel.filter(list, "  ", null).size)
    }
}
