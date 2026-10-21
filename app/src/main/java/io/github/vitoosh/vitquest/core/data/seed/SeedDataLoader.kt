package io.github.vitoosh.vitquest.core.data.seed

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.vitoosh.vitquest.core.data.local.FoodDao
import io.github.vitoosh.vitquest.core.data.local.RecipeDao
import io.github.vitoosh.vitquest.core.data.preferences.UserPreferencesDataSource
import io.github.vitoosh.vitquest.core.time.TimeSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Loads the bundled food and recipe database the first time, and again
 * whenever [SEED_VERSION] is raised. Built-in rows are matched by seed key,
 * so custom foods, favourites and meal plans are never lost.
 */
@Singleton
class SeedDataLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val foodDao: FoodDao,
    private val recipeDao: RecipeDao,
    private val preferences: UserPreferencesDataSource,
    private val timeSource: TimeSource,
) {
    private val mutex = Mutex()

    suspend fun ensureLoaded() = mutex.withLock {
        val loaded = preferences.seedDataVersion()
        if (loaded >= SEED_VERSION) return@withLock
        withContext(Dispatchers.IO) {
            val foods = SeedImporter.parseFoods(readAsset("seed/foods.json"))
            val recipes = SeedImporter.parseRecipes(readAsset("seed/recipes.json"))
            SeedImporter.foodRows(foods, timeSource.now().toEpochMilli()).forEach { (food, servings) ->
                foodDao.upsertFoodWithServings(food, servings)
            }
            SeedImporter.recipeRows(recipes, foods).forEach { (recipe, ingredients) ->
                recipeDao.upsertRecipe(recipe, ingredients)
            }
        }
        preferences.setSeedDataVersion(SEED_VERSION)
    }

    private fun readAsset(path: String): String = context.assets.open(path).bufferedReader().use { it.readText() }

    companion object {
        /** Raise when assets/seed changes so existing installs pick up the new values. */
        const val SEED_VERSION = 2
    }
}
