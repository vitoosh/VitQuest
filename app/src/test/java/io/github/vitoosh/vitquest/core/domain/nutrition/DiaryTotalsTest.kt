package io.github.vitoosh.vitquest.core.domain.nutrition

import io.github.vitoosh.vitquest.core.model.FoodLogEntry
import io.github.vitoosh.vitquest.core.model.MealType
import io.github.vitoosh.vitquest.core.model.Nutrients
import io.github.vitoosh.vitquest.core.model.Recipe
import io.github.vitoosh.vitquest.core.model.RecipeIngredient
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiaryTotalsTest {

    private fun entry(meal: MealType, nutrients: Nutrients) = FoodLogEntry(
        id = 0, date = LocalDate.parse("2026-10-19"), meal = meal, foodId = null, nameId = "x", nameEn = "x",
        grams = 100.0, quantity = 1.0, servingLabelId = null, servingLabelEn = null, nutrients = nutrients, createdAt = Instant.EPOCH,
    )

    @Test
    fun totalsAddUpPerMealAndDay() {
        val totals = DiaryTotals.of(
            listOf(
                entry(MealType.BREAKFAST, Nutrients(300.0, 10.0, 40.0, 10.0)),
                entry(MealType.BREAKFAST, Nutrients(85.4, 7.0, 0.5, 5.0)),
                entry(MealType.LUNCH, Nutrients(500.0, 30.0, 50.0, 15.0)),
            ),
        )
        assertEquals(885, totals.kcal)
        assertEquals(385.4, totals.byMeal.getValue(MealType.BREAKFAST).kcal, 0.001)
        assertEquals(0.0, totals.byMeal.getValue(MealType.DINNER).kcal, 0.001)
        assertEquals(47.0, totals.total.proteinG!!, 0.001)
        assertEquals(1_115, totals.remainingKcal(2_000))
        assertEquals(-385, totals.remainingKcal(500))
    }

    @Test
    fun unknownMacrosMakeTheTotalUnknownInsteadOfTooLow() {
        val totals = DiaryTotals.of(
            listOf(entry(MealType.SNACK, Nutrients(100.0, 5.0, 10.0, 2.0)), entry(MealType.SNACK, Nutrients(200.0))),
        )
        assertEquals(300, totals.kcal)
        assertNull(totals.total.proteinG)
    }

    @Test
    fun portionsScaleWithWeight() {
        val per100 = Nutrients(180.0, 3.0, 39.8, 0.3)
        val portion = PortionCalculator.nutrients(per100, 150.0)
        assertEquals(270.0, portion.kcal, 0.001)
        assertEquals(59.7, portion.carbsG!!, 0.001)
        assertEquals(150.0, PortionCalculator.grams(100.0, 1.5), 0.001)
    }

    @Test
    fun shoppingListMergesAndScalesIngredients() {
        fun item(name: String, qty: Double, unit: String) = RecipeIngredient(name, name, qty, unit, unit, 10.0, null, null, Nutrients.ZERO)
        val soup = Recipe(1, "Sop", "Soup", "", "", 4, 0, 0, emptySet(), listOf(item("Bawang putih, geprek", 3.0, "siung"), item("Wortel", 2.0, "buah")), emptyList(), emptyList())
        val stirFry = Recipe(2, "Tumis", "Stir-fry", "", "", 2, 0, 0, emptySet(), listOf(item("Bawang putih, cincang", 2.0, "siung")), emptyList(), emptyList())

        val lines = ShoppingListBuilder.build(listOf(soup to 2.0, stirFry to 2.0))

        // Half the soup (2 of 4 servings) plus the full stir-fry.
        val garlic = lines.single { it.nameId == "Bawang putih" }
        assertEquals(3.5, garlic.quantity, 0.001)
        assertEquals(1.0, lines.single { it.nameId == "Wortel" }.quantity, 0.001)
    }
}
