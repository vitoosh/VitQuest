package io.github.vitoosh.vitquest.feature.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.core.data.local.ObtainOutcome
import io.github.vitoosh.vitquest.core.data.repository.BudgetEntryInput
import io.github.vitoosh.vitquest.core.data.repository.RewardInput
import io.github.vitoosh.vitquest.core.data.repository.RewardRepository
import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.domain.rewards.LevelProgress
import io.github.vitoosh.vitquest.core.domain.rewards.Leveling
import io.github.vitoosh.vitquest.core.model.BudgetEntry
import io.github.vitoosh.vitquest.core.model.BudgetEntryKind
import io.github.vitoosh.vitquest.core.model.ObtainedReward
import io.github.vitoosh.vitquest.core.model.Reward
import io.github.vitoosh.vitquest.core.model.RewardTransaction
import io.github.vitoosh.vitquest.core.model.RewardType
import io.github.vitoosh.vitquest.core.time.TimeSource
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RewardsUiState(
    val today: LocalDate,
    val level: LevelProgress,
    val transactions: List<RewardTransaction>,
    val rewards: List<Reward>,
    val history: List<ObtainedReward>,
    val budgetEntries: List<BudgetEntry>,
    val monthlyBudget: Long?,
) {
    /** Real money spent on rewards this calendar month. */
    val spentThisMonth: Long
        get() = budgetEntries.filter { it.kind == BudgetEntryKind.SPENDING && it.date.year == today.year && it.date.month == today.month }
            .sumOf { it.amountRupiah }

    fun savedFor(rewardId: Long): Long = budgetEntries.filter { it.rewardId == rewardId }
        .sumOf { if (it.kind == BudgetEntryKind.SAVING) it.amountRupiah else 0L }

    fun timesObtained(rewardId: Long): Int = history.count { it.rewardId == rewardId }
}

enum class RewardMessage { OBTAINED, ALREADY_OBTAINED, REWARD_MISSING, UNDONE }

data class RewardForm(val name: String, val description: String, val type: RewardType, val price: String)

data class RewardFormErrors(val name: FieldError? = null, val price: FieldError? = null) {
    val isEmpty: Boolean get() = name == null && price == null
}

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val repository: RewardRepository,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val today = MutableStateFlow(timeSource.today())
    private val _message = MutableStateFlow<RewardMessage?>(null)
    val message: StateFlow<RewardMessage?> = _message.asStateFlow()

    private val ledger = combine(repository.xp, repository.transactions()) { xp, transactions -> xp to transactions }

    val uiState: StateFlow<RewardsUiState?> = combine(
        ledger,
        repository.rewards(),
        repository.history(),
        combine(repository.budgetEntries(), repository.monthlyBudget) { entries, budget -> entries to budget },
        today,
    ) { (xp, transactions), rewards, history, (entries, budget), date ->
        RewardsUiState(date, Leveling.progress(xp), transactions, rewards, history, entries, budget)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Picks up anything earned since the last visit. */
    fun refresh() {
        today.value = timeSource.today()
        viewModelScope.launch { repository.sync() }
    }

    fun messageShown() {
        _message.value = null
    }

    fun saveReward(id: Long?, form: RewardForm): RewardFormErrors {
        val price: Result<Long?> = if (form.price.isBlank()) Result.success(null) else RupiahInput.parse(form.price)
        val errors = RewardFormErrors(
            name = if (form.name.isBlank()) FieldError.REQUIRED else null,
            price = price.exceptionOrNull()?.let { it as? FieldException }?.error,
        )
        if (!errors.isEmpty) return errors
        viewModelScope.launch {
            repository.saveReward(id, RewardInput(form.name, form.description, form.type, price.getOrThrow()))
        }
        return errors
    }

    fun setWishlist(reward: Reward, wishlist: Boolean) {
        viewModelScope.launch { repository.setWishlist(reward.id, wishlist) }
    }

    fun deleteReward(reward: Reward) {
        viewModelScope.launch { repository.deleteReward(reward.id) }
    }

    /** Blank [pricePaid] records the reward without any spending. */
    fun markObtained(requestId: String, reward: Reward, pricePaid: String): FieldError? {
        val price: Result<Long?> = if (pricePaid.isBlank()) Result.success(null) else RupiahInput.parse(pricePaid)
        price.exceptionOrNull()?.let { return (it as? FieldException)?.error ?: FieldError.INVALID_NUMBER }
        viewModelScope.launch {
            _message.value = when (repository.markObtained(requestId, reward.id, price.getOrThrow())) {
                is ObtainOutcome.Recorded -> RewardMessage.OBTAINED
                is ObtainOutcome.AlreadyRecorded -> RewardMessage.ALREADY_OBTAINED
                ObtainOutcome.RewardMissing -> RewardMessage.REWARD_MISSING
            }
        }
        return null
    }

    fun undoObtained(entry: ObtainedReward) {
        viewModelScope.launch { if (repository.undoObtained(entry.id)) _message.value = RewardMessage.UNDONE }
    }

    fun addBudgetEntry(kind: BudgetEntryKind, amount: String, rewardId: Long?, note: String): FieldError? {
        val parsed = RupiahInput.parse(amount)
        parsed.exceptionOrNull()?.let { return (it as? FieldException)?.error ?: FieldError.INVALID_NUMBER }
        viewModelScope.launch {
            repository.addBudgetEntry(BudgetEntryInput(timeSource.today(), kind, parsed.getOrThrow(), rewardId, note))
        }
        return null
    }

    fun deleteBudgetEntry(entry: BudgetEntry) {
        viewModelScope.launch { repository.deleteBudgetEntry(entry.id) }
    }

    /** Blank clears the budget. */
    fun setMonthlyBudget(input: String): FieldError? {
        if (input.isBlank()) {
            viewModelScope.launch { repository.setMonthlyBudget(null) }
            return null
        }
        val parsed = RupiahInput.parse(input)
        parsed.exceptionOrNull()?.let { return (it as? FieldException)?.error ?: FieldError.INVALID_NUMBER }
        viewModelScope.launch { repository.setMonthlyBudget(parsed.getOrThrow()) }
        return null
    }
}

class FieldException(val error: FieldError) : Exception()

/** Parses whole rupiah amounts typed with or without thousand separators (85.000, 85,000, 85000). */
object RupiahInput {
    val RANGE = 1L..1_000_000_000L

    fun parse(input: String): Result<Long> {
        val trimmed = input.trim().removePrefix("Rp").removePrefix("rp").trim()
        if (trimmed.isEmpty()) return Result.failure(FieldException(FieldError.REQUIRED))
        if (!trimmed.matches(Regex("[0-9][0-9., ]*"))) return Result.failure(FieldException(FieldError.INVALID_NUMBER))
        val value = trimmed.filter(Char::isDigit).toLongOrNull() ?: return Result.failure(FieldException(FieldError.OUT_OF_RANGE))
        return if (value in RANGE) Result.success(value) else Result.failure(FieldException(FieldError.OUT_OF_RANGE))
    }
}
