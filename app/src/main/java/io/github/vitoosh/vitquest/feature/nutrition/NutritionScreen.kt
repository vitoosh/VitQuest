package io.github.vitoosh.vitquest.feature.nutrition

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.VqTopAppBar
import io.github.vitoosh.vitquest.core.model.MealType
import io.github.vitoosh.vitquest.feature.nutrition.diary.DiaryTab
import io.github.vitoosh.vitquest.feature.nutrition.plan.PlanTab
import io.github.vitoosh.vitquest.feature.nutrition.recipes.RecipesTab
import java.time.LocalDate

private enum class NutritionTab(val labelRes: Int) {
    DIARY(R.string.nutrition_tab_diary),
    RECIPES(R.string.nutrition_tab_recipes),
    PLAN(R.string.nutrition_tab_plan),
}

/** Nutrition tab: food diary with the calorie counter, recipes, and the meal plan. */
@Composable
fun NutritionScreen(
    onOpenSettings: () -> Unit,
    onAddFood: (LocalDate, MealType) -> Unit,
    onOpenRecipe: (Long) -> Unit,
    onOpenShoppingList: () -> Unit,
    onOpenHydration: () -> Unit,
    onEditProfile: () -> Unit,
) {
    var selected by rememberSaveable { mutableIntStateOf(NutritionTab.DIARY.ordinal) }
    Scaffold(
        topBar = {
            VqTopAppBar(title = stringResource(R.string.nav_nutrition)) {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = selected) {
                NutritionTab.entries.forEach { tab ->
                    Tab(
                        selected = selected == tab.ordinal,
                        onClick = { selected = tab.ordinal },
                        text = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
            when (NutritionTab.entries[selected]) {
                NutritionTab.DIARY -> DiaryTab(
                    onAddFood = onAddFood,
                    onOpenHydration = onOpenHydration,
                    onEditProfile = onEditProfile,
                )
                NutritionTab.RECIPES -> RecipesTab(onOpenRecipe = onOpenRecipe)
                NutritionTab.PLAN -> PlanTab(
                    onBrowseRecipes = { selected = NutritionTab.RECIPES.ordinal },
                    onOpenRecipe = onOpenRecipe,
                    onOpenShoppingList = onOpenShoppingList,
                )
            }
        }
    }
}
