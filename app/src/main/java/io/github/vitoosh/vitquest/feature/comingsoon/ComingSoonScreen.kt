package io.github.vitoosh.vitquest.feature.comingsoon

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.ComingSoonBadge
import io.github.vitoosh.vitquest.core.designsystem.component.IconBubble
import io.github.vitoosh.vitquest.core.designsystem.component.VqTopAppBar
import io.github.vitoosh.vitquest.core.designsystem.icon.VqIcons
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing

/** Placeholder content for tabs whose features arrive in later development phases. */
enum class ComingSoonFeature(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val plannedRes: List<Int>,
    val icon: ImageVector,
) {
    NUTRITION(
        titleRes = R.string.feature_nutrition_title,
        descriptionRes = R.string.feature_nutrition_description,
        plannedRes = listOf(
            R.string.feature_nutrition_planned_1,
            R.string.feature_nutrition_planned_2,
            R.string.feature_nutrition_planned_3,
        ),
        icon = VqIcons.Restaurant,
    ),
    WORKOUTS(
        titleRes = R.string.feature_workouts_title,
        descriptionRes = R.string.feature_workouts_description,
        plannedRes = listOf(
            R.string.feature_workouts_planned_1,
            R.string.feature_workouts_planned_2,
            R.string.feature_workouts_planned_3,
        ),
        icon = VqIcons.FitnessCenter,
    ),
    REWARDS(
        titleRes = R.string.feature_rewards_title,
        descriptionRes = R.string.feature_rewards_description,
        plannedRes = listOf(
            R.string.feature_rewards_planned_1,
            R.string.feature_rewards_planned_2,
            R.string.feature_rewards_planned_3,
        ),
        icon = VqIcons.Trophy,
    ),
}

@Composable
fun ComingSoonScreen(
    @StringRes screenTitleRes: Int,
    feature: ComingSoonFeature,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            VqTopAppBar(title = stringResource(screenTitleRes)) {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        CenteredContent(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = VqSpacing.xl, vertical = VqSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(VqSpacing.lg),
            ) {
                IconBubble(icon = feature.icon, size = 72)
                ComingSoonBadge()
                Text(
                    text = stringResource(feature.titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(feature.descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(
                    modifier = Modifier.padding(top = VqSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(VqSpacing.sm),
                ) {
                    Text(
                        text = stringResource(R.string.coming_soon_planned_heading),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    feature.plannedRes.forEach { res ->
                        Row(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(text = stringResource(res), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
