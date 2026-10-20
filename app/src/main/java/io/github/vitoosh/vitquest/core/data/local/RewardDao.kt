package io.github.vitoosh.vitquest.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

sealed interface ObtainOutcome {
    data class Recorded(val historyId: Long) : ObtainOutcome
    /** The same request was already saved; nothing was recorded twice. */
    data class AlreadyRecorded(val historyId: Long) : ObtainOutcome
    data object RewardMissing : ObtainOutcome
}

@Dao
abstract class RewardDao {

    // ---- XP ledger ----

    @Query("SELECT * FROM reward_transactions WHERE currency = 'XP' ORDER BY created_at_epoch_ms DESC, id DESC")
    abstract fun observeTransactions(): Flow<List<RewardTransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM reward_transactions WHERE currency = 'XP'")
    abstract fun observeXp(): Flow<Int>

    @Query("SELECT * FROM reward_transactions WHERE currency = 'XP'")
    abstract suspend fun allTransactions(): List<RewardTransactionEntity>

    /** Lines whose key already exists are skipped, which makes every write idempotent. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertTransactions(lines: List<RewardTransactionEntity>)

    /** Reads the ledger and appends what [plan] returns, all in one transaction. */
    @Transaction
    open suspend fun appendPlanned(plan: (List<RewardTransactionEntity>) -> List<RewardTransactionEntity>): Int {
        val added = plan(allTransactions())
        insertTransactions(added)
        return added.size
    }

    // ---- Rewards ----

    @Query("SELECT * FROM reward_items ORDER BY wishlist DESC, name COLLATE NOCASE")
    abstract fun observeRewards(): Flow<List<RewardEntity>>

    @Query("SELECT * FROM reward_items WHERE id = :id")
    abstract suspend fun getReward(id: Long): RewardEntity?

    @Insert
    abstract suspend fun insertReward(reward: RewardEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertRewards(rewards: List<RewardEntity>)

    @Update
    abstract suspend fun updateReward(reward: RewardEntity)

    @Query("UPDATE reward_items SET wishlist = :wishlist WHERE id = :id")
    abstract suspend fun setWishlist(id: Long, wishlist: Boolean)

    @Query("DELETE FROM reward_items WHERE id = :id")
    abstract suspend fun deleteReward(id: Long)

    // ---- Rewards obtained ----

    @Query("SELECT * FROM reward_history ORDER BY obtained_at_epoch_ms DESC, id DESC")
    abstract fun observeHistory(): Flow<List<RewardHistoryEntity>>

    @Query("SELECT * FROM reward_history WHERE request_id = :requestId")
    abstract suspend fun getHistoryByRequest(requestId: String): RewardHistoryEntity?

    @Query("SELECT * FROM reward_history WHERE id = :id")
    abstract suspend fun getHistory(id: Long): RewardHistoryEntity?

    @Insert
    abstract suspend fun insertHistory(entry: RewardHistoryEntity): Long

    @Query("DELETE FROM reward_history WHERE id = :id")
    abstract suspend fun deleteHistory(id: Long)

    /**
     * Records a reward as obtained, plus a spending entry when a price is
     * given, in one transaction. A repeated [requestId] returns the earlier
     * record instead of saving it (or the spending) twice.
     */
    @Transaction
    open suspend fun markObtained(requestId: String, rewardId: Long, pricePaidRupiah: Long?, date: String, nowEpochMs: Long): ObtainOutcome {
        getHistoryByRequest(requestId)?.let { return ObtainOutcome.AlreadyRecorded(it.id) }
        val reward = getReward(rewardId) ?: return ObtainOutcome.RewardMissing
        val budgetEntryId = pricePaidRupiah?.let { price ->
            insertBudgetEntry(
                BudgetEntryEntity(date = date, kind = SPENDING, amountRupiah = price, rewardId = reward.id, note = null, createdAtEpochMs = nowEpochMs),
            )
        }
        val id = insertHistory(
            RewardHistoryEntity(
                requestId = requestId,
                rewardId = reward.id,
                rewardName = reward.name,
                pricePaidRupiah = pricePaidRupiah,
                budgetEntryId = budgetEntryId,
                obtainedAtEpochMs = nowEpochMs,
                date = date,
            ),
        )
        return ObtainOutcome.Recorded(id)
    }

    /** Removes a record made by mistake, together with the spending entry it created. */
    @Transaction
    open suspend fun undoObtained(id: Long): Boolean {
        val entry = getHistory(id) ?: return false
        entry.budgetEntryId?.let { deleteBudgetEntry(it) }
        deleteHistory(id)
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
        const val SPENDING = "SPENDING"
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
