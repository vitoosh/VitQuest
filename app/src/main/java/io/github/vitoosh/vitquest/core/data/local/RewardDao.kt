package io.github.vitoosh.vitquest.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

sealed interface RedeemOutcome {
    data class Redeemed(val redemptionId: Long) : RedeemOutcome
    /** The same request was already processed; nothing was charged again. */
    data class AlreadyRedeemed(val redemptionId: Long) : RedeemOutcome
    data class NotEnoughCredits(val balance: Int, val cost: Int) : RedeemOutcome
    data object RewardMissing : RedeemOutcome
}

@Dao
abstract class RewardDao {

    // ---- Ledger ----

    @Query("SELECT * FROM reward_transactions ORDER BY created_at_epoch_ms DESC, id DESC")
    abstract fun observeTransactions(): Flow<List<RewardTransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM reward_transactions WHERE currency = :currency")
    abstract fun observeBalance(currency: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM reward_transactions WHERE currency = :currency")
    abstract suspend fun balance(currency: String): Int

    @Query("SELECT * FROM reward_transactions")
    abstract suspend fun allTransactions(): List<RewardTransactionEntity>

    /** Lines whose key already exists are skipped, which makes every write idempotent. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertTransactions(lines: List<RewardTransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertTransaction(line: RewardTransactionEntity): Long

    /** Reads the ledger and appends what [plan] returns, all in one transaction. */
    @Transaction
    open suspend fun appendPlanned(plan: (List<RewardTransactionEntity>) -> List<RewardTransactionEntity>): Int {
        val added = plan(allTransactions())
        insertTransactions(added)
        return added.size
    }

    // ---- Catalog ----

    @Query("SELECT * FROM rewards ORDER BY wishlist DESC, credit_cost, name COLLATE NOCASE")
    abstract fun observeRewards(): Flow<List<RewardEntity>>

    @Query("SELECT * FROM rewards WHERE id = :id")
    abstract suspend fun getReward(id: Long): RewardEntity?

    @Query("SELECT COUNT(*) FROM rewards")
    abstract suspend fun rewardCount(): Int

    @Insert
    abstract suspend fun insertReward(reward: RewardEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertRewards(rewards: List<RewardEntity>)

    @Update
    abstract suspend fun updateReward(reward: RewardEntity)

    @Query("UPDATE rewards SET wishlist = :wishlist WHERE id = :id")
    abstract suspend fun setWishlist(id: Long, wishlist: Boolean)

    @Query("DELETE FROM rewards WHERE id = :id")
    abstract suspend fun deleteReward(id: Long)

    // ---- Redemptions ----

    @Query("SELECT * FROM redemptions ORDER BY redeemed_at_epoch_ms DESC, id DESC")
    abstract fun observeRedemptions(): Flow<List<RedemptionEntity>>

    @Query("SELECT * FROM redemptions WHERE request_id = :requestId")
    abstract suspend fun getRedemptionByRequest(requestId: String): RedemptionEntity?

    @Query("SELECT * FROM redemptions WHERE id = :id")
    abstract suspend fun getRedemption(id: Long): RedemptionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertRedemption(redemption: RedemptionEntity): Long

    @Query("UPDATE redemptions SET status = :status WHERE id = :id")
    abstract suspend fun setRedemptionStatus(id: Long, status: String)

    /**
     * Spends credits atomically: the balance check, the redemption row and
     * the ledger line succeed or fail together, and a repeated [requestId]
     * returns the earlier result instead of charging twice.
     */
    @Transaction
    open suspend fun redeem(requestId: String, rewardId: Long, date: String, nowEpochMs: Long): RedeemOutcome {
        getRedemptionByRequest(requestId)?.let { return RedeemOutcome.AlreadyRedeemed(it.id) }
        val reward = getReward(rewardId) ?: return RedeemOutcome.RewardMissing
        val balance = balance(CREDIT)
        if (balance < reward.creditCost) return RedeemOutcome.NotEnoughCredits(balance, reward.creditCost)
        val id = insertRedemption(
            RedemptionEntity(
                requestId = requestId,
                rewardId = reward.id,
                rewardName = reward.name,
                creditCost = reward.creditCost,
                redeemedAtEpochMs = nowEpochMs,
                date = date,
                status = STATUS_REDEEMED,
            ),
        )
        insertTransaction(
            RewardTransactionEntity(
                currency = CREDIT,
                amount = -reward.creditCost,
                reason = REASON_REDEMPTION,
                sourceKey = "redeem:$requestId",
                date = date,
                createdAtEpochMs = nowEpochMs,
            ),
        )
        return RedeemOutcome.Redeemed(id)
    }

    /** Refunds a redemption once; cancelling again does nothing. */
    @Transaction
    open suspend fun cancelRedemption(id: Long, date: String, nowEpochMs: Long): Boolean {
        val redemption = getRedemption(id)?.takeIf { it.status == STATUS_REDEEMED } ?: return false
        setRedemptionStatus(id, STATUS_CANCELLED)
        insertTransaction(
            RewardTransactionEntity(
                currency = CREDIT,
                amount = redemption.creditCost,
                reason = REASON_REDEMPTION_CANCELLED,
                sourceKey = "redeem-cancel:${redemption.requestId}",
                date = date,
                createdAtEpochMs = nowEpochMs,
            ),
        )
        return true
    }

    // ---- Real-money budget ----

    @Query("SELECT * FROM budget_entries ORDER BY date DESC, created_at_epoch_ms DESC")
    abstract fun observeBudgetEntries(): Flow<List<BudgetEntryEntity>>

    @Insert
    abstract suspend fun insertBudgetEntry(entry: BudgetEntryEntity): Long

    @Query("DELETE FROM budget_entries WHERE id = :id")
    abstract suspend fun deleteBudgetEntry(id: Long)

    companion object {
        const val CREDIT = "CREDIT"
        const val STATUS_REDEEMED = "REDEEMED"
        const val STATUS_CANCELLED = "CANCELLED"
        const val REASON_REDEMPTION = "REDEMPTION"
        const val REASON_REDEMPTION_CANCELLED = "REDEMPTION_CANCELLED"
    }
}

@Dao
abstract class ReminderDao {

    @Query("SELECT * FROM reminder_settings")
    abstract fun observeSettings(): Flow<List<ReminderSettingEntity>>

    @Query("SELECT * FROM reminder_settings")
    abstract suspend fun getSettings(): List<ReminderSettingEntity>

    @Upsert
    abstract suspend fun upsertSetting(setting: ReminderSettingEntity)

    @Query("SELECT * FROM reminder_window WHERE id = 1")
    abstract fun observeWindow(): Flow<ReminderWindowEntity?>

    @Query("SELECT * FROM reminder_window WHERE id = 1")
    abstract suspend fun getWindow(): ReminderWindowEntity?

    @Upsert
    abstract suspend fun upsertWindow(window: ReminderWindowEntity)
}
