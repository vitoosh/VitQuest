package io.github.vitoosh.vitquest.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.vitoosh.vitquest.feature.onboarding.OnboardingRoute
import io.github.vitoosh.vitquest.ui.navigation.TopLevelDestination
import io.github.vitoosh.vitquest.ui.navigation.VitQuestNavHost
import io.github.vitoosh.vitquest.ui.navigation.navigateToTopLevel

/** Root UI: onboarding until it is completed, then the main navigation. */
@Composable
fun VitQuestApp(onboardingCompleted: Boolean) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (onboardingCompleted) {
            MainNavigation()
        } else {
            OnboardingRoute()
        }
    }
}

@Composable
private fun MainNavigation(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val onTopLevel = currentDestination == null ||
        TopLevelDestination.entries.any { currentDestination.isOn(it) }

    val adaptiveType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfoV2())
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                item(
                    selected = currentDestination?.isOn(destination) == true,
                    onClick = { navController.navigateToTopLevel(destination) },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = { Text(stringResource(destination.labelRes)) },
                )
            }
        },
        // Detail screens (settings, profile) hide the navigation bar.
        layoutType = if (onTopLevel) adaptiveType else NavigationSuiteType.None,
    ) {
        VitQuestNavHost(navController = navController, modifier = Modifier.fillMaxSize())
    }
}

private fun NavDestination.isOn(destination: TopLevelDestination): Boolean =
    hierarchy.any { it.hasRoute(destination.routeClass) }
