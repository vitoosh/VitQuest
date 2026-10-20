package io.github.vitoosh.vitquest.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.pluralStringResource
import io.github.vitoosh.vitquest.feature.rewards.xpText
import io.github.vitoosh.vitquest.feature.workouts.minutesText
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.DataSourceKind
import io.github.vitoosh.vitquest.core.designsystem.component.DataSourceTag
import io.github.vitoosh.vitquest.core.designsystem.component.IconBubble
import io.github.vitoosh.vitquest.core.designsystem.component.VqTopAppBar
import io.github.vitoosh.vitquest.core.designsystem.icon.VqIcons
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.domain.nutrition.CalorieTarget
import io.github.vitoosh.vitquest.core.domain.nutrition.EnergyEstimate
import io.github.vitoosh.vitquest.core.domain.steps.StepStats
import io.github.vitoosh.vitquest.core.model.DailySteps
import io.github.vitoosh.vitquest.feature.activity.StepSourceTag
import io.github.vitoosh.vitquest.core.ui.currentLocale
import io.github.vitoosh.vitquest.core.ui.formatDecimal
import io.github.vitoosh.vitquest.core.ui.formatInteger
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs

/** Navigation callbacks from Home. */
data class HomeActions(
    val openSettings: () -> Unit = {},
    val editProfile: () -> Unit = {},
    val openActivity: () -> Unit = {},
    val openNutrition: () -> Unit = {},
    val logFood: () -> Unit = {},
    val openHydration: () -> Unit = {},
    val openWeight: () -> Unit = {},
    val openWorkouts: () -> Unit = {},
    val openHabits: () -> Unit = {},
    val openProgress: () -> Unit = {},
    val openRewards: () -> Unit = {},
)

