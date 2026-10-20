package io.github.vitoosh.vitquest.core.domain.nutrition

import io.github.vitoosh.vitquest.core.model.Recipe

data class ShoppingLine(
    val nameId: String,
    val nameEn: String,
    val quantity: Double,
    val unitId: String,
    val unitEn: String,
)

/** Combines the ingredients of planned recipes, scaled to the planned servings. */
object ShoppingListBuilder {

    fun build(planned: List<Pair<Recipe, Double>>): List<ShoppingLine> {
        val lines = linkedMapOf<Pair<String, String>, ShoppingLine>()
        planned.forEach { (recipe, servings) ->
            val factor = servings / recipe.servings
            recipe.ingredients.forEach { item ->
                // "Bawang putih, cincang" and "Bawang putih, geprek" are the same thing to buy.
                val nameId = item.nameId.baseName()
                val nameEn = item.nameEn.baseName()
                val key = nameId.lowercase() to item.unitId
                val previous = lines[key]
                lines[key] = ShoppingLine(
                    nameId = previous?.nameId ?: nameId,
                    nameEn = previous?.nameEn ?: nameEn,
                    quantity = (previous?.quantity ?: 0.0) + item.quantity * factor,
                    unitId = item.unitId,
                    unitEn = item.unitEn,
                )
            }
        }
        return lines.values.toList()
    }

    private fun String.baseName(): String = substringBefore(",").substringBefore("(").trim()
}
