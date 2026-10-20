package io.github.vitoosh.vitquest.feature.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.core.data.local.RedeemOutcome
import io.github.vitoosh.vitquest.core.data.repository.BudgetEntryInput
import io.github.vitoosh.vitquest.core.data.repository.RewardInput
import io.github.vitoosh.vitquest.core.data.repository.RewardRepository
import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.domain.rewards.LevelProgress
import io.github.vitoosh.vitquest.core.domain.rewards.Leveling
import io.github.vitoosh.vitquest.core.model.BudgetEntry
import io.github.vitoosh.vitquest.core.model.BudgetEntryKind
import io.github.vitoosh.vitquest.core.model.Redemption
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
    val credits: Int,
    val transactions: List<RewardTransaction>,
    val rewards: List<Reward>,
    val redemptions: List<Redemption>,
    val budgetEntries: List<BudgetEntry>,
    val monthlyBudget: Long?,
) {
    /** Real money spent on rewards this calendar month. */
    val spentThisMonth: Long
        get() = budgetEntries.filter { it.kind == BudgetEntryKind.SPENDING && it.date.year == today.year && it.date.month == today.month }
            .sumOf { it.amountRupiah }

    fun savedFor(rewardId: Long): Long = budgetEntries.filter { it.rewardId == rewardId }
        .sumOf { if (it.kind == BudgetEntryKind.SAVING) it.amountRupiah else 0L }
}

enum class RewardMessage { REDEEMED, ALREADY_REDEEMED, NOT_ENOUGH_CREDITS, REWARD_MISSING, CANCELLED }

data class RewardForm(val name: String, val description: String, val type: RewardType, val cost: String, val price: String)

data class RewardFormErrors(val name: FieldError? = null, val cost: FieldError? = null, val price: FieldError? = null) {
    val isEmpty: Boolean get() = name == null && cost == null && price == null
}

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val repository: RewardRepository,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val today = MutableStateFlow(timeSource.today())
    private val _message = MutableStateFlow<RewardMessage?>(null)
    val message: StateFlow<RewardMessage?> = _message.asStateFlow()

    private val ledger = combine(repository.xp, repository.credits, repository.transactions()) { xp, credits, transactions ->
        Triple(xp, credits, transactions)
    }

    val uiState: StateFlow<RewardsUiState?> = combine(
        ledger,
        repository.rewards(),
        repository.redemptions(),
        combine(repository.budgetEntries(), repository.monthlyBudget) { entries, budget -> entries to budget },
        today,
    ) { (xp, credits, transactions), rewards, redemptions, (entries, budget), date ->
        RewardsUiState(date, Leveling.progress(xp), credits, transactions, rewards, redemptions, entries, budget)
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
        val cost = RupiahInput.parseCount(form.cost, COST_RANGE)
        val price: Result<Long?> = if (form.price.isBlank()) Result.success(null) else RupiahInput.parse(form.price)
        val errors = RewardFormErrors(
            name = if (form.name.isBlank()) FieldError.REQUIRED else null,
            cost = cost.exceptionOrNull()?.let { it as? FieldException }?.error,
            price = price.exceptionOrNull()?.let { it as? FieldException }?.error,
        )
        if (!errors.isEmpty) return errors
        viewModelScope.launch {
            repository.saveReward(id, RewardInput(form.name, form.description, form.type, cost.getOrThrow(), price.getOrThrow()))
        }
        return errors
    }

    fun setWishlist(reward: Reward, wishlist: Boolean) {
        viewModelScope.launch { repository.setWishlist(reward.id, wishlist) }
    }

    fun deleteReward(reward: Reward) {
        viewModelScope.launch { repository.deleteReward(reward.id) }
    }

    fun redeem(requestId: String, reward: Reward) {
        viewModelScope.launch {
            _message.value = when (repository.redeem(requestId, reward.id)) {
                is RedeemOutcome.Redeemed -> RewardMessage.REDEEMED
                is RedeemOutcome.AlreadyRedeemed -> RewardMessage.ALREADY_REDEEMED
                is RedeemOutcome.NotEnoughCredits -> RewardMessage.NOT_ENOUGH_CREDITS
                RedeemOutcome.RewardMissing -> RewardMessage.REWARD_MISSING
            }
        }
    }

    fun cancelRedemption(redemption: Redemption) {
        viewModelScope.launch { if (repository.cancelRedemption(redemption.id)) _message.value = RewardMessage.CANCELLED }
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

    private companion object {
        val COST_RANGE = 1..100_000
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

    fun parseCount(input: String, range: IntRange): Result<Int> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return Result.failure(FieldException(FieldError.REQUIRED))
        val value = trimmed.toIntOrNull() ?: return Result.failure(FieldException(FieldError.INVALID_NUMBER))
        return if (value in range) Result.success(value) else Result.failure(FieldException(FieldError.OUT_OF_RANGE))
    }
}