@Composable
fun HomeRoute(
    actions: HomeActions,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    HomeScreen(uiState = uiState, actions = actions, onAddWater = viewModel::addWater)
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    actions: HomeActions,
    onAddWater: (Int) -> Unit = {},
) {
    Scaffold(
        topBar = {
            VqTopAppBar(title = stringResource(R.string.app_name)) {
                IconButton(onClick = actions.editProfile) {
                    Icon(Icons.Filled.Person, contentDescription = stringResource(R.string.action_edit_profile))
                }
                IconButton(onClick = actions.openSettings) {
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
                HomeContent(state = uiState, actions = actions, onAddWater = onAddWater)
            }
        }
    }
}

@Composable
private fun HomeContent(state: HomeUiState.Ready, actions: HomeActions, onAddWater: (Int) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(VqSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(VqSpacing.lg),
    ) {
        item { GreetingHeader() }
        item { QuickActions(state, actions, onAddWater) }
        if (state.energy is EnergyEstimate.Missing) {
            item { CompleteProfileCard(onEditProfile = actions.editProfile) }
        }
        item { TodayStepsCard(steps = state.todaySteps, goal = state.goals?.dailyStepGoal, onOpenActivity = actions.openActivity) }
        item { CaloriesCard(state = state, onOpen = actions.openNutrition) }
        item { WaterCard(state = state, onOpen = actions.openHydration) }
        item { WorkoutCard(state = state, onOpen = actions.openWorkouts) }
        item { HabitsCard(state = state, onOpenHabits = actions.openHabits, onOpenProgress = actions.openProgress) }
        item { WeightCard(state = state, onOpen = actions.openWeight) }
        item { RewardsCard(state = state, onOpen = actions.openRewards) }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickActions(state: HomeUiState.Ready, actions: HomeActions, onAddWater: (Int) -> Unit) {
    val serving = state.goals?.waterServingMl ?: DEFAULT_SERVING_ML
    FlowRow(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
        AssistChip(
            onClick = actions.openWorkouts,
            label = { Text(stringResource(R.string.quick_start_workout)) },
            leadingIcon = { Icon(VqIcons.FitnessCenter, contentDescription = null) },
        )
        AssistChip(
            onClick = actions.logFood,
            label = { Text(stringResource(R.string.quick_log_food)) },
            leadingIcon = { Icon(VqIcons.Restaurant, contentDescription = null) },
        )
        AssistChip(
            onClick = { onAddWater(serving) },
            label = { Text(stringResource(R.string.water_add_amount, formatInteger(serving))) },
            leadingIcon = { Icon(VqIcons.WaterDrop, contentDescription = null) },
        )
        AssistChip(
            onClick = actions.openWeight,
            label = { Text(stringResource(R.string.quick_log_weight)) },
            leadingIcon = { Icon(VqIcons.MonitorWeight, contentDescription = null) },
        )
    }
}

private const val DEFAULT_SERVING_ML = 250

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
private fun HomeCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
                IconBubble(icon = icon, size = 36)
                Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                trailing?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun TodayStepsCard(steps: DailySteps?, goal: Int?, onOpenActivity: () -> Unit) {
    HomeCard(
        icon = VqIcons.Walk,
        title = stringResource(R.string.steps_today_title),
        onClick = onOpenActivity,
        trailing = steps?.let { { StepSourceTag(it.source) } },
    ) {
        if (steps != null) {
            Text(
                text = if (goal != null) {
                    stringResource(R.string.steps_of_goal, formatInteger(steps.steps), formatInteger(goal))
                } else {
                    stringResource(R.string.value_steps, formatInteger(steps.steps))
                },
                style = MaterialTheme.typography.headlineSmall,
            )
            if (goal != null) {
                LinearProgressIndicator(progress = { StepStats.progress(steps.steps, goal) }, modifier = Modifier.fillMaxWidth())
            }
        } else {
            HintText(stringResource(R.string.home_steps_no_data))
        }
    }
}

@Composable
private fun CaloriesCard(state: HomeUiState.Ready, onOpen: () -> Unit) {
    val target = state.calorieTarget
    HomeCard(
        icon = VqIcons.Restaurant,
        title = stringResource(R.string.calories_title),
        onClick = onOpen,
        trailing = target?.let {
            { DataSourceTag(if (it is CalorieTarget.Manual) DataSourceKind.MANUAL else DataSourceKind.ESTIMATE) }
        },
    ) {
        if (target != null) {
            Text(
                text = stringResource(R.string.calories_eaten_of_target, formatInteger(state.diary.kcal), formatInteger(target.kcal)),
                style = MaterialTheme.typography.headlineSmall,
            )
            LinearProgressIndicator(
                progress = { (state.diary.kcal.toFloat() / target.kcal).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            val remaining = state.diary.remainingKcal(target.kcal)
            HintText(
                if (remaining >= 0) {
                    stringResource(R.string.calories_remaining, formatInteger(remaining))
                } else {
                    stringResource(R.string.calories_over, formatInteger(abs(remaining)))
                },
            )
        } else {
            Text(
                text = stringResource(R.string.calories_eaten_only, formatInteger(state.diary.kcal)),
                style = MaterialTheme.typography.headlineSmall,
            )
            HintText(stringResource(R.string.calories_no_target))
        }
        if (state.diary.entryCount == 0) HintText(stringResource(R.string.home_calories_empty))
    }
}

@Composable
private fun WaterCard(state: HomeUiState.Ready, onOpen: () -> Unit) {
    val target = state.goals?.dailyWaterTargetMl
    HomeCard(icon = VqIcons.WaterDrop, title = stringResource(R.string.hydration_title), onClick = onOpen) {
        Text(
            text = target?.let { stringResource(R.string.water_of_target, formatInteger(state.waterMl), formatInteger(it)) }
                ?: stringResource(R.string.value_ml, formatInteger(state.waterMl)),
            style = MaterialTheme.typography.headlineSmall,
        )
        if (target != null && target > 0) {
            LinearProgressIndicator(progress = { (state.waterMl.toFloat() / target).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            if (state.waterMl >= target) HintText(stringResource(R.string.water_goal_reached))
        }
    }
}

@Composable
private fun WeightCard(state: HomeUiState.Ready, onOpen: () -> Unit) {
    val summary = state.weight
    val latest = summary?.latest
    HomeCard(
        icon = VqIcons.MonitorWeight,
        title = stringResource(R.string.weight_title),
        onClick = onOpen,
        trailing = latest?.let { { DataSourceTag(DataSourceKind.MANUAL) } },
    ) {
        if (latest == null) {
            HintText(stringResource(R.string.weight_empty))
            return@HomeCard
        }
        Text(stringResource(R.string.value_kg, formatDecimal(latest.weightKg)), style = MaterialTheme.typography.headlineSmall)
        summary.last30DaysChangeKg?.let { change ->
            HintText(stringResource(R.string.home_weight_change_30, signed(change)))
        }
        state.goals?.targetWeightKg?.let { target ->
            val diff = latest.weightKg - target
            HintText(
                when {
                    abs(diff) < 0.05 -> stringResource(R.string.home_weight_at_target)
                    diff > 0 -> stringResource(R.string.home_weight_to_lose, formatDecimal(diff))
                    else -> stringResource(R.string.home_weight_to_gain, formatDecimal(-diff))
                },
            )
        }
    }
}

@Composable
private fun WorkoutCard(state: HomeUiState.Ready, onOpen: () -> Unit) {
    HomeCard(icon = VqIcons.FitnessCenter, title = stringResource(R.string.home_workout_title), onClick = onOpen) {
        if (state.workoutsToday > 0) {
            Text(
                text = stringResource(R.string.workout_today_done, minutesText(state.workoutSecondsToday)),
                style = MaterialTheme.typography.titleLarge,
            )
        } else {
            HintText(stringResource(R.string.home_workout_none))
        }
        HintText(pluralStringResource(R.plurals.workout_week_days, state.weekWorkoutDays, state.weekWorkoutDays))
        if (state.workoutsToday == 0) {
            FilledTonalButton(onClick = onOpen) { Text(stringResource(R.string.quick_start_workout)) }
        }
    }
}

@Composable
private fun HabitsCard(state: HomeUiState.Ready, onOpenHabits: () -> Unit, onOpenProgress: () -> Unit) {
    val habits = state.habits
    HomeCard(icon = VqIcons.Checklist, title = stringResource(R.string.habits_title), onClick = onOpenHabits) {
        if (habits != null) {
            Text(
                text = stringResource(R.string.habits_today, formatInteger(habits.todayDone), formatInteger(habits.todayTotal)),
                style = MaterialTheme.typography.titleLarge,
            )
            if (habits.todayTotal > 0) {
                LinearProgressIndicator(progress = { habits.todayDone.toFloat() / habits.todayTotal }, modifier = Modifier.fillMaxWidth())
            }
        }
        TextButton(onClick = onOpenProgress) {
            Icon(VqIcons.Insights, contentDescription = null)
            Text(stringResource(R.string.progress_title), modifier = Modifier.padding(start = VqSpacing.xs))
        }
    }
}

@Composable
private fun signed(value: Double): String {
    val sign = if (value > 0.05) "+" else if (value < -0.05) "−" else "±"
    return sign + stringResource(R.string.value_kg, formatDecimal(abs(value)))
}

@Composable
private fun HintText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun RewardsCard(state: HomeUiState.Ready, onOpen: () -> Unit) {
    val level = state.level
    HomeCard(icon = VqIcons.Trophy, title = stringResource(R.string.home_rewards_title), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.rewards_level, formatInteger(level.level)),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            Text(xpText(level.totalXp), style = MaterialTheme.typography.titleMedium)
        }
        LinearProgressIndicator(progress = { level.fraction }, modifier = Modifier.fillMaxWidth())
        HintText(stringResource(R.string.rewards_level_progress, formatInteger(level.xpIntoLevel), formatInteger(level.xpForNext), formatInteger(level.level + 1)))
    }
}
