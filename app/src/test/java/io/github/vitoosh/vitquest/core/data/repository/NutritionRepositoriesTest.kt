package io.github.vitoosh.vitquest.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.core.data.local.GoalsEntity
import io.github.vitoosh.vitquest.core.data.local.UserProfileEntity
import io.github.vitoosh.vitquest.core.data.local.VitQuestDatabase
import io.github.vitoosh.vitquest.core.data.seed.SeedImporter
import io.github.vitoosh.vitquest.core.model.MealType
import io.github.vitoosh.vitquest.core.model.Nutrients
import io.github.vitoosh.vitquest.testing.FixedTimeSource
import java.io.File
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NutritionRepositoriesTest {

    private lateinit var database: VitQuestDatabase
    private lateinit var nutrition: OfflineNutritionRepository
    private lateinit var recipes: OfflineRecipeRepository
    private lateinit var body: OfflineBodyRepository
    private val time = FixedTimeSource(Instant.parse("2026-10-19T03:00:00Z"))
    private val today = LocalDate.parse("2026-10-19")

    @Before
    fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), VitQuestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        nutrition = OfflineNutritionRepository(database.foodDao(), time)
        recipes = OfflineRecipeRepository(database.recipeDao(), time)
        body = OfflineBodyRepository(database.bodyDao(), database.profileDao(), time)

        val foods = SeedImporter.parseFoods(File("src/main/assets/seed/foods.json").readText())
        val recipeFile = SeedImporter.parseRecipes(File("src/main/assets/seed/recipes.json").readText())
        SeedImporter.foodRows(foods, 0).forEach { (food, servings) -> database.foodDao().upsertFoodWithServings(food, servings) }
        SeedImporter.recipeRows(recipeFile, foods).forEach { (recipe, items) -> database.recipeDao().upsertRecipe(recipe, items) }
    }

    @After
    fun tearDown() = database.close()

    private suspend fun food(query: String) = nutrition.searchFoods(query).first().first()

    @Test
    fun searchFindsIndonesianAndEnglishNames() = runTest {
        assertEquals("Tempe (mentah/kukus)", food("tempe").nameId)
        assertTrue(nutrition.searchFoods("tofu").first().any { it.nameId.startsWith("Tahu") })
    }

    @Test
    fun loggingSnapshotsNutrientsAndScalesWithAmount() = runTest {
        val rice = food("nasi putih")
        val plate = rice.servings.single { it.grams == 150.0 }

        val id = nutrition.logFood(today, MealType.LUNCH, rice, Portion(plate, 1.0, 150.0))
        assertEquals(270.0, nutrition.diary(today).first().single().nutrients.kcal, 0.01)

        nutrition.updateAmount(id, quantity = 2.0, grams = 300.0)
        val entry = nutrition.diary(today).first().single()
        assertEquals(540.0, entry.nutrients.kcal, 0.01)
        assertEquals(2.0, entry.quantity, 0.0)
    }

    @Test
    fun deletingACustomFoodKeepsItsDiaryEntries() = runTest {
        val id = nutrition.saveCustomFood(CustomFoodInput("Keripik", Nutrients(500.0, 5.0, 60.0, 25.0), "1 bungkus", 30.0))
        val custom = requireNotNull(nutrition.getFood(id))
        nutrition.logFood(today, MealType.SNACK, custom, Portion(custom.servings.single(), 1.0, 30.0))

        nutrition.deleteCustomFood(id)

        assertNull(nutrition.getFood(id))
        val entry = nutrition.diary(today).first().single()
        assertEquals("Keripik", entry.nameId)
        assertNull(entry.foodId)
        assertEquals(150.0, entry.nutrients.kcal, 0.01)
    }

    @Test
    fun builtInFoodsCannotBeDeletedAndFavoritesSurviveReseeding() = runTest {
        val tempe = food("tempe")
        nutrition.deleteCustomFood(tempe.id)
        assertEquals(tempe.id, food("tempe").id)

        nutrition.setFavorite(tempe.id, true)
        val foods = SeedImporter.parseFoods(File("src/main/assets/seed/foods.json").readText())
        SeedImporter.foodRows(foods, 0).forEach { (row, servings) -> database.foodDao().upsertFoodWithServings(row, servings) }

        assertTrue(nutrition.favoriteFoods().first().any { it.id == tempe.id })
    }

    @Test
    fun recentFoodsFollowTheDiary() = runTest {
        val egg = food("telur rebus")
        nutrition.logFood(today, MealType.BREAKFAST, egg, Portion(egg.servings.first(), 1.0, 50.0))
        assertEquals(listOf(egg.id), nutrition.recentFoods().first().map { it.id })
    }

    @Test
    fun loggingARecipeUsesItsPerServingNutrition() = runTest {
        val recipe = recipes.recipes().first().first { it.nameId == "Omelet Sayur" }
        nutrition.logRecipe(today, MealType.BREAKFAST, recipe, 1.0)
        assertEquals(recipe.nutrientsPerServing.kcal, nutrition.diary(today).first().single().nutrients.kcal, 0.01)
    }

    @Test
    fun shoppingListComesFromThePlanAndKeepsManualItems() = runTest {
        val all = recipes.recipes().first()
        val soup = all.first { it.nameId == "Sop Ayam Sayuran" }
        recipes.addShoppingItem("Sabun cuci piring")
        recipes.addToPlan(today, MealType.DINNER, soup.id, 2.0)

        val lines = recipes.generateShoppingList(today, today.plusDays(6))

        val items = recipes.shoppingList().first()
        assertEquals(soup.ingredients.size, lines)
        assertTrue(items.any { it.nameId == "Sabun cuci piring" && !it.fromPlan })
        // 2 of 4 servings -> half of 200 g chicken.
        assertEquals(100.0, items.first { it.nameId == "Dada ayam tanpa kulit" }.quantity!!, 0.01)

        // Regenerating replaces generated items instead of duplicating them.
        recipes.generateShoppingList(today, today.plusDays(6))
        assertEquals(items.size, recipes.shoppingList().first().size)

        recipes.removeFromPlan(recipes.plan(today, today).first().single().id)
        assertTrue(recipes.plan(today, today).first().isEmpty())
    }

    @Test
    fun weightEntriesReplaceSameDayAndSyncTheProfile() = runTest {
        database.profileDao().upsertProfileAndGoals(
            UserProfileEntity(age = 30, heightCm = 170.0, currentWeightKg = 82.0, fitnessExperience = null, activityLevel = null,
                workoutLocation = null, equipment = "", dietaryPreferences = "", updatedAtEpochMs = 0),
            GoalsEntity(targetWeightKg = 75.0, dailyStepGoal = 7_000, dailyWaterTargetMl = 2_000, waterServingMl = 250, updatedAtEpochMs = 0),
        )

        body.logWeight(today.minusDays(7), 82.0, null)
        body.logWeight(today, 81.2, "pagi")
        body.logWeight(today, 81.0, null)

        val weights = body.weights().first()
        assertEquals(listOf(82.0, 81.0), weights.map { it.weightKg })
        assertEquals(81.0, database.profileDao().observeProfile().first()?.currentWeightKg)

        body.deleteWeight(weights.last().id)
        assertEquals(82.0, database.profileDao().observeProfile().first()?.currentWeightKg)
    }

    @Test
    fun waterIsTotalledPerDay() = runTest {
        body.addWater(today, 250)
        body.addWater(today, 500)
        body.addWater(today.minusDays(1), 300)

        assertEquals(750, body.water(today, today).first().sumOf { it.amountMl })
        val log = body.water(today, today).first().first()
        body.deleteWater(log.id)
        assertEquals(1, body.water(today, today).first().size)
        assertFalse(body.water(today.minusDays(1), today.minusDays(1)).first().isEmpty())
    }
}
