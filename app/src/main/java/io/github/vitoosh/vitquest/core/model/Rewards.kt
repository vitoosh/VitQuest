package io.github.vitoosh.vitquest.core.model

import java.time.Instant
import java.time.LocalDate

/**
 * The two virtual currencies. XP only grows (levels); Reward Credits are earned
 * and spent in the in-app catalog. Neither is money nor exchangeable for cash.
 */
enum class RewardCurrency { XP, CREDIT }

/** Why a ledger entry exists. Shown in the transaction history. */
enum class RewardReason {
    WORKOUT,
    STEP_GOAL,
    WATER_GOAL,
    FOOD_LOG,
    WEIGHT_LOG,
    HABIT_CHECK_IN,
    ACHIEVEMENT,
    REDEMPTION,
    REDEMPTION_CANCELLED,
}

/** One immutable line of the ledger. Corrections are new lines, never edits. */
data class RewardTransaction(
    val id: Long,
    val currency: RewardCurrency,
    val amount: Int,
    val reason: RewardReason,
    /** The day the earning activity happened (or the redemption day). */
    val date: LocalDate,
    val createdAt: Instant,
    /** True for an entry that takes back an earlier one because its activity was deleted. */
    val isReversal: Boolean,
)

enum class RewardType { PHYSICAL, EXPERIENCE }

data class Reward(
    val id: Long,
    val name: String,
    val description: String?,
    val type: RewardType,
    val creditCost: Int,
    /** Optional real-world price in rupiah, only used for the separate savings tracker. */
    val priceRupiah: Long?,
    val wishlist: Boolean,
)

enum class RedemptionStatus { REDEEMED, CANCELLED }

data class Redemption(
    val id: Long,
    val rewardId: Long?,
    val rewardName: String,
    val creditCost: Int,
    val redeemedAt: Instant,
    val date: LocalDate,
    val status: RedemptionStatus,
)

/** Real money, kept apart from Reward Credits. */
enum class BudgetEntryKind { SAVING, SPENDING }

data class BudgetEntry(
    val id: Long,
    val date: LocalDate,
    val kind: BudgetEntryKind,
    val amountRupiah: Long,
    val rewardId: Long?,
    val note: String?,
)
