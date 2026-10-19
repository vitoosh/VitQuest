package io.github.vitoosh.vitquest.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.feature.activity.ActivityRoute as ActivityScreenRoute
import io.github.vitoosh.vitquest.feature.comingsoon.ComingSoonFeature
import io.github.vitoosh.vitquest.feature.comingsoon.ComingSoonScreen
import io.github.vitoosh.vitquest.feature.home.HomeRoute as HomeScreenRoute
import io.github.vitoosh.vitquest.feature.profile.EditProfileRoute as EditProfileScreenRoute
import io.github.vitoosh.vitquest.feature.settings.SettingsRoute as SettingsScreenRoute

@Composable
fun VitQuestNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val openSettings = { navController.navigate(SettingsRoute) { launchSingleTop = true } }
    val editProfile = { navController.navigate(EditProfileRoute) { launchSingleTop = true } }

    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        composable<HomeRoute> {
            HomeScreenRoute(
                onOpenSettings = openSettings,
                onEditProfile = editProfile,
                onOpenActivity = { navController.navigateToTopLevel(TopLevelDestination.ACTIVITY) },
            )
        }
        composable<ActivityRoute> {
            ActivityScreenRoute(onOpenSettings = openSettings)
        }
        composable<NutritionRoute> {
            ComingSoonScreen(R.string.nav_nutrition, ComingSoonFeature.NUTRITION, onOpenSettings = openSettings)
        }
        composable<WorkoutsRoute> {
            ComingSoonScreen(R.string.nav_workouts, ComingSoonFeature.WORKOUTS, onOpenSettings = openSettings)
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
    }
}
