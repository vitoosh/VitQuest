package io.github.vitoosh.vitquest.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.core.data.repository.BodyRepository
import io.github.vitoosh.vitquest.core.data.repository.NutritionRepository
import io.github.vitoosh.vitquest.core.data.repository.ProfileRepository
import io.github.vitoosh.vitquest.core.data.repository.StepsRepository
import io.github.vitoosh.vitquest.core.domain.nutrition.CalorieTarget
import io.github.vitoosh.vitquest.core.domain.nutrition.DiaryTotals
import io.github.vitoosh.vitquest.core.domain.nutrition.EnergyCalculator
import io.github.vitoosh.vitquest.core.domain.nutrition.EnergyEstimate
import io.github.vitoosh.vitquest.core.domain.nutrition.EnergyInputs
import io.github.vitoosh.vitquest.core.domain.weight.WeightStats
import io.github.vitoosh.vitquest.core.domain.weight.WeightSummary
import io.github.vitoosh.vitquest.core.model.DailySteps
import io.github.vitoosh.vitquest.core.model.Goals
import io.github.vitoosh.vitquest.core.model.UserProfile
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

/** What happened today, gathered from every tracker. */
data class TodaySnapshot(
    val steps: DailySteps?,
    val diary: DiaryTotals,
    val waterMl: Int,
)

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Ready(
        val profile: UserProfile?,
        val goals: Goals?,
        /** Null means no source reported steps today, which is different from 0. */
        val todaySteps: DailySteps? = null,
        val diary: DiaryTotals = DiaryTotals.of(emptyList()),
        val waterMl: Int = 0,
        val energy: EnergyEstimate = EnergyEstimate.Missing(emptySet()),
        val calorieTarget: CalorieTarget? = null,
        val weight: WeightSummary? = null,
        val today: LocalDate = LocalDate.now(),
    ) : HomeUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    private val stepsRepository: StepsRepository,
    nutritionRepository: NutritionRepository,
    private val bodyRepository: BodyRepository,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val today = MutableStateFlow(timeSource.today())

    private val snapshot = today.flatMapLatest { date ->
        combine(
            stepsRepository.observeDailySteps(date, date).map { it[date] },
            nutritionRepository.diary(date).map { DiaryTotals.of(it) },
            bodyRepository.water(date, date).map { logs -> logs.sumOf { it.amountMl } },
        ) { steps, diary, water -> TodaySnapshot(steps, diary, water) }
    }

    val uiState: StateFlow<HomeUiState> =
        combine(profileRepository.profile, profileRepository.goals, snapshot, bodyRepository.weights(), today) {
                profile, goals, todayData, weights, date ->
            val energy = EnergyCalculator.estimate(
                EnergyInputs(
                    age = profile?.age,
                    heightCm = profile?.heightCm,
                    weightKg = profile?.currentWeightKg,
                    sex = profile?.sex,
                    activityLevel = profile?.activityLevel,
                    targetWeightKg = goals?.targetWeightKg,
                ),
            )
            HomeUiState.Ready(
                profile = profile,
                goals = goals,
                todaySteps = todayData.steps,
                diary = todayData.diary,
                waterMl = todayData.waterMl,
                energy = energy,
                calorieTarget = CalorieTarget.resolve(goals?.dailyCalorieTarget, energy),
                weight = if (weights.isEmpty()) null else WeightStats.summarize(weights, goals?.targetWeightKg, date),
                today = date,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading,
        )

    fun refresh() {
        today.value = timeSource.today()
        viewModelScope.launch { stepsRepository.refresh() }
    }

    fun addWater(amountMl: Int) {
        viewModelScope.launch { bodyRepository.addWater(timeSource.today(), amountMl) }
    }
}
