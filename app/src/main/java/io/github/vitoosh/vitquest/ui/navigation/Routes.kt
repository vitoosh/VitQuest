package io.github.vitoosh.vitquest.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.icon.VqIcons
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute

@Serializable data object ActivityRoute

@Serializable data object NutritionRoute

@Serializable data object WorkoutsRoute

@Serializable data object SettingsRoute

@Serializable data object EditProfileRoute

/** Date as ISO-8601 and meal as a MealType name, so the route stays serializable. */
@Serializable data class AddFoodRoute(val date: String, val meal: String)

@Serializable data class RecipeDetailRoute(val recipeId: Long)

@Serializable data object ShoppingListRoute

@Serializable data object WeightRoute

@Serializable data object HydrationRoute

@Serializable data class ExerciseDetailRoute(val key: String)

/** The plan is rebuilt from these values, so the player survives process death without storing it. */
@Serializable
data class WorkoutPlayerRoute(
    val minutes: Int,
    val level: String,
    val location: String,
    val equipment: String,
    val focus: String,
    val seed: Long,
)

@Serializable data object HabitsRoute

@Serializable data object ProgressRoute

@Serializable data object RemindersRoute

/** The four bottom-navigation destinations. */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(HomeRoute, HomeRoute::class, R.string.nav_home, Icons.Filled.Home),
    ACTIVITY(ActivityRoute, ActivityRoute::class, R.string.nav_activity, VqIcons.Walk),
    NUTRITION(NutritionRoute, NutritionRoute::class, R.string.nav_nutrition, VqIcons.Restaurant),
    WORKOUTS(WorkoutsRoute, WorkoutsRoute::class, R.string.nav_workouts, VqIcons.FitnessCenter),
}

/** Switches tabs while keeping each tab's own back stack and scroll state. */
fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
