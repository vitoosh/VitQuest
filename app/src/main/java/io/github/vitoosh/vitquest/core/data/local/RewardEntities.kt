package io.github.vitoosh.vitquest.core.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The XP and Reward Credit ledger. Lines are only ever inserted; a unique
 * (currency, source_key) pair makes every grant, reversal and redemption
 * idempotent.
 */
@Entity(
    tableName = "reward_transactions",
    indices = [Index(value = ["currency", "source_key"], unique = true), Index("date")],
)
data class RewardTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val currency: String,
    val amount: Int,
    val reason: String,
    @ColumnInfo(name = "source_key") val sourceKey: String,
    val date: String,
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long,
)

/** Built-in suggestions have a seed key; rewards the user adds have none. */
@Entity(tableName = "rewards", indices = [Index(value = ["seed_key"], unique = true)])
data class RewardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "seed_key") val seedKey: String?,
    val name: String,
    val description: String?,
    val type: String,
    @ColumnInfo(name = "credit_cost") val creditCost: Int,
    @ColumnInfo(name = "price_rupiah") val priceRupiah: Long?,
    val wishlist: Boolean,
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long,
)

/** [requestId] is unique, so a repeated tap or retry can never redeem twice. */
@Entity(
    tableName = "redemptions",
    foreignKeys = [ForeignKey(entity = RewardEntity::class, parentColumns = ["id"], childColumns = ["reward_id"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index(value = ["request_id"], unique = true), Index("reward_id")],
)
data class RedemptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "request_id") val requestId: String,
    @ColumnInfo(name = "reward_id") val rewardId: Long?,
    @ColumnInfo(name = "reward_name") val rewardName: String,
    @ColumnInfo(name = "credit_cost") val creditCost: Int,
    @ColumnInfo(name = "redeemed_at_epoch_ms") val redeemedAtEpochMs: Long,
    val date: String,
    val status: String,
)

/** Real money in rupiah. Never mixed with Reward Credits. */
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
