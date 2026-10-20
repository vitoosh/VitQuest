package io.github.vitoosh.vitquest.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RecipeDao {

    @Transaction
    @Query("SELECT * FROM recipes ORDER BY name_id COLLATE NOCASE")
    abstract fun observeRecipes(): Flow<List<RecipeWithIngredients>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    abstract fun observeRecipe(id: Long): Flow<RecipeWithIngredients?>

    @Query("SELECT * FROM recipes WHERE seed_key = :seedKey")
    abstract suspend fun getBySeedKey(seedKey: String): RecipeEntity?

    @Insert
    abstract suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Update
    abstract suspend fun updateRecipe(recipe: RecipeEntity)

    @Insert
    abstract suspend fun insertIngredients(ingredients: List<RecipeIngredientEntity>)

    @Query("DELETE FROM recipe_ingredients WHERE recipe_id = :recipeId")
    abstract suspend fun deleteIngredients(recipeId: Long)

    /** Updates a built-in recipe in place (keeping its id, so meal plans stay linked) or inserts it. */
    @Transaction
    open suspend fun upsertRecipe(recipe: RecipeEntity, ingredients: List<RecipeIngredientEntity>): Long {
        val existing = recipe.seedKey?.let { getBySeedKey(it) }
        val id = if (existing != null) {
            updateRecipe(recipe.copy(id = existing.id))
            existing.id
        } else {
            insertRecipe(recipe)
        }
        deleteIngredients(id)
        insertIngredients(ingredients.mapIndexed { index, item -> item.copy(id = 0, recipeId = id, position = index) })
        return id
    }

    // ---- Meal plan ----

    @Transaction
    @Query("SELECT * FROM meal_plan_entries WHERE date BETWEEN :from AND :to ORDER BY date, created_at_epoch_ms")
    abstract fun observePlan(from: String, to: String): Flow<List<MealPlanWithRecipe>>

    @Transaction
    @Query("SELECT * FROM meal_plan_entries WHERE date BETWEEN :from AND :to")
    abstract suspend fun getPlan(from: String, to: String): List<MealPlanWithRecipe>

    @Insert
    abstract suspend fun insertPlanEntry(entry: MealPlanEntity): Long

    @Query("DELETE FROM meal_plan_entries WHERE id = :id")
    abstract suspend fun deletePlanEntry(id: Long)

    // ---- Shopping list ----

    @Query("SELECT * FROM shopping_items ORDER BY checked, created_at_epoch_ms")
    abstract fun observeShoppingItems(): Flow<List<ShoppingItemEntity>>

    @Insert
    abstract suspend fun insertShoppingItems(items: List<ShoppingItemEntity>)

    @Query("UPDATE shopping_items SET checked = :checked WHERE id = :id")
    abstract suspend fun setShoppingItemChecked(id: Long, checked: Boolean)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    abstract suspend fun deleteShoppingItem(id: Long)

    @Query("DELETE FROM shopping_items WHERE checked = 1")
    abstract suspend fun deleteCheckedShoppingItems()

    @Query("DELETE FROM shopping_items WHERE from_plan = 1")
    abstract suspend fun deletePlanShoppingItems()

    /** Regenerating from the plan replaces earlier generated items but keeps the user's own items. */
    @Transaction
    open suspend fun replacePlanShoppingItems(items: List<ShoppingItemEntity>) {
        deletePlanShoppingItems()
        insertShoppingItems(items)
    }
}
