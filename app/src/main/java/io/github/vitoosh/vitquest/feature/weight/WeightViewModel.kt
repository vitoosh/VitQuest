package io.github.vitoosh.vitquest.feature.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.core.data.repository.BodyRepository
import io.github.vitoosh.vitquest.core.data.repository.MeasurementInput
import io.github.vitoosh.vitquest.core.data.repository.ProfileRepository
import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.domain.FieldResult
import io.github.vitoosh.vitquest.core.domain.InputLimits
import io.github.vitoosh.vitquest.core.domain.NumberInput
import io.github.vitoosh.vitquest.core.domain.errorOrNull
import io.github.vitoosh.vitquest.core.domain.weight.WeightStats
import io.github.vitoosh.vitquest.core.domain.weight.WeightSummary
import io.github.vitoosh.vitquest.core.model.BodyMeasurement
import io.github.vitoosh.vitquest.core.model.WeightEntry
import io.github.vitoosh.vitquest.core.time.TimeSource
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WeightUiState(
    val today: LocalDate,
    val entries: List<WeightEntry>,
    val summary: WeightSummary,
    val targetKg: Double?,
    val measurements: List<BodyMeasurement>,
)

data class MeasurementForm(
    val waist: String = "",
    val hip: String = "",
    val chest: String = "",
    val arm: String = "",
    val thigh: String = "",
)

data class MeasurementErrors(
    val waist: FieldError? = null,
    val hip: FieldError? = null,
    val chest: FieldError? = null,
    val arm: FieldError? = null,
    val thigh: FieldError? = null,
    val allEmpty: Boolean = false,
) {
    val isEmpty get() = waist == null && hip == null && chest == null && arm == null && thigh == null && !allEmpty
}

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val bodyRepository: BodyRepository,
    profileRepository: ProfileRepository,
    private val timeSource: TimeSource,
) : ViewModel() {

    val uiState: StateFlow<WeightUiState?> = combine(
        bodyRepository.weights(),
        bodyRepository.measurements(),
        profileRepository.goals.map { it?.targetWeightKg },
    ) { weights, measurements, target ->
        val today = timeSource.today()
        WeightUiState(
            today = today,
            entries = weights,
            summary = WeightStats.summarize(weights, target, today),
            targetKg = target,
            measurements = measurements,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun saveWeight(date: LocalDate, input: String, note: String): FieldError? {
        if (date.isAfter(timeSource.today())) return FieldError.OUT_OF_RANGE
        return when (val result = NumberInput.parseDecimal(input, InputLimits.WEIGHT_KG, required = true)) {
            is FieldResult.Invalid -> result.error
            is FieldResult.Valid -> {
                viewModelScope.launch { bodyRepository.logWeight(date, requireNotNull(result.value), note) }
                null
            }
        }
    }

    fun deleteWeight(entry: WeightEntry) {
        viewModelScope.launch { bodyRepository.deleteWeight(entry.id) }
    }

    fun saveMeasurement(form: MeasurementForm): MeasurementErrors {
        val range = InputLimits.BODY_MEASUREMENT_CM
        val waist = NumberInput.parseDecimal(form.waist, range)
        val hip = NumberInput.parseDecimal(form.hip, range)
        val chest = NumberInput.parseDecimal(form.chest, range)
        val arm = NumberInput.parseDecimal(form.arm, range)
        val thigh = NumberInput.parseDecimal(form.thigh, range)
        val values = listOf(waist, hip, chest, arm, thigh).map { (it as? FieldResult.Valid)?.value }
        val errors = MeasurementErrors(
            waist = waist.errorOrNull,
            hip = hip.errorOrNull,
            chest = chest.errorOrNull,
            arm = arm.errorOrNull,
            thigh = thigh.errorOrNull,
            allEmpty = values.all { it == null } && listOf(waist, hip, chest, arm, thigh).all { it is FieldResult.Valid },
        )
        if (!errors.isEmpty) return errors
        viewModelScope.launch {
            bodyRepository.saveMeasurement(timeSource.today(), MeasurementInput(values[0], values[1], values[2], values[3], values[4]))
        }
        return errors
    }

    fun deleteMeasurement(item: BodyMeasurement) {
        viewModelScope.launch { bodyRepository.deleteMeasurement(item.id) }
    }
}
