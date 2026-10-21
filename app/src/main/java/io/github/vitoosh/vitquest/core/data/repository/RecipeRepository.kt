package io.github.vitoosh.vitquest.core.data.repository

import io.github.vitoosh.vitquest.core.data.local.MealPlanEntity
import io.github.vitoosh.vitquest.core.data.local.RecipeDao
import io.github.vitoosh.vitquest.core.data.local.ShoppingItemEntity
import io.github.vitoosh.vitquest.core.data.local.toModel
import io.github.vitoosh.vitquest.core.domain.nutrition.ShoppingListBuilder
import io.github.vitoosh.vitquest.core.model.MealPlanEntry
import io.github.vitoosh.vitquest.core.model.MealType
import io.github.vitoosh.vitquest.core.model.Recipe
import io.github.vitoosh.vitquest.core.model.ShoppingItem
import io.github.vitoosh.vitquest.core.time.TimeSource
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface RecipeRepository {
    fun recipes(): Flow<List<Recipe>>
    fun recipe(id: Long): Flow<Recipe?>

    fun plan(from: LocalDate, to: LocalDate): Flow<List<MealPlanEntry>>
    suspend fun addToPlan(date: LocalDate, meal: MealType, recipeId: Long, servings: Double)
    suspend fun removeFromPlan(entryId: Long)

    fun shoppingList(): Flow<List<ShoppingItem>>
    /** Replaces generated items with the ingredients planned for [from]..[to]. Returns the number of lines. */
    suspend fun generateShoppingList(from: LocalDate, to: LocalDate): Int
    suspend fun addShoppingItem(name: String)
    suspend fun setShoppingItemChecked(id: Long, checked: Boolean)
    suspend fun deleteShoppingItem(id: Long)
    suspend fun clearCheckedShoppingItems()
}

@Singleton
class OfflineRecipeRepository @Inject constructor(
    private val recipeDao: RecipeDao,
    private val timeSource: TimeSource,
) : RecipeRepository {

    override fun recipes(): Flow<List<Recipe>> = recipeDao.observeRecipes().map { rows -> rows.map { it.toModel() } }

    override fun recipe(id: Long): Flow<Recipe?> = recipeDao.observeRecipe(id).map { it?.toModel() }

    override fun plan(from: LocalDate, to: LocalDate): Flow<List<MealPlanEntry>> =
        recipeDao.observePlan(from.toString(), to.toString()).map { rows -> rows.map { it.toModel() } }

    override suspend fun addToPlan(date: LocalDate, meal: MealType, recipeId: Long, servings: Double) {
        recipeDao.insertPlanEntry(
            MealPlanEntity(
                date = date.toString(),
                meal = meal.name,
                recipeId = recipeId,
                servings = servings,
                createdAtEpochMs = timeSource.now().toEpochMilli(),
            ),
        )
    }

    override suspend fun removeFromPlan(entryId: Long) = recipeDao.deletePlanEntry(entryId)

    override fun shoppingList(): Flow<List<ShoppingItem>> =
        recipeDao.observeShoppingItems().map { rows -> rows.map { it.toModel() } }

    override suspend fun generateShoppingList(from: LocalDate, to: LocalDate): Int {
        val planned = recipeDao.getPlan(from.toString(), to.toString()).map { it.recipe.toModel() to it.entry.servings }
        val now = timeSource.now().toEpochMilli()
        val lines = ShoppingListBuilder.build(planned)
        recipeDao.replacePlanShoppingItems(
            lines.map {
                ShoppingItemEntity(
                    nameId = it.nameId,
                    nameEn = it.nameEn,
                    quantity = it.quantity,
                    unitId = it.unitId,
                    unitEn = it.unitEn,
                    checked = false,
                    fromPlan = true,
                    createdAtEpochMs = now,
                )
            },
        )
        return lines.size
    }

    override suspend fun addShoppingItem(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        recipeDao.insertShoppingItems(
            listOf(
                ShoppingItemEntity(
                    nameId = trimmed,
                    nameEn = trimmed,
                    quantity = null,
                    unitId = null,
                    unitEn = null,
                    checked = false,
                    fromPlan = false,
                    createdAtEpochMs = timeSource.now().toEpochMilli(),
                ),
            ),
        )
    }

    override suspend fun setShoppingItemChecked(id: Long, checked: Boolean) = recipeDao.setShoppingItemChecked(id, checked)

    override suspend fun deleteShoppingItem(id: Long) = recipeDao.deleteShoppingItem(id)

    override suspend fun clearCheckedShoppingItems() = recipeDao.deleteCheckedShoppingItems()
}
