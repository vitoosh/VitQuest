package io.github.vitoosh.vitquest.feature.reminders

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.content.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.SectionHeader
import io.github.vitoosh.vitquest.core.designsystem.component.VqTopAppBar
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.domain.reminders.ReminderPlanner
import io.github.vitoosh.vitquest.core.domain.reminders.ReminderProblem
import io.github.vitoosh.vitquest.core.model.ReminderSetting
import io.github.vitoosh.vitquest.core.model.ReminderType
import io.github.vitoosh.vitquest.core.model.ReminderWindow
import io.github.vitoosh.vitquest.core.reminders.ReminderNotifications
import io.github.vitoosh.vitquest.core.ui.currentLocale
import io.github.vitoosh.vitquest.core.ui.dayLabel
import io.github.vitoosh.vitquest.core.ui.formatInteger
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private val INTERVALS = listOf(15, 30, 45, 60, 90, 120)

data class ReminderActions(
    val save: (ReminderSetting) -> Unit = {},
    val saveWindow: (ReminderWindow) -> Unit = {},
    val requestPermission: () -> Unit = {},
    val openBatterySettings: () -> Unit = {},
)

@Composable
fun RemindersRoute(onNavigateBack: () -> Unit, viewModel: RemindersViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var canNotify by rememberSaveable { mutableStateOf(ReminderNotifications.canNotify(context)) }
    LifecycleResumeEffect(Unit) {
        // The user may have changed the permission in system settings.
        canNotify = ReminderNotifications.canNotify(context)
        onPauseOrDispose { }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        canNotify = ReminderNotifications.canNotify(context)
        viewModel.permissionChanged()
    }
    RemindersScreen(
        uiState = uiState,
        canNotify = canNotify,
        actions = ReminderActions(
            save = viewModel::save,
            saveWindow = viewModel::saveWindow,
            requestPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !wasDeniedBefore(context)) {
                    markAsked(context)
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    context.startSafely(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }
            },
            openBatterySettings = { context.startSafely(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
        ),
        onNavigateBack = onNavigateBack,
    )
}

// Android shows the permission dialog only a limited number of times; after the first ask we send
// the user to the system notification settings instead of a request that may silently do nothing.
private const val PREFS = "reminder_permission"
private fun wasDeniedBefore(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("asked", false)
private fun markAsked(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean("asked", true) }

private fun Context.startSafely(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Composable
fun RemindersScreen(uiState: RemindersUiState?, canNotify: Boolean, actions: ReminderActions, onNavigateBack: () -> Unit) {
    var picking by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(topBar = { VqTopAppBar(title = stringResource(R.string.reminders_title), onNavigateBack = onNavigateBack) }) { padding ->
        if (uiState == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        CenteredContent(modifier = Modifier.padding(padding)) {
            LazyColumn(contentPadding = PaddingValues(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
                if (!canNotify) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Column(Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                                Text(stringResource(R.string.reminders_permission_title), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.reminders_permission_text), style = MaterialTheme.typography.bodyMedium)
                                FilledTonalButton(onClick = actions.requestPermission) { Text(stringResource(R.string.reminders_permission_button)) }
                            }
                        }
                    }
                }
                item { WindowCard(uiState.window, onPick = { picking = it }, onClearQuiet = { actions.saveWindow(uiState.window.copy(quietStart = null, quietEnd = null)) }) }
                item { SectionHeader(stringResource(R.string.reminders_types)) }
                items(ReminderType.entries, key = { it.name }) { type ->
                    ReminderCard(
                        setting = uiState.settings.getValue(type),
                        window = uiState.window,
                        next = uiState.next[type],
                        onSave = actions.save,
                        onPickTime = { picking = "time:${type.name}" },
                    )
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                            Text(stringResource(R.string.reminders_battery_title), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.reminders_battery_text), style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(onClick = actions.openBatterySettings) { Text(stringResource(R.string.reminders_battery_button)) }
                        }
                    }
                }
            }
        }
        picking?.let { target ->
            val window = uiState.window
            val initial = when {
                target == "activeStart" -> window.activeStart
                target == "activeEnd" -> window.activeEnd
                target == "quietStart" -> window.quietStart
                target == "quietEnd" -> window.quietEnd
                else -> uiState.settings[ReminderType.valueOf(target.removePrefix("time:"))]?.time
            }
            TimeDialog(
                title = stringResource(
                    when (target) {
                        "activeStart" -> R.string.reminders_active_start
                        "activeEnd" -> R.string.reminders_active_end
                        "quietStart" -> R.string.reminders_quiet_start
                        "quietEnd" -> R.string.reminders_quiet_end
                        else -> R.string.reminders_time
                    },
                ),
                initial = initial,
                onDismiss = { picking = null },
                onPicked = { time ->
                    picking = null
                    when (target) {
                        // Picking a start leads straight to the matching end.
                        "activeStart" -> { actions.saveWindow(window.copy(activeStart = time)); picking = "activeEnd" }
                        "activeEnd" -> actions.saveWindow(window.copy(activeEnd = time))
                        "quietStart" -> { actions.saveWindow(window.copy(quietStart = time)); picking = "quietEnd" }
                        "quietEnd" -> actions.saveWindow(window.copy(quietEnd = time))
                        else -> {
                            val type = ReminderType.valueOf(target.removePrefix("time:"))
                            actions.save(uiState.settings.getValue(type).copy(time = time, enabled = true))
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun WindowCard(window: ReminderWindow, onPick: (String) -> Unit, onClearQuiet: () -> Unit) {
    val notSet = stringResource(R.string.reminders_not_set)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
            Text(stringResource(R.string.reminders_active_hours), style = MaterialTheme.typography.titleMedium)
            Text(if (window.hasActiveHours) rangeText(window.activeStart!!, window.activeEnd!!) else notSet, style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.reminders_active_hours_help), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { onPick("activeStart") }) { Text(stringResource(R.string.reminders_set_hours)) }
            Text(stringResource(R.string.reminders_quiet_hours), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = VqSpacing.sm))
            Text(if (window.hasQuietHours) rangeText(window.quietStart!!, window.quietEnd!!) else stringResource(R.string.reminders_quiet_none), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.reminders_quiet_help), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                OutlinedButton(onClick = { onPick("quietStart") }) { Text(stringResource(R.string.reminders_set_hours)) }
                if (window.hasQuietHours) TextButton(onClick = onClearQuiet) { Text(stringResource(R.string.reminders_clear_quiet)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderCard(
    setting: ReminderSetting,
    window: ReminderWindow,
    next: ZonedDateTime?,
    onSave: (ReminderSetting) -> Unit,
    onPickTime: () -> Unit,
) {
    val type = setting.type
    val title = stringResource(ReminderNotifications.channelName(type))
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(type.descriptionRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = setting.enabled,
                    onCheckedChange = { enabled ->
                        // A timed reminder needs a time chosen by the user before it can run.
                        if (enabled && !type.usesInterval && setting.time == null) onPickTime() else onSave(setting.copy(enabled = enabled))
                    },
                    modifier = Modifier.semantics { contentDescription = title },
                )
            }
            if (type.usesInterval) {
                Text(stringResource(R.string.reminders_every), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                    INTERVALS.forEach { minutes ->
                        FilterChip(
                            selected = setting.intervalMinutes == minutes,
                            onClick = { onSave(setting.copy(intervalMinutes = minutes)) },
                            label = { Text(stringResource(R.string.value_minutes, formatInteger(minutes))) },
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                    Text(setting.time?.let { timeText(it) } ?: stringResource(R.string.reminders_not_set), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onPickTime) { Text(stringResource(R.string.reminders_change_time)) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(VqSpacing.xs)) {
                    val locale = currentLocale()
                    DayOfWeek.entries.forEach { day ->
                        val selected = day in setting.days
                        FilterChip(
                            selected = selected,
                            onClick = { onSave(setting.copy(days = if (selected) setting.days - day else setting.days + day)) },
                            label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) },
                        )
                    }
                }
            }
            if (setting.enabled) {
                when (ReminderPlanner.problem(setting, window)) {
                    ReminderProblem.NEEDS_ACTIVE_HOURS -> Warning(stringResource(R.string.reminders_needs_active_hours))
                    ReminderProblem.NEEDS_TIME -> Warning(stringResource(R.string.reminders_needs_time))
                    ReminderProblem.NEEDS_DAYS -> Warning(stringResource(R.string.reminders_needs_days))
                    null -> next?.let {
                        Text(
                            text = stringResource(R.string.reminders_next, dayLabel(it.toLocalDate(), java.time.LocalDate.now(it.zone)) + " " + timeText(it.toLocalTime())),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(stringResource(R.string.reminders_skip_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Warning(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}

private val ReminderType.descriptionRes: Int
    get() = when (this) {
        ReminderType.WATER -> R.string.reminder_water_description
        ReminderType.MOVEMENT -> R.string.reminder_movement_description
        ReminderType.WORKOUT -> R.string.reminder_workout_description
        ReminderType.FOOD_LOG -> R.string.reminder_food_log_description
        ReminderType.WEIGHT -> R.string.reminder_weight_description
    }

@Composable
private fun timeText(time: LocalTime): String = DateTimeFormatter.ofPattern("HH:mm", currentLocale()).format(time)

@Composable
private fun rangeText(start: LocalTime, end: LocalTime): String = timeText(start) + "–" + timeText(end)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(title: String, initial: LocalTime?, onDismiss: () -> Unit, onPicked: (LocalTime) -> Unit) {
    // The picker needs a starting point; it is only saved once the user confirms.
    val state = rememberTimePickerState(initialHour = initial?.hour ?: LocalTime.now().hour, initialMinute = initial?.minute ?: 0, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onPicked(LocalTime.of(state.hour, state.minute)) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
