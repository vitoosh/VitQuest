package io.github.vitoosh.vitquest.feature.nutrition.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.core.data.repository.BodyRepository
import io.github.vitoosh.vitquest.core.data.repository.NutritionRepository
import io.github.vitoosh.vitquest.core.data.repository.ProfileRepository
import io.github.vitoosh.vitquest.core.data.repository.StepsRepository
import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.domain.FieldResult
import io.github.vitoosh.vitquest.core.domain.InputLimits
import io.github.vitoosh.vitquest.core.domain.NumberInput
import io.github.vitoosh.vitquest.core.domain.nutrition.CalorieTarget
import io.github.vitoosh.vitquest.core.domain.nutrition.DiaryTotals
import io.github.vitoosh.vitquest.core.domain.nutrition.EnergyCalculator
import io.github.vitoosh.vitquest.core.domain.nutrition.EnergyEstimate
import io.github.vitoosh.vitquest.core.domain.nutrition.EnergyInputs
import io.github.vitoosh.vitquest.core.model.FoodLogEntry
import io.github.vitoosh.vitquest.core.time.TimeSource
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EnergySummary(
    val estimate: EnergyEstimate,
    val target: CalorieTarget?,
    /** Rough energy used by today's walking; informational only. */
    val walkingKcal: Int?,
)

sealed interface DiaryUiState {
    data object Loading : DiaryUiState

    data class Ready(
        val date: LocalDate,
        val today: LocalDate,
        val entries: List<FoodLogEntry>,
        val totals: DiaryTotals,
        val energy: EnergySummary,
        val waterMl: Int,
        val waterTargetMl: Int?,
        val waterServingMl: Int,
    ) : DiaryUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository,
    private val profileRepository: ProfileRepository,
    private val bodyRepository: BodyRepository,
    private val stepsRepository: StepsRepository,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val today = MutableStateFlow(timeSource.today())
    private val selectedDate = MutableStateFlow(timeSource.today())

    val uiState: StateFlow<DiaryUiState> = selectedDate.flatMapLatest { date ->
        combine(
            nutritionRepository.diary(date),
            profileRepository.profile,
            profileRepository.goals,
            stepsRepository.observeDailySteps(date, date).map { it[date]?.steps },
            bodyRepository.water(date, date).map { logs -> logs.sumOf { it.amountMl } },
        ) { entries, profile, goals, steps, water ->
            val estimate = EnergyCalculator.estimate(
                EnergyInputs(
                    age = profile?.age,
                    heightCm = profile?.heightCm,
                    weightKg = profile?.currentWeightKg,
                    sex = profile?.sex,
                    activityLevel = profile?.activityLevel,
                    targetWeightKg = goals?.targetWeightKg,
                ),
            )
            DiaryUiState.Ready(
                date = date,
                today = today.value,
                entries = entries,
                totals = DiaryTotals.of(entries),
                energy = EnergySummary(
                    estimate = estimate,
                    target = CalorieTarget.resolve(goals?.dailyCalorieTarget, estimate),
                    walkingKcal = steps?.let { EnergyCalculator.walkingKcal(it, profile?.currentWeightKg) },
                ),
                waterMl = water,
                waterTargetMl = goals?.dailyWaterTargetMl,
                waterServingMl = goals?.waterServingMl ?: DEFAULT_SERVING_ML,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiaryUiState.Loading)

    fun refreshToday() {
        val now = timeSource.today()
        if (today.value != now) {
            // After midnight, follow the new day if the user was looking at "today".
            if (selectedDate.value == today.value) selectedDate.value = now
            today.value = now
        }
    }

    fun selectDate(date: LocalDate) {
        if (!date.isAfter(today.value)) selectedDate.value = date
    }

    fun previousDay() = selectDate(selectedDate.value.minusDays(1))

    fun nextDay() = selectDate(selectedDate.value.plusDays(1))

    fun goToToday() = selectDate(today.value)

    /** Returns an error to show, or null when the new amount was saved. */
    fun updateQuantity(entry: FoodLogEntry, input: String): FieldError? {
        return when (val result = NumberInput.parseDecimal(input, InputLimits.SERVING_QUANTITY, required = true)) {
            is FieldResult.Invalid -> result.error
            is FieldResult.Valid -> {
                val quantity = requireNotNull(result.value)
                val gramsPerUnit = if (entry.quantity > 0) entry.grams / entry.quantity else entry.grams
                viewModelScope.launch { nutritionRepository.updateAmount(entry.id, quantity, gramsPerUnit * quantity) }
                null
            }
        }
    }

    fun deleteEntry(entry: FoodLogEntry) {
        viewModelScope.launch { nutritionRepository.deleteEntry(entry.id) }
    }

    fun saveCalorieTarget(input: String): FieldError? {
        return when (val result = NumberInput.parseInt(input, InputLimits.DAILY_CALORIE_TARGET, required = true)) {
            is FieldResult.Invalid -> result.error
            is FieldResult.Valid -> {
                viewModelScope.launch { profileRepository.updateDailyCalorieTarget(result.value) }
                null
            }
        }
    }

    fun useRecommendedTarget() {
        viewModelScope.launch { profileRepository.updateDailyCalorieTarget(null) }
    }

    fun addWater(amountMl: Int) {
        val date = selectedDate.value
        viewModelScope.launch { bodyRepository.addWater(date, amountMl) }
    }

    private companion object {
        const val DEFAULT_SERVING_ML = 250
    }
}
