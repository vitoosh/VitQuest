package io.github.vitoosh.vitquest.core.model

import java.time.Instant
import java.time.LocalDate

/** Why an XP ledger entry exists. Shown in the history. */
enum class RewardReason {
    WORKOUT,
    STEP_GOAL,
    WATER_GOAL,
    FOOD_LOG,
    WEIGHT_LOG,
    HABIT_CHECK_IN,
    ACHIEVEMENT,
}

/** One immutable line of the XP ledger. Corrections are new lines, never edits. */
data class RewardTransaction(
    val id: Long,
    val amount: Int,
    val reason: RewardReason,
    /** The day the earning activity happened. */
    val date: LocalDate,
    val createdAt: Instant,
    /** True for an entry that takes back an earlier one because its activity was deleted. */
    val isReversal: Boolean,
)

enum class RewardType { PHYSICAL, EXPERIENCE }

/** Something real the user wants to treat themselves to, optionally with a price to save for. */
data class Reward(
    val id: Long,
    val name: String,
    val description: String?,
    val type: RewardType,
    /** Real-world price in rupiah, used for the savings progress. */
    val priceRupiah: Long?,
    val wishlist: Boolean,
)

/** A reward the user actually bought or did. */
data class ObtainedReward(
    val id: Long,
    val rewardId: Long?,
    val rewardName: String,
    val pricePaidRupiah: Long?,
    val obtainedAt: Instant,
    val date: LocalDate,
)

/** Real money the user saved or spent on rewards. */
enum class BudgetEntryKind { SAVING, SPENDING }

data class BudgetEntry(
    val id: Long,
    val date: LocalDate,
    val kind: BudgetEntryKind,
    val amountRupiah: Long,
    val rewardId: Long?,
    val note: String?,
)
