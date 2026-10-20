package io.github.vitoosh.vitquest.core.domain.nutrition

import io.github.vitoosh.vitquest.core.model.FoodLogEntry
import io.github.vitoosh.vitquest.core.model.MealType
import io.github.vitoosh.vitquest.core.model.Nutrients
import kotlin.math.roundToInt

data class DiaryTotals(
    val total: Nutrients,
    val byMeal: Map<MealType, Nutrients>,
    val entryCount: Int,
) {
    val kcal: Int get() = total.kcal.roundToInt()

    fun remainingKcal(target: Int): Int = target - kcal

    companion object {
        fun of(entries: List<FoodLogEntry>): DiaryTotals {
            val byMeal = MealType.entries.associateWith { meal ->
                entries.filter { it.meal == meal }.fold(Nutrients.ZERO) { acc, e -> acc + e.nutrients }
            }
            return DiaryTotals(
                total = entries.fold(Nutrients.ZERO) { acc, e -> acc + e.nutrients },
                byMeal = byMeal,
                entryCount = entries.size,
            )
        }
    }
}

/** Nutrients for a portion of a food given its per-100 g values. */
object PortionCalculator {
    fun nutrients(per100g: Nutrients, grams: Double): Nutrients = per100g * (grams / 100.0)

    fun grams(servingGrams: Double, quantity: Double): Double = servingGrams * quantity
}
