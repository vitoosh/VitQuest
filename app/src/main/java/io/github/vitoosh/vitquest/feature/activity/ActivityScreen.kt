package io.github.vitoosh.vitquest.feature.activity

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.core.app.ActivityCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.data.health.AndroidHealthConnectStepsSource
import io.github.vitoosh.vitquest.core.data.repository.HealthConnectStatus
import io.github.vitoosh.vitquest.core.data.repository.PhoneSensorStatus
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.IconBubble
import io.github.vitoosh.vitquest.core.designsystem.component.SectionHeader
import io.github.vitoosh.vitquest.core.designsystem.component.VqTopAppBar
import io.github.vitoosh.vitquest.core.designsystem.icon.VqIcons
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.domain.steps.StepDay
import io.github.vitoosh.vitquest.core.domain.steps.StepSourceResolver
import io.github.vitoosh.vitquest.core.domain.steps.StepStats
import io.github.vitoosh.vitquest.core.model.StepSourcePreference
import io.github.vitoosh.vitquest.core.ui.dayLabel
import io.github.vitoosh.vitquest.core.ui.formatInteger
import java.time.LocalDate

/** Actions that need the platform (permission prompts, other apps). */
data class ActivityPlatformActions(
    val connectHealthConnect: () -> Unit,
    val installHealthConnect: () -> Unit,
    val openHealthConnectSettings: () -> Unit,
    val enablePhoneSensor: () -> Unit,
    val openAppSettings: () -> Unit,
)

@Composable
fun ActivityRoute(
    onOpenSettings: () -> Unit,
    viewModel: ActivityViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    var sensorPermanentlyDenied by rememberSaveable { mutableStateOf(false) }

    // Permissions can change in system settings, so re-check every time the screen resumes.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val healthConnectLauncher = rememberLauncherForActivityResult(
        AndroidHealthConnectStepsSource.permissionContract(),
    ) { viewModel.refresh(force = true) }

    val sensorLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        sensorPermanentlyDenied = !granted && activity != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACTIVITY_RECOGNITION)
        viewModel.refresh(force = true)
    }

    val actions = ActivityPlatformActions(
        connectHealthConnect = { healthConnectLauncher.launch(AndroidHealthConnectStepsSource.STEPS_PERMISSIONS) },
        installHealthConnect = {
            if (!context.startSafely(AndroidHealthConnectStepsSource.installIntent())) {
                context.startSafely(AndroidHealthConnectStepsSource.installWebIntent())
            }
        },
        openHealthConnectSettings = { context.startSafely(AndroidHealthConnectStepsSource.settingsIntent()) },
        enablePhoneSensor = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                sensorLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            } else {
                viewModel.refresh(force = true)
            }
        },
        openAppSettings = {
            context.startSafely(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
            )
        },
    )

    ActivityScreen(
        uiState = uiState,
        sensorPermanentlyDenied = sensorPermanentlyDenied,
        actions = actions,
        onSaveManualSteps = viewModel::saveManualSteps,
        onClearManualSteps = viewModel::clearManualSteps,
        onSaveStepGoal = viewModel::saveStepGoal,
        onPreferenceSelected = viewModel::setPreference,
        onRetry = { viewModel.refresh(force = true) },
        onOpenSettings = onOpenSettings,
    )
}

private fun Context.startSafely(intent: Intent): Boolean = try {
    startActivity(intent)
    true
} catch (e: ActivityNotFoundException) {
    false
}

private enum class Rationale { HEALTH_CONNECT, PHONE_SENSOR }

