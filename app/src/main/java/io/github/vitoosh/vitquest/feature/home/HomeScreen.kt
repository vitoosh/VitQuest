package io.github.vitoosh.vitquest.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.DataSourceKind
import io.github.vitoosh.vitquest.core.designsystem.component.DataSourceTag
import io.github.vitoosh.vitquest.core.designsystem.component.FeaturePreviewRow
import io.github.vitoosh.vitquest.core.designsystem.component.IconBubble
import io.github.vitoosh.vitquest.core.designsystem.component.SectionHeader
import io.github.vitoosh.vitquest.core.designsystem.component.VqTopAppBar
import io.github.vitoosh.vitquest.core.designsystem.icon.VqIcons
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.ui.currentLocale
import io.github.vitoosh.vitquest.core.ui.formatDecimal
import io.github.vitoosh.vitquest.core.ui.formatInteger
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs

@Composable
fun HomeRoute(
    onOpenSettings: () -> Unit,
    onEditProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(uiState = uiState, onOpenSettings = onOpenSettings, onEditProfile = onEditProfile)
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onOpenSettings: () -> Unit,
    onEditProfile: () -> Unit,
) {
    Scaffold(
        topBar = {
            VqTopAppBar(title = stringResource(R.string.app_name)) {
                IconButton(onClick = onEditProfile) {
                    Icon(Icons.Filled.Person, contentDescription = stringResource(R.string.action_edit_profile))
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        when (uiState) {
            HomeUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is HomeUiState.Ready -> CenteredContent(modifier = Modifier.padding(padding)) {
                HomeContent(state = uiState, onEditProfile = onEditProfile)
            }
        }
    }
}

@Composable
private fun HomeContent(state: HomeUiState.Ready, onEditProfile: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(VqSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(VqSpacing.lg),
    ) {
        item { GreetingHeader() }
        if (state.profile?.currentWeightKg == null) {
            item { CompleteProfileCard(onEditProfile = onEditProfile) }
        }
        state.goals?.let { goals ->
            item { TargetsCard(state = state, dailySteps = goals.dailyStepGoal, waterMl = goals.dailyWaterTargetMl) }
        }
        item { ComingSoonCard() }
        item {
            Text(
                text = stringResource(R.string.home_local_data_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = VqSpacing.xs),
            )
        }
    }
}

@Composable
private fun GreetingHeader() {
    val locale = currentLocale()
    val now = remember { LocalDateTime.now() }
    val date = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(now)
    }
    Column(verticalArrangement = Arrangement.spacedBy(VqSpacing.xs)) {
        Text(
            text = stringResource(DayPeriod.fromHour(now.hour).greetingRes),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = date, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CompleteProfileCard(onEditProfile: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(
            modifier = Modifier.padding(VqSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VqSpacing.sm),
        ) {
            Text(text = stringResource(R.string.home_complete_profile_title), style = MaterialTheme.typography.titleMedium)
            Text(text = stringResource(R.string.home_complete_profile_description), style = MaterialTheme.typography.bodyMedium)
            FilledTonalButton(onClick = onEditProfile) { Text(stringResource(R.string.action_edit_profile)) }
        }
    }
}

@Composable
private fun TargetsCard(state: HomeUiState.Ready, dailySteps: Int, waterMl: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            modifier = Modifier.padding(VqSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VqSpacing.md),
        ) {
            SectionHeader(stringResource(R.string.home_targets_title))
            TargetRow(
                icon = VqIcons.Walk,
                label = stringResource(R.string.home_target_steps),
                value = stringResource(R.string.value_steps, formatInteger(dailySteps)),
            )
            TargetRow(
                icon = VqIcons.WaterDrop,
                label = stringResource(R.string.home_target_water),
                value = stringResource(R.string.value_ml, formatInteger(waterMl)),
            )
            val currentWeight = state.profile?.currentWeightKg
            if (currentWeight != null) {
                HorizontalDivider()
                TargetRow(
                    icon = VqIcons.MonitorWeight,
                    label = stringResource(R.string.home_current_weight),
                    value = stringResource(R.string.value_kg, formatDecimal(currentWeight)),
                    tag = DataSourceKind.MANUAL,
                )
                state.goals?.targetWeightKg?.let { target ->
                    TargetRow(
                        icon = VqIcons.Flag,
                        label = stringResource(R.string.home_target_weight),
                        value = stringResource(R.string.value_kg, formatDecimal(target)),
                    )
                }
                state.kgToTarget?.let { diff ->
                    Text(
                        text = when {
                            abs(diff) < 0.05 -> stringResource(R.string.home_weight_at_target)
                            diff > 0 -> stringResource(R.string.home_weight_to_lose, formatDecimal(diff))
                            else -> stringResource(R.string.home_weight_to_gain, formatDecimal(-diff))
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = stringResource(R.string.home_targets_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TargetRow(
    icon: ImageVector,
    label: String,
    value: String,
    tag: DataSourceKind? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(VqSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBubble(icon = icon, size = 36)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleMedium)
        }
        if (tag != null) DataSourceTag(kind = tag)
    }
}

@Composable
private fun ComingSoonCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.padding(VqSpacing.lg)) {
            SectionHeader(stringResource(R.string.home_coming_soon_title))
            Text(
                text = stringResource(R.string.home_coming_soon_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = VqSpacing.xs, bottom = VqSpacing.sm),
            )
            FeaturePreviewRow(
                icon = VqIcons.Walk,
                title = stringResource(R.string.feature_activity_title),
                description = stringResource(R.string.feature_activity_summary),
                // The card title already says these are coming soon.
                trailing = null,
            )
            FeaturePreviewRow(
                icon = VqIcons.Restaurant,
                title = stringResource(R.string.feature_nutrition_title),
                description = stringResource(R.string.feature_nutrition_summary),
                trailing = null,
            )
            FeaturePreviewRow(
                icon = VqIcons.WaterDrop,
                title = stringResource(R.string.feature_hydration_title),
                description = stringResource(R.string.feature_hydration_summary),
                trailing = null,
            )
            FeaturePreviewRow(
                icon = VqIcons.FitnessCenter,
                title = stringResource(R.string.feature_workouts_title),
                description = stringResource(R.string.feature_workouts_summary),
                trailing = null,
            )
            FeaturePreviewRow(
                icon = VqIcons.Trophy,
                title = stringResource(R.string.feature_rewards_title),
                description = stringResource(R.string.feature_rewards_summary),
                trailing = null,
            )
        }
    }
}
