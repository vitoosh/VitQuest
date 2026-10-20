package io.github.vitoosh.vitquest.feature.hydration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.core.data.repository.BodyRepository
import io.github.vitoosh.vitquest.core.data.repository.ProfileRepository
import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.domain.FieldResult
import io.github.vitoosh.vitquest.core.domain.InputLimits
import io.github.vitoosh.vitquest.core.domain.NumberInput
import io.github.vitoosh.vitquest.core.model.WaterLog
import io.github.vitoosh.vitquest.core.time.TimeSource
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HydrationDay(val date: LocalDate, val totalMl: Int)

data class HydrationUiState(
    val today: LocalDate,
    val todayLogs: List<WaterLog>,
    val todayMl: Int,
    val targetMl: Int?,
    val servingMl: Int,
    /** Last 7 days including today, newest first. */
    val week: List<HydrationDay>,
) {
    val goalReached: Boolean get() = targetMl != null && todayMl >= targetMl
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HydrationViewModel @Inject constructor(
    private val bodyRepository: BodyRepository,
    profileRepository: ProfileRepository,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val today = MutableStateFlow(timeSource.today())

    val uiState: StateFlow<HydrationUiState?> = today.flatMapLatest { date ->
        val from = date.minusDays(WEEK_DAYS - 1)
        combine(bodyRepository.water(from, date), profileRepository.goals) { logs, goals ->
            val byDate = logs.groupBy { it.date }
            val todayLogs = byDate[date].orEmpty()
            HydrationUiState(
                today = date,
                todayLogs = todayLogs.sortedByDescending { it.loggedAt },
                todayMl = todayLogs.sumOf { it.amountMl },
                targetMl = goals?.dailyWaterTargetMl,
                servingMl = goals?.waterServingMl ?: DEFAULT_SERVING_ML,
                week = (0 until WEEK_DAYS).map { offset ->
                    val day = date.minusDays(offset)
                    HydrationDay(day, byDate[day].orEmpty().sumOf { it.amountMl })
                },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Each day starts from zero: totals are per local date, so nothing needs resetting. */
    fun refreshToday() {
        today.value = timeSource.today()
    }

    fun add(amountMl: Int) {
        viewModelScope.launch { bodyRepository.addWater(timeSource.today(), amountMl) }
    }

    fun addCustom(input: String): FieldError? =
        when (val result = NumberInput.parseInt(input, InputLimits.WATER_LOG_ML, required = true)) {
            is FieldResult.Invalid -> result.error
            is FieldResult.Valid -> {
                add(requireNotNull(result.value))
                null
            }
        }

    fun delete(log: WaterLog) {
        viewModelScope.launch { bodyRepository.deleteWater(log.id) }
    }

    private companion object {
        const val WEEK_DAYS = 7L
        const val DEFAULT_SERVING_ML = 250
    }
}
