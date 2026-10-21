package io.github.vitoosh.vitquest.feature.progress

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.IconBubble
import io.github.vitoosh.vitquest.core.designsystem.component.SectionHeader
import io.github.vitoosh.vitquest.core.designsystem.component.VqTopAppBar
import io.github.vitoosh.vitquest.core.designsystem.icon.VqIcons
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.domain.habits.Achievement
import io.github.vitoosh.vitquest.core.domain.habits.AchievementStatus
import io.github.vitoosh.vitquest.core.domain.habits.WeekSummary
import io.github.vitoosh.vitquest.core.ui.formatInteger
import io.github.vitoosh.vitquest.core.ui.shortDate
import io.github.vitoosh.vitquest.feature.weight.WeightChart
import io.github.vitoosh.vitquest.feature.workouts.minutesText

@get:StringRes
val Achievement.titleRes: Int
    get() = when (this) {
        Achievement.FIRST_WORKOUT -> R.string.achievement_first_workout
        Achievement.WORKOUTS_10 -> R.string.achievement_workouts_10
        Achievement.WORKOUTS_25 -> R.string.achievement_workouts_25
        Achievement.ACTIVE_WEEK -> R.string.achievement_active_week
        Achievement.STEP_GOAL_DAYS_7 -> R.string.achievement_step_goal_7
        Achievement.HYDRATION_DAYS_7 -> R.string.achievement_hydration_7
        Achievement.FOOD_LOG_DAYS_7 -> R.string.achievement_food_log_7
        Achievement.HABIT_CHECK_INS_10 -> R.string.achievement_habit_check_ins_10
    }

@get:StringRes
val Achievement.descriptionRes: Int
    get() = when (this) {
        Achievement.FIRST_WORKOUT -> R.string.achievement_first_workout_description
        Achievement.WORKOUTS_10 -> R.string.achievement_workouts_10_description
        Achievement.WORKOUTS_25 -> R.string.achievement_workouts_25_description
        Achievement.ACTIVE_WEEK -> R.string.achievement_active_week_description
        Achievement.STEP_GOAL_DAYS_7 -> R.string.achievement_step_goal_7_description
        Achievement.HYDRATION_DAYS_7 -> R.string.achievement_hydration_7_description
        Achievement.FOOD_LOG_DAYS_7 -> R.string.achievement_food_log_7_description
        Achievement.HABIT_CHECK_INS_10 -> R.string.achievement_habit_check_ins_10_description
    }

@Composable
fun ProgressRoute(onNavigateBack: () -> Unit, onOpenWeight: () -> Unit, viewModel: ProgressViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refreshToday()
        onPauseOrDispose { }
    }
    ProgressScreen(uiState, onNavigateBack, onOpenWeight)
}

@Composable
fun ProgressScreen(uiState: ProgressUiState?, onNavigateBack: () -> Unit, onOpenWeight: () -> Unit) {
    Scaffold(topBar = { VqTopAppBar(title = stringResource(R.string.progress_title), onNavigateBack = onNavigateBack) }) { padding ->
        if (uiState == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        CenteredContent(modifier = Modifier.padding(padding)) {
            LazyColumn(contentPadding = PaddingValues(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
                item { WeekComparison(uiState.thisWeek, uiState.lastWeek) }
                item {
                    ChartCard(stringResource(R.string.progress_workout_minutes)) {
                        WeeklyBarChart(
                            bars = uiState.weeks.map { week ->
                                ChartBar(shortDate(week.start), stringResource(R.string.progress_week_of, shortDate(week.start)), (week.workoutSeconds + 30) / 60)
                            },
                            valueText = { stringResource(R.string.value_minutes, formatInteger(it)) },
                            emptyText = stringResource(R.string.progress_no_data),
                        )
                    }
                }
                item {
                    ChartCard(stringResource(R.string.progress_average_steps)) {
                        WeeklyBarChart(
                            bars = uiState.weeks.map { week ->
                                ChartBar(shortDate(week.start), stringResource(R.string.progress_week_of, shortDate(week.start)), week.averageSteps)
                            },
                            valueText = { stringResource(R.string.value_steps, formatInteger(it)) },
                            emptyText = stringResource(R.string.progress_no_data),
                        )
                    }
                }
                item {
                    ChartCard(stringResource(R.string.progress_weight_trend)) {
                        if (uiState.weights.isEmpty()) {
                            Text(stringResource(R.string.weight_empty), style = MaterialTheme.typography.bodyMedium)
                        } else {
                            WeightChart(entries = uiState.weights, targetKg = uiState.targetWeightKg)
                        }
                        TextButton(onClick = onOpenWeight) { Text(stringResource(R.string.progress_open_weight)) }
                    }
                }
                item { SectionHeader(stringResource(R.string.progress_achievements)) }
                item {
                    Text(
                        text = stringResource(R.string.progress_achievements_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(uiState.achievements.sortedByDescending { it.earnedOn }, key = { it.achievement.name }) { status ->
                    AchievementRow(status)
                }
            }
        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun WeekComparison(thisWeek: WeekSummary, lastWeek: WeekSummary) {
    val none = stringResource(R.string.progress_no_data)
    ChartCard(stringResource(R.string.progress_this_week)) {
        Row(Modifier.fillMaxWidth()) {
            Text("", modifier = Modifier.weight(1.4f))
            Text(stringResource(R.string.progress_column_this_week), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.progress_column_last_week), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
        ComparisonRow(stringResource(R.string.progress_row_steps), thisWeek.averageSteps?.let { formatInteger(it) } ?: none, lastWeek.averageSteps?.let { formatInteger(it) } ?: none)
        ComparisonRow(stringResource(R.string.progress_row_workout_days), formatInteger(thisWeek.workoutDays), formatInteger(lastWeek.workoutDays))
        ComparisonRow(stringResource(R.string.progress_row_workout_minutes), minutesText(thisWeek.workoutSeconds), minutesText(lastWeek.workoutSeconds))
        ComparisonRow(
            stringResource(R.string.progress_row_water_days),
            thisWeek.waterTargetDays?.let { formatInteger(it) } ?: none,
            lastWeek.waterTargetDays?.let { formatInteger(it) } ?: none,
        )
        ComparisonRow(stringResource(R.string.progress_row_food_days), formatInteger(thisWeek.foodLogDays), formatInteger(lastWeek.foodLogDays))
        Text(
            text = stringResource(R.string.progress_week_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ComparisonRow(label: String, current: String, previous: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.4f))
        Text(current, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text(previous, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AchievementRow(status: AchievementStatus) {
    val achievement = status.achievement
    ListItem(
        leadingContent = {
            if (status.earned) {
                IconBubble(icon = VqIcons.Trophy, size = 40)
            } else {
                Box(Modifier.padding(VqSpacing.sm)) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        headlineContent = { Text(stringResource(achievement.titleRes)) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(VqSpacing.xs)) {
                Text(stringResource(achievement.descriptionRes))
                val earnedOn = status.earnedOn
                if (earnedOn != null) {
                    Text(stringResource(R.string.achievement_earned_on, shortDate(earnedOn)), color = MaterialTheme.colorScheme.primary)
                } else {
                    LinearProgressIndicator(progress = { status.progress.toFloat() / achievement.goal }, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.achievement_progress, formatInteger(status.progress), formatInteger(achievement.goal)))
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
