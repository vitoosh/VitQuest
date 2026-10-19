package io.github.vitoosh.vitquest.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.icon.VqIcons
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute

@Serializable data object ActivityRoute

@Serializable data object NutritionRoute

@Serializable data object WorkoutsRoute

@Serializable data object RewardsRoute

@Serializable data object SettingsRoute

@Serializable data object EditProfileRoute

/** The five bottom-navigation destinations. */
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
    REWARDS(RewardsRoute, RewardsRoute::class, R.string.nav_rewards, VqIcons.Trophy),
}
