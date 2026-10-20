package io.github.vitoosh.vitquest.core.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The XP ledger. Lines are only ever inserted; a unique (currency, source_key)
 * pair makes every grant and reversal idempotent. `currency` is always "XP"
 * since Reward Credits were removed in database version 6.
 */
@Entity(
    tableName = "reward_transactions",
    indices = [Index(value = ["currency", "source_key"], unique = true), Index("date")],
)
data class RewardTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val currency: String = XP,
    val amount: Int,
    val reason: String,
    @ColumnInfo(name = "source_key") val sourceKey: String,
    val date: String,
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long,
) {
    companion object {
        const val XP = "XP"
    }
}

/** Real rewards to save for. Built-in suggestions have a seed key; the user's own have none. */
@Entity(tableName = "reward_items", indices = [Index(value = ["seed_key"], unique = true)])
data class RewardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "seed_key") val seedKey: String?,
    val name: String,
    val description: String?,
    val type: String,
    @ColumnInfo(name = "price_rupiah") val priceRupiah: Long?,
    val wishlist: Boolean,
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long,
)

/**
 * Rewards the user actually got. [requestId] is unique, so a repeated tap
 * records it once; [budgetEntryId] is the spending entry created with it.
 */
@Entity(
    tableName = "reward_history",
    foreignKeys = [ForeignKey(entity = RewardEntity::class, parentColumns = ["id"], childColumns = ["reward_id"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index(value = ["request_id"], unique = true), Index("reward_id")],
)
data class RewardHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "request_id") val requestId: String,
    @ColumnInfo(name = "reward_id") val rewardId: Long?,
    @ColumnInfo(name = "reward_name") val rewardName: String,
    @ColumnInfo(name = "price_paid_rupiah") val pricePaidRupiah: Long?,
    @ColumnInfo(name = "budget_entry_id") val budgetEntryId: Long?,
    @ColumnInfo(name = "obtained_at_epoch_ms") val obtainedAtEpochMs: Long,
    val date: String,
)

/** Real money in rupiah, entered by the user. */
@Entity(
    tableName = "budget_entries",
    foreignKeys = [ForeignKey(entity = RewardEntity::class, parentColumns = ["id"], childColumns = ["reward_id"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("date"), Index("reward_id")],
)
data class BudgetEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val kind: String,
    @ColumnInfo(name = "amount_rupiah") val amountRupiah: Long,
    @ColumnInfo(name = "reward_id") val rewardId: Long?,
    val note: String?,
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long,
)

@Entity(tableName = "reminder_settings")
data class ReminderSettingEntity(
    @PrimaryKey val type: String,
    val enabled: Boolean,
    @ColumnInfo(name = "interval_minutes") val intervalMinutes: Int?,
    /** Minutes after midnight. */
    @ColumnInfo(name = "time_minutes") val timeMinutes: Int?,
    /** Comma-separated DayOfWeek names. */
    val days: String,
)

/** Single row holding active and quiet hours as minutes after midnight. */
@Entity(tableName = "reminder_window")
data class ReminderWindowEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "active_start") val activeStart: Int?,
    @ColumnInfo(name = "active_end") val activeEnd: Int?,
    @ColumnInfo(name = "quiet_start") val quietStart: Int?,
    @ColumnInfo(name = "quiet_end") val quietEnd: Int?,
)
