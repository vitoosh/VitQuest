package io.github.vitoosh.vitquest.core.data.repository

import io.github.vitoosh.vitquest.core.data.local.FoodDao
import io.github.vitoosh.vitquest.core.data.local.FoodEntity
import io.github.vitoosh.vitquest.core.data.local.FoodLogEntity
import io.github.vitoosh.vitquest.core.data.local.FoodServingEntity
import io.github.vitoosh.vitquest.core.data.local.toModel
import io.github.vitoosh.vitquest.core.domain.nutrition.PortionCalculator
import io.github.vitoosh.vitquest.core.model.Food
import io.github.vitoosh.vitquest.core.model.FoodCategory
import io.github.vitoosh.vitquest.core.model.FoodLogEntry
import io.github.vitoosh.vitquest.core.model.FoodServing
import io.github.vitoosh.vitquest.core.model.MealType
import io.github.vitoosh.vitquest.core.model.Nutrients
import io.github.vitoosh.vitquest.core.model.Recipe
import io.github.vitoosh.vitquest.core.time.TimeSource
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** A portion chosen in the UI: a serving (or plain grams) times a quantity. */
data class Portion(val serving: FoodServing?, val quantity: Double, val grams: Double)

data class CustomFoodInput(
    val name: String,
    val per100g: Nutrients,
    val servingLabel: String?,
    val servingGrams: Double?,
)

interface NutritionRepository {
    fun searchFoods(query: String): Flow<List<Food>>
    fun favoriteFoods(): Flow<List<Food>>
    fun recentFoods(): Flow<List<Food>>
    suspend fun getFood(id: Long): Food?
    suspend fun setFavorite(foodId: Long, favorite: Boolean)
    suspend fun saveCustomFood(input: CustomFoodInput): Long
    suspend fun deleteCustomFood(foodId: Long)

    fun diary(date: LocalDate): Flow<List<FoodLogEntry>>
    fun diaryRange(from: LocalDate, to: LocalDate): Flow<List<FoodLogEntry>>
    suspend fun logFood(date: LocalDate, meal: MealType, food: Food, portion: Portion): Long
    suspend fun logRecipe(date: LocalDate, meal: MealType, recipe: Recipe, servings: Double): Long
    /** Changes the amount of a logged item; nutrients scale with the new weight. */
    suspend fun updateAmount(entryId: Long, quantity: Double, grams: Double)
    suspend fun deleteEntry(entryId: Long)
}

@Singleton
class OfflineNutritionRepository @Inject constructor(
    private val foodDao: FoodDao,
    private val timeSource: TimeSource,
) : NutritionRepository {

    override fun searchFoods(query: String): Flow<List<Food>> =
        foodDao.searchFoods(query.trim(), SEARCH_LIMIT).map { rows -> rows.map { it.toModel() } }

    override fun favoriteFoods(): Flow<List<Food>> = foodDao.observeFavorites().map { rows -> rows.map { it.toModel() } }

    override fun recentFoods(): Flow<List<Food>> = foodDao.observeRecent(RECENT_LIMIT).map { rows -> rows.map { it.toModel() } }

    override suspend fun getFood(id: Long): Food? = foodDao.getFood(id)?.toModel()

    override suspend fun setFavorite(foodId: Long, favorite: Boolean) = foodDao.setFavorite(foodId, favorite)

    override suspend fun saveCustomFood(input: CustomFoodInput): Long {
        val servings = if (input.servingGrams != null) {
            val label = input.servingLabel?.takeIf { it.isNotBlank() } ?: "1 porsi"
            listOf(FoodServingEntity(foodId = 0, position = 0, labelId = label, labelEn = label, grams = input.servingGrams))
        } else {
            emptyList()
        }
        return foodDao.upsertFoodWithServings(
            FoodEntity(
                seedKey = null,
                nameId = input.name.trim(),
                nameEn = input.name.trim(),
                category = FoodCategory.OTHER.name,
                kcalPer100g = input.per100g.kcal,
                proteinPer100g = input.per100g.proteinG,
                carbsPer100g = input.per100g.carbsG,
                fatPer100g = input.per100g.fatG,
                isCustom = true,
                isFavorite = false,
                source = null,
                createdAtEpochMs = timeSource.now().toEpochMilli(),
            ),
            servings,
        )
    }

    override suspend fun deleteCustomFood(foodId: Long) = foodDao.deleteCustomFood(foodId)

    override fun diary(date: LocalDate): Flow<List<FoodLogEntry>> =
        foodDao.observeLog(date.toString()).map { rows -> rows.map { it.toModel() } }

    override fun diaryRange(from: LocalDate, to: LocalDate): Flow<List<FoodLogEntry>> =
        foodDao.observeLogRange(from.toString(), to.toString()).map { rows -> rows.map { it.toModel() } }

    override suspend fun logFood(date: LocalDate, meal: MealType, food: Food, portion: Portion): Long {
        val nutrients = PortionCalculator.nutrients(food.per100g, portion.grams)
        return foodDao.insertLog(
            FoodLogEntity(
                date = date.toString(),
                meal = meal.name,
                foodId = food.id,
                nameId = food.nameId,
                nameEn = food.nameEn,
                grams = portion.grams,
                quantity = portion.quantity,
                servingLabelId = portion.serving?.labelId,
                servingLabelEn = portion.serving?.labelEn,
                kcal = nutrients.kcal,
                proteinG = nutrients.proteinG,
                carbsG = nutrients.carbsG,
                fatG = nutrients.fatG,
                createdAtEpochMs = timeSource.now().toEpochMilli(),
            ),
        )
    }

    override suspend fun logRecipe(date: LocalDate, meal: MealType, recipe: Recipe, servings: Double): Long {
        val nutrients = recipe.nutrientsPerServing * servings
        val gramsPerServing = recipe.ingredients.sumOf { it.grams } / recipe.servings
        return foodDao.insertLog(
            FoodLogEntity(
                date = date.toString(),
                meal = meal.name,
                foodId = null,
                nameId = recipe.nameId,
                nameEn = recipe.nameEn,
                grams = gramsPerServing * servings,
                quantity = servings,
                servingLabelId = "porsi",
                servingLabelEn = "serving",
                kcal = nutrients.kcal,
                proteinG = nutrients.proteinG,
                carbsG = nutrients.carbsG,
                fatG = nutrients.fatG,
                createdAtEpochMs = timeSource.now().toEpochMilli(),
            ),
        )
    }

    override suspend fun updateAmount(entryId: Long, quantity: Double, grams: Double) {
        val entry = foodDao.getLogEntry(entryId) ?: return
        // Nutrients are linear in weight, so scaling the stored snapshot is exact for foods and recipes.
        val factor = if (entry.grams > 0) grams / entry.grams else 1.0
        val nutrients = Nutrients(entry.kcal, entry.proteinG, entry.carbsG, entry.fatG) * factor
        foodDao.updateLog(
            entry.copy(
                grams = grams,
                quantity = quantity,
                kcal = nutrients.kcal,
                proteinG = nutrients.proteinG,
                carbsG = nutrients.carbsG,
                fatG = nutrients.fatG,
            ),
        )
    }

    override suspend fun deleteEntry(entryId: Long) = foodDao.deleteLog(entryId)

    private companion object {
        // Large enough to list the whole bundled database when the search is empty.
        const val SEARCH_LIMIT = 300
        const val RECENT_LIMIT = 15
    }
}