@Composable
fun ActivityScreen(
    uiState: ActivityUiState,
    sensorPermanentlyDenied: Boolean,
    actions: ActivityPlatformActions,
    onSaveManualSteps: (LocalDate, String) -> FieldError?,
    onClearManualSteps: (LocalDate) -> Unit,
    onSaveStepGoal: (String) -> FieldError?,
    onPreferenceSelected: (StepSourcePreference) -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var editingDate by rememberSaveable { mutableStateOf<String?>(null) }
    var editingGoal by rememberSaveable { mutableStateOf(false) }
    var choosingSource by rememberSaveable { mutableStateOf(false) }
    var rationale by rememberSaveable { mutableStateOf<Rationale?>(null) }

    Scaffold(
        topBar = {
            VqTopAppBar(title = stringResource(R.string.nav_activity)) {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        when (uiState) {
            ActivityUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is ActivityUiState.Ready -> CenteredContent(modifier = Modifier.padding(padding)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(VqSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(VqSpacing.lg),
                ) {
                    item {
                        TodayStepsCard(
                            state = uiState,
                            onLogManually = { editingDate = uiState.today.date.toString() },
                            onEditGoal = { editingGoal = true },
                        )
                    }
                    item {
                        StepSourcesCard(
                            state = uiState,
                            sensorPermanentlyDenied = sensorPermanentlyDenied,
                            actions = actions,
                            onConnectHealthConnect = { rationale = Rationale.HEALTH_CONNECT },
                            onEnableSensor = { rationale = Rationale.PHONE_SENSOR },
                            onChangePreference = { choosingSource = true },
                            onRetry = onRetry,
                        )
                    }
                    item { WeekCard(state = uiState) }
                    item {
                        SectionHeader(
                            text = stringResource(R.string.steps_history_title),
                            modifier = Modifier.padding(top = VqSpacing.sm),
                        )
                    }
                    items(uiState.history, key = { it.date.toString() }) { day ->
                        HistoryRow(
                            day = day,
                            today = uiState.today.date,
                            goal = uiState.goal,
                            onClick = { editingDate = day.date.toString() },
                        )
                    }
                    item { LimitationsNote() }
                }

                editingDate?.let { LocalDate.parse(it) }?.let { date ->
                    val day = uiState.history.firstOrNull { it.date == date }
                    ManualStepsDialog(
                        date = date,
                        today = uiState.today.date,
                        current = day?.steps,
                        onSave = { input -> onSaveManualSteps(date, input).also { if (it == null) editingDate = null } },
                        onClear = {
                            onClearManualSteps(date)
                            editingDate = null
                        },
                        onDismiss = { editingDate = null },
                    )
                }
                if (editingGoal) {
                    StepGoalDialog(
                        currentGoal = uiState.goal,
                        onSave = { input -> onSaveStepGoal(input).also { if (it == null) editingGoal = false } },
                        onDismiss = { editingGoal = false },
                    )
                }
                if (choosingSource) {
                    SourcePreferenceDialog(
                        selected = uiState.preference,
                        onSelect = {
                            onPreferenceSelected(it)
                            choosingSource = false
                        },
                        onDismiss = { choosingSource = false },
                    )
                }
                rationale?.let { kind ->
                    PermissionRationaleDialog(
                        title = stringResource(
                            if (kind == Rationale.HEALTH_CONNECT) R.string.rationale_health_connect_title else R.string.rationale_sensor_title,
                        ),
                        message = stringResource(
                            if (kind == Rationale.HEALTH_CONNECT) R.string.rationale_health_connect_message else R.string.rationale_sensor_message,
                        ),
                        onContinue = {
                            rationale = null
                            if (kind == Rationale.HEALTH_CONNECT) actions.connectHealthConnect() else actions.enablePhoneSensor()
                        },
                        onDismiss = { rationale = null },
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayStepsCard(
    state: ActivityUiState.Ready,
    onLogManually: () -> Unit,
    onEditGoal: () -> Unit,
) {
    val today = state.today.steps
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
                IconBubble(icon = VqIcons.Walk)
                Text(
                    text = stringResource(R.string.steps_today_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (today != null) StepSourceTag(today.source)
            }
            if (today != null) {
                Text(text = formatInteger(today.steps), style = MaterialTheme.typography.displaySmall)
                state.goal?.let { goal ->
                    LinearProgressIndicator(
                        progress = { StepStats.progress(today.steps, goal) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = if (today.steps >= goal) {
                            stringResource(R.string.steps_goal_reached, formatInteger(goal))
                        } else {
                            stringResource(R.string.steps_remaining, formatInteger(goal - today.steps), formatInteger(goal))
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(text = stringResource(R.string.steps_no_data_today), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(R.string.steps_no_data_today_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                OutlinedButton(onClick = onLogManually) { Text(stringResource(R.string.steps_log_manually)) }
                TextButton(onClick = onEditGoal) { Text(stringResource(R.string.steps_edit_goal)) }
            }
        }
    }
}

@Composable
private fun StepSourcesCard(
    state: ActivityUiState.Ready,
    sensorPermanentlyDenied: Boolean,
    actions: ActivityPlatformActions,
    onConnectHealthConnect: () -> Unit,
    onEnableSensor: () -> Unit,
    onChangePreference: () -> Unit,
    onRetry: () -> Unit,
) {
    val status = state.status
    val hcUsed = StepSourceResolver.usesHealthConnect(state.preference)
    val sensorUsed = StepSourceResolver.usesPhoneSensor(state.preference)

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
            SectionHeader(stringResource(R.string.steps_sources_title))

            SourceRow(
                icon = VqIcons.Shield,
                title = stringResource(R.string.step_source_health_connect),
                description = when (status?.healthConnect) {
                    null -> stringResource(R.string.source_checking)
                    HealthConnectStatus.NOT_SUPPORTED -> stringResource(R.string.hc_status_not_supported)
                    HealthConnectStatus.UPDATE_REQUIRED -> stringResource(R.string.hc_status_update_required)
                    HealthConnectStatus.PERMISSION_REQUIRED -> stringResource(R.string.hc_status_permission_required)
                    HealthConnectStatus.READ_FAILED -> stringResource(R.string.hc_status_read_failed)
                    HealthConnectStatus.CONNECTED -> stringResource(
                        if (hcUsed) R.string.hc_status_connected else R.string.source_status_not_used,
                    )
                },
            ) {
                when (status?.healthConnect) {
                    HealthConnectStatus.UPDATE_REQUIRED ->
                        FilledTonalButton(onClick = actions.installHealthConnect) { Text(stringResource(R.string.action_install)) }
                    HealthConnectStatus.PERMISSION_REQUIRED -> Row(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                        FilledTonalButton(onClick = onConnectHealthConnect) { Text(stringResource(R.string.action_connect)) }
                        TextButton(onClick = actions.openHealthConnectSettings) { Text(stringResource(R.string.action_manage)) }
                    }
                    HealthConnectStatus.READ_FAILED ->
                        TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
                    HealthConnectStatus.CONNECTED ->
                        TextButton(onClick = actions.openHealthConnectSettings) { Text(stringResource(R.string.action_manage)) }
                    else -> Unit
                }
            }

            HorizontalDivider()

            SourceRow(
                icon = VqIcons.Walk,
                title = stringResource(R.string.step_source_phone_sensor),
                description = when (status?.phoneSensor) {
                    null -> stringResource(R.string.source_checking)
                    PhoneSensorStatus.NOT_AVAILABLE -> stringResource(R.string.sensor_status_not_available)
                    PhoneSensorStatus.PERMISSION_REQUIRED -> stringResource(
                        if (sensorPermanentlyDenied) R.string.sensor_status_denied else R.string.sensor_status_permission_required,
                    )
                    PhoneSensorStatus.ACTIVE -> stringResource(
                        if (sensorUsed) R.string.sensor_status_active else R.string.source_status_not_used,
                    )
                },
            ) {
                if (status?.phoneSensor == PhoneSensorStatus.PERMISSION_REQUIRED) {
                    if (sensorPermanentlyDenied) {
                        FilledTonalButton(onClick = actions.openAppSettings) { Text(stringResource(R.string.action_open_settings)) }
                    } else {
                        FilledTonalButton(onClick = onEnableSensor) { Text(stringResource(R.string.action_enable)) }
                    }
                }
            }

            HorizontalDivider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.steps_preference_title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = stringResource(state.preference.labelRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onChangePreference) { Text(stringResource(R.string.action_change)) }
            }
            Text(
                text = stringResource(R.string.steps_no_double_counting),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SourceRow(
    icon: ImageVector,
    title: String,
    description: String,
    actions: @Composable () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
        IconBubble(icon = icon, size = 36)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VqSpacing.xs)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            actions()
        }
    }
}

@Composable
private fun WeekCard(state: ActivityUiState.Ready) {
    val summary = state.weekSummary
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
            SectionHeader(stringResource(R.string.steps_week_title))
            WeeklyStepsChart(days = state.week, goal = state.goal)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
                StatItem(
                    label = stringResource(R.string.steps_week_total),
                    value = if (summary.daysWithData == 0) "–" else formatInteger(summary.totalSteps.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()),
                    modifier = Modifier.weight(1f),
                )
                StatItem(
                    label = stringResource(R.string.steps_week_average),
                    value = summary.averageSteps?.let { formatInteger(it) } ?: "–",
                    modifier = Modifier.weight(1f),
                )
                StatItem(
                    label = stringResource(R.string.steps_week_goal_days),
                    value = if (state.goal == null) "–" else stringResource(R.string.steps_days_fraction, summary.daysGoalMet, summary.dayCount),
                    modifier = Modifier.weight(1f),
                )
            }
            if (summary.daysWithData in 1 until summary.dayCount) {
                Text(
                    text = pluralStringResource(R.plurals.steps_week_partial, summary.dayCount, summary.daysWithData, summary.dayCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Text(text = value, style = MaterialTheme.typography.titleMedium)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HistoryRow(day: StepDay, today: LocalDate, goal: Int?, onClick: () -> Unit) {
    val steps = day.steps
    ListItem(
        headlineContent = { Text(dayLabel(day.date, today)) },
        supportingContent = {
            Text(
                text = steps?.let { stringResource(R.string.value_steps, formatInteger(it.steps)) }
                    ?: stringResource(R.string.steps_no_data_short),
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                if (steps != null) StepSourceTag(steps.source)
                if (steps != null && goal != null && steps.steps >= goal) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.steps_goal_met_description),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}

@Composable
private fun LimitationsNote() {
    Row(
        modifier = Modifier.padding(vertical = VqSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(VqSpacing.md),
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = stringResource(R.string.steps_limitations),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
