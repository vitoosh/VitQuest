package io.github.vitoosh.vitquest.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.domain.nutrition.mealForHour
import io.github.vitoosh.vitquest.core.model.MealType
import io.github.vitoosh.vitquest.core.model.WorkoutRequest
import io.github.vitoosh.vitquest.feature.comingsoon.ComingSoonFeature
import io.github.vitoosh.vitquest.feature.comingsoon.ComingSoonScreen
import io.github.vitoosh.vitquest.feature.home.HomeActions
import io.github.vitoosh.vitquest.feature.nutrition.NutritionScreen
import io.github.vitoosh.vitquest.feature.workouts.WorkoutsScreen
import io.github.vitoosh.vitquest.feature.workouts.generator.WorkoutRoutes
import java.time.LocalDate
import java.time.LocalTime
import io.github.vitoosh.vitquest.feature.activity.ActivityRoute as ActivityScreenRoute
import io.github.vitoosh.vitquest.feature.home.HomeRoute as HomeScreenRoute
import io.github.vitoosh.vitquest.feature.habits.HabitsRoute as HabitsScreenRoute
import io.github.vitoosh.vitquest.feature.hydration.HydrationRoute as HydrationScreenRoute
import io.github.vitoosh.vitquest.feature.progress.ProgressRoute as ProgressScreenRoute
import io.github.vitoosh.vitquest.feature.workouts.library.ExerciseDetailRoute as ExerciseDetailScreenRoute
import io.github.vitoosh.vitquest.feature.workouts.player.WorkoutPlayerRoute as WorkoutPlayerScreenRoute
import io.github.vitoosh.vitquest.feature.nutrition.food.AddFoodRoute as AddFoodScreenRoute
import io.github.vitoosh.vitquest.feature.nutrition.plan.ShoppingListRoute as ShoppingListScreenRoute
import io.github.vitoosh.vitquest.feature.nutrition.recipes.RecipeDetailRoute as RecipeDetailScreenRoute
import io.github.vitoosh.vitquest.feature.profile.EditProfileRoute as EditProfileScreenRoute
import io.github.vitoosh.vitquest.feature.settings.SettingsRoute as SettingsScreenRoute
import io.github.vitoosh.vitquest.feature.weight.WeightRoute as WeightScreenRoute

@Composable
fun VitQuestNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val openSettings = { navController.navigate(SettingsRoute) { launchSingleTop = true } }
    val editProfile = { navController.navigate(EditProfileRoute) { launchSingleTop = true } }
    val openHydration = { navController.navigate(HydrationRoute) { launchSingleTop = true } }
    val openWeight = { navController.navigate(WeightRoute) { launchSingleTop = true } }
    val openRecipe = { id: Long -> navController.navigate(RecipeDetailRoute(id)) }
    val openHabits = { navController.navigate(HabitsRoute) { launchSingleTop = true } }
    val openProgress = { navController.navigate(ProgressRoute) { launchSingleTop = true } }
    val openExercise = { key: String -> navController.navigate(ExerciseDetailRoute(key)) }
    val startWorkout = { request: WorkoutRequest ->
        navController.navigate(
            WorkoutPlayerRoute(
                minutes = request.minutes,
                level = request.level.name,
                location = request.location.name,
                equipment = WorkoutRoutes.equipmentText(request.equipment),
                focus = request.focus.name,
                seed = request.seed,
            ),
        )
    }
    val addFood = { date: LocalDate, meal: MealType ->
        navController.navigate(AddFoodRoute(date.toString(), meal.name))
    }

    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        composable<HomeRoute> {
            HomeScreenRoute(
                actions = HomeActions(
                    openSettings = openSettings,
                    editProfile = editProfile,
                    openActivity = { navController.navigateToTopLevel(TopLevelDestination.ACTIVITY) },
                    openNutrition = { navController.navigateToTopLevel(TopLevelDestination.NUTRITION) },
                    logFood = { addFood(LocalDate.now(), mealForHour(LocalTime.now().hour)) },
                    openHydration = openHydration,
                    openWeight = openWeight,
                    openWorkouts = { navController.navigateToTopLevel(TopLevelDestination.WORKOUTS) },
                    openHabits = openHabits,
                    openProgress = openProgress,
                ),
            )
        }
        composable<ActivityRoute> {
            ActivityScreenRoute(onOpenSettings = openSettings)
        }
        composable<NutritionRoute> {
            NutritionScreen(
                onOpenSettings = openSettings,
                onAddFood = addFood,
                onOpenRecipe = openRecipe,
                onOpenShoppingList = { navController.navigate(ShoppingListRoute) { launchSingleTop = true } },
                onOpenHydration = openHydration,
                onEditProfile = editProfile,
            )
        }
        composable<WorkoutsRoute> {
            WorkoutsScreen(
                onOpenSettings = openSettings,
                onOpenHabits = openHabits,
                onOpenProgress = openProgress,
                onStartWorkout = startWorkout,
                onOpenExercise = openExercise,
            )
        }
        composable<RewardsRoute> {
            ComingSoonScreen(R.string.nav_rewards, ComingSoonFeature.REWARDS, onOpenSettings = openSettings)
        }
        composable<SettingsRoute> {
            SettingsScreenRoute(onNavigateBack = navController::navigateUp, onEditProfile = editProfile)
        }
        composable<EditProfileRoute> {
            EditProfileScreenRoute(onNavigateBack = navController::navigateUp)
        }
        composable<AddFoodRoute> {
            AddFoodScreenRoute(onNavigateBack = navController::navigateUp)
        }
        composable<RecipeDetailRoute> {
            RecipeDetailScreenRoute(onNavigateBack = navController::navigateUp)
        }
        composable<ShoppingListRoute> {
            ShoppingListScreenRoute(onNavigateBack = navController::navigateUp)
        }
        composable<WeightRoute> {
            WeightScreenRoute(onNavigateBack = navController::navigateUp)
        }
        composable<HydrationRoute> {
            HydrationScreenRoute(onNavigateBack = navController::navigateUp)
        }
        composable<ExerciseDetailRoute> {
            ExerciseDetailScreenRoute(onNavigateBack = navController::navigateUp)
        }
        composable<WorkoutPlayerRoute> {
            WorkoutPlayerScreenRoute(onClose = navController::navigateUp)
        }
        composable<HabitsRoute> {
            HabitsScreenRoute(onNavigateBack = navController::navigateUp)
        }
        composable<ProgressRoute> {
            ProgressScreenRoute(onNavigateBack = navController::navigateUp, onOpenWeight = openWeight)
        }
    }
}
