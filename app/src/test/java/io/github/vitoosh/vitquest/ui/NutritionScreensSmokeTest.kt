package io.github.vitoosh.vitquest.ui

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.theme.VitQuestTheme
import io.github.vitoosh.vitquest.core.domain.nutrition.CalorieTarget
import io.github.vitoosh.vitquest.core.domain.nutrition.DiaryTotals
import io.github.vitoosh.vitquest.core.domain.nutrition.EnergyEstimate
import io.github.vitoosh.vitquest.core.domain.nutrition.MissingEnergyInput
import io.github.vitoosh.vitquest.core.domain.nutrition.WeightDirection
import io.github.vitoosh.vitquest.core.model.FoodLogEntry
import io.github.vitoosh.vitquest.core.model.MealType
import io.github.vitoosh.vitquest.core.model.Nutrients
import io.github.vitoosh.vitquest.core.model.Recipe
import io.github.vitoosh.vitquest.core.model.RecipeIngredient
import io.github.vitoosh.vitquest.core.model.RecipeTag
import io.github.vitoosh.vitquest.feature.hydration.HydrationDay
import io.github.vitoosh.vitquest.feature.hydration.HydrationScreen
import io.github.vitoosh.vitquest.feature.hydration.HydrationUiState
import io.github.vitoosh.vitquest.feature.nutrition.diary.DiaryContent
import io.github.vitoosh.vitquest.feature.nutrition.diary.DiaryUiState
import io.github.vitoosh.vitquest.feature.nutrition.diary.EnergySummary
import io.github.vitoosh.vitquest.feature.nutrition.recipes.RecipeDetailScreen
import io.github.vitoosh.vitquest.testing.assertAccessible
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en")
class NutritionScreensSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.parse("2026-10-19")

    private fun string(id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private val rice = FoodLogEntry(
        id = 1, date = today, meal = MealType.LUNCH, foodId = 1, nameId = "Nasi putih", nameEn = "Steamed white rice",
        grams = 150.0, quantity = 1.0, servingLabelId = "1 piring (150 g)", servingLabelEn = "1 plate (150 g)",
        nutrients = Nutrients(270.0, 4.5, 59.7, 0.45), createdAt = Instant.EPOCH,
    )

    private fun diaryState(estimate: EnergyEstimate, target: CalorieTarget?) = DiaryUiState.Ready(
        date = today,
        today = today,
        entries = listOf(rice),
        totals = DiaryTotals.of(listOf(rice)),
        energy = EnergySummary(estimate, target, walkingKcal = 260),
        waterMl = 750,
        waterTargetMl = 2_400,
        waterServingMl = 250,
    )

    @Test
    fun diaryShowsCalorieCounterAndMeals() {
        var addedTo: MealType? = null
        val estimate = EnergyEstimate.Available(1_780, 2_760, 2_260, WeightDirection.LOSE, activityAssumed = false, limitedByFloor = false)
        composeRule.setContent {
            VitQuestTheme {
                DiaryContent(
                    uiState = diaryState(estimate, CalorieTarget.Recommended(2_260)),
                    onPrevious = {}, onNext = {}, onToday = {},
                    onAddFood = { _, meal -> addedTo = meal },
                    onUpdateQuantity = { _, _ -> null }, onDelete = {},
                    onSaveTarget = { null }, onUseRecommended = {}, onAddWater = {},
                    onOpenHydration = {}, onEditProfile = {},
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.calories_eaten_of_target, "270", "2,260")).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.calories_remaining, "1,990")).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.data_source_estimate)).assertIsDisplayed()

        // The info button explains BMR and TDEE.
        composeRule.onNodeWithContentDescription(string(R.string.calories_target_info)).performClick()
        composeRule.onNodeWithText(string(R.string.energy_bmr)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_close)).performClick()

        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Steamed white rice"))
        composeRule.onNodeWithText("Steamed white rice").assertIsDisplayed()
        val addDinner = string(R.string.diary_add_food_description, string(R.string.meal_dinner))
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasContentDescription(addDinner))
        composeRule.onNodeWithContentDescription(addDinner).performClick()
        assertEquals(MealType.DINNER, addedTo)
    }

    @Test
    fun diaryWithoutProfileDataAsksForItInsteadOfInventingATarget() {
        composeRule.setContent {
            VitQuestTheme {
                DiaryContent(
                    uiState = diaryState(EnergyEstimate.Missing(setOf(MissingEnergyInput.SEX)), null),
                    onPrevious = {}, onNext = {}, onToday = {}, onAddFood = { _, _ -> },
                    onUpdateQuantity = { _, _ -> null }, onDelete = {},
                    onSaveTarget = { null }, onUseRecommended = {}, onAddWater = {},
                    onOpenHydration = {}, onEditProfile = {},
                )
            }
        }
        composeRule.onNodeWithText(string(R.string.calories_eaten_only, "270")).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.calories_no_target)).assertIsDisplayed()
    }

    @Test
    fun recipeScalesIngredientsWithServings() {
        val recipe = Recipe(
            id = 1, nameId = "Omelet Sayur", nameEn = "Vegetable Omelette", descriptionId = "", descriptionEn = "Quick breakfast",
            servings = 1, prepMinutes = 5, cookMinutes = 5, tags = setOf(RecipeTag.BREAKFAST),
            ingredients = listOf(RecipeIngredient("Telur", "Eggs", 2.0, "butir", "egg", 100.0, null, null, Nutrients(143.0, 12.6, 0.7, 9.5))),
            stepsId = listOf("Kocok telur."), stepsEn = listOf("Beat the eggs."),
        )
        composeRule.setContent {
            VitQuestTheme {
                RecipeDetailScreen(recipe, today, SnackbarHostState(), onLog = { _, _ -> }, onPlan = { _, _, _ -> }, onNavigateBack = {})
            }
        }
        composeRule.assertAccessible()
        composeRule.onNodeWithText(string(R.string.recipe_ingredient_line, "2", "egg", "Eggs", "100")).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.recipe_servings_increase)).performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.recipe_ingredient_line, "4", "egg", "Eggs", "200")).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun hydrationShowsProgressAndQuickAdd() {
        var added = 0
        composeRule.setContent {
            VitQuestTheme {
                HydrationScreen(
                    uiState = HydrationUiState(today, emptyList(), 2_500, 2_400, 250, listOf(HydrationDay(today, 2_500))),
                    onAdd = { added += it },
                    onAddCustom = { null },
                    onDelete = {},
                    onNavigateBack = {},
                )
            }
        }
        composeRule.assertAccessible()
        composeRule.onNodeWithText(string(R.string.water_of_target, "2,500", "2,400")).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.water_goal_reached)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.water_add_amount, "500")).performClick()
        assertEquals(500, added)
    }
}
