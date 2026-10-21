package io.github.vitoosh.vitquest.feature.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.data.repository.HealthConnectStatus
import io.github.vitoosh.vitquest.core.data.repository.StepSensorTracker
import io.github.vitoosh.vitquest.core.data.repository.StepSourcesStatus
import io.github.vitoosh.vitquest.core.data.repository.StepsRepository
import io.github.vitoosh.vitquest.core.data.sensor.StepSensorSource
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.SectionHeader
import io.github.vitoosh.vitquest.core.designsystem.component.VqTopAppBar
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.domain.steps.SensorCheckpoint
import io.github.vitoosh.vitquest.core.domain.steps.StepDiagnostics
import io.github.vitoosh.vitquest.core.domain.steps.StepSourceResolver
import io.github.vitoosh.vitquest.core.model.DailySteps
import io.github.vitoosh.vitquest.core.model.StepRecord
import io.github.vitoosh.vitquest.core.model.StepSource
import io.github.vitoosh.vitquest.core.model.StepSourcePreference
import io.github.vitoosh.vitquest.core.time.TimeSource
import io.github.vitoosh.vitquest.core.ui.currentLocale
import io.github.vitoosh.vitquest.core.ui.formatInteger
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StepDiagnosticsUiState(
    val diagnostics: StepDiagnostics,
    val permissionGranted: Boolean,
    val checkpoint: SensorCheckpoint?,
    val todayRecords: List<StepRecord>,
    val shown: DailySteps?,
    val preference: StepSourcePreference,
    val status: StepSourcesStatus?,
    val zone: ZoneId,
)

@HiltViewModel
class StepDiagnosticsViewModel @Inject constructor(
    stepsRepository: StepsRepository,
    tracker: StepSensorTracker,
    private val sensor: StepSensorSource,
    private val timeSource: TimeSource,
) : ViewModel() {
    private val today = timeSource.today()

    val uiState: StateFlow<StepDiagnosticsUiState?> = combine(
        tracker.diagnostics,
        stepsRepository.sensorCheckpoint,
        stepsRepository.observeRecords(today),
        stepsRepository.preference,
        stepsRepository.sourcesStatus,
    ) { diagnostics, checkpoint, records, preference, status ->
        StepDiagnosticsUiState(
            diagnostics = diagnostics,
            permissionGranted = sensor.hasPermission(),
            checkpoint = checkpoint,
            todayRecords = records,
            shown = StepSourceResolver.resolve(records, preference)[today],
            preference = preference,
            status = status,
            zone = timeSource.zone(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun StepDiagnosticsRoute(onNavigateBack: () -> Unit, viewModel: StepDiagnosticsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StepDiagnosticsScreen(state, onNavigateBack)
}

/** Raw numbers behind the step count, so a wrong count can be traced to its source. Nothing here leaves the phone. */
@Composable
fun StepDiagnosticsScreen(state: StepDiagnosticsUiState?, onNavigateBack: () -> Unit) {
    Scaffold(topBar = { VqTopAppBar(title = stringResource(R.string.diagnostics_title), onNavigateBack = onNavigateBack) }) { padding ->
        CenteredContent(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(VqSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(VqSpacing.md),
            ) {
                Text(stringResource(R.string.diagnostics_intro), style = MaterialTheme.typography.bodyMedium)
                if (state == null) return@Column
                val d = state.diagnostics
                val yes = stringResource(R.string.diagnostics_yes)
                val no = stringResource(R.string.diagnostics_no)
                val none = stringResource(R.string.diagnostics_none)
                val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(currentLocale())
                fun time(at: Instant?) = at?.let { formatter.format(it.atZone(state.zone)) } ?: none

                DiagnosticsCard(stringResource(R.string.diagnostics_collection)) {
                    Line(stringResource(R.string.diagnostics_permission), if (state.permissionGranted) yes else no)
                    Line(
                        stringResource(R.string.diagnostics_collecting),
                        when {
                            !d.collecting -> stringResource(R.string.diagnostics_stopped)
                            d.serviceRunning -> stringResource(R.string.diagnostics_collecting_service)
                            else -> stringResource(R.string.diagnostics_collecting_app)
                        },
                    )
                    Line(stringResource(R.string.diagnostics_preference), stringResource(state.preference.labelRes))
                }
                DiagnosticsCard(stringResource(R.string.step_source_phone_sensor)) {
                    Line(stringResource(R.string.diagnostics_available), if (d.counterAvailable) yes else no)
                    Line(stringResource(R.string.diagnostics_counter_value), d.lastCounterValue?.let { formatInteger(it) } ?: none)
                    Line(stringResource(R.string.diagnostics_counter_time), time(d.lastCounterAt))
                    Line(stringResource(R.string.diagnostics_checkpoint), state.checkpoint?.let { formatInteger(it.counter) } ?: none)
                    Line(stringResource(R.string.diagnostics_checkpoint_time), time(state.checkpoint?.recordedAt))
                    Hint(stringResource(R.string.diagnostics_counter_hint))
                }
                DiagnosticsCard(stringResource(R.string.step_source_motion)) {
                    Line(stringResource(R.string.diagnostics_available), if (d.motionAvailable) yes else no)
                    Line(stringResource(R.string.diagnostics_motion_samples), formatInteger(d.motionSamplesThisSession))
                    Line(stringResource(R.string.diagnostics_motion_steps), formatInteger(d.motionStepsThisSession))
                    Line(stringResource(R.string.diagnostics_motion_last_step), time(d.lastMotionStepAt))
                    if (d.serviceRunning) {
                        Line(stringResource(R.string.diagnostics_motion_wake_up), if (d.motionWakeUp) yes else no)
                    }
                    Hint(stringResource(R.string.diagnostics_motion_hint))
                }
                DiagnosticsCard(stringResource(R.string.diagnostics_today)) {
                    StepSource.entries.forEach { source ->
                        val steps = state.todayRecords.firstOrNull { it.source == source }?.steps
                        Line(stringResource(source.labelRes), steps?.let { formatInteger(it) } ?: none)
                    }
                    Line(
                        stringResource(R.string.diagnostics_shown),
                        state.shown?.let { formatInteger(it.steps) + " · " + stringResource(it.source.labelRes) } ?: none,
                        bold = true,
                    )
                }
                DiagnosticsCard(stringResource(R.string.step_source_health_connect)) {
                    Line(
                        stringResource(R.string.diagnostics_status),
                        when (state.status?.healthConnect) {
                            null -> stringResource(R.string.source_checking)
                            HealthConnectStatus.NOT_SUPPORTED -> stringResource(R.string.hc_status_not_supported)
                            HealthConnectStatus.UPDATE_REQUIRED -> stringResource(R.string.hc_status_update_required)
                            HealthConnectStatus.PERMISSION_REQUIRED -> stringResource(R.string.hc_status_permission_required)
                            HealthConnectStatus.READ_FAILED -> stringResource(R.string.hc_status_read_failed)
                            HealthConnectStatus.CONNECTED -> stringResource(R.string.hc_status_connected)
                        },
                    )
                    Line(stringResource(R.string.diagnostics_last_sync), time(state.status?.lastHealthConnectSync))
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
            SectionHeader(title)
            content()
        }
    }
}

@Composable
private fun Line(label: String, value: String, bold: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.Bold else null)
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
