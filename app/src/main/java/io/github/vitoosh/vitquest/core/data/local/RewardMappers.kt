package io.github.vitoosh.vitquest.core.data.local

import io.github.vitoosh.vitquest.core.domain.rewards.LedgerLine
import io.github.vitoosh.vitquest.core.domain.rewards.LedgerReconciler
import io.github.vitoosh.vitquest.core.model.BudgetEntry
import io.github.vitoosh.vitquest.core.model.BudgetEntryKind
import io.github.vitoosh.vitquest.core.model.ObtainedReward
import io.github.vitoosh.vitquest.core.model.ReminderSetting
import io.github.vitoosh.vitquest.core.model.ReminderType
import io.github.vitoosh.vitquest.core.model.ReminderWindow
import io.github.vitoosh.vitquest.core.model.Reward
import io.github.vitoosh.vitquest.core.model.RewardReason
import io.github.vitoosh.vitquest.core.model.RewardTransaction
import io.github.vitoosh.vitquest.core.model.RewardType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

fun RewardTransactionEntity.toModel(): RewardTransaction? {
    val reason = reason.toEnumOrNull<RewardReason>() ?: return null
    return RewardTransaction(
        id = id,
        amount = amount,
        reason = reason,
        date = LocalDate.parse(date),
        createdAt = Instant.ofEpochMilli(createdAtEpochMs),
        isReversal = LedgerReconciler.isReversal(sourceKey),
    )
}

fun RewardTransactionEntity.toLine(): LedgerLine? {
    val reason = reason.toEnumOrNull<RewardReason>() ?: return null
    return LedgerLine(amount, sourceKey, reason, LocalDate.parse(date))
}

fun LedgerLine.toEntity(nowEpochMs: Long) = RewardTransactionEntity(
    amount = amount,
    reason = reason.name,
    sourceKey = sourceKey,
    date = date.toString(),
    createdAtEpochMs = nowEpochMs,
)

fun RewardEntity.toModel() = Reward(
    id = id,
    name = name,
    description = description,
    type = type.toEnumOrNull<RewardType>() ?: RewardType.PHYSICAL,
    priceRupiah = priceRupiah,
    wishlist = wishlist,
)

fun RewardHistoryEntity.toModel() = ObtainedReward(
    id = id,
    rewardId = rewardId,
    rewardName = rewardName,
    pricePaidRupiah = pricePaidRupiah,
    obtainedAt = Instant.ofEpochMilli(obtainedAtEpochMs),
    date = LocalDate.parse(date),
)

fun BudgetEntryEntity.toModel() = BudgetEntry(
    id = id,
    date = LocalDate.parse(date),
    kind = kind.toEnumOrNull<BudgetEntryKind>() ?: BudgetEntryKind.SPENDING,
    amountRupiah = amountRupiah,
    rewardId = rewardId,
    note = note,
)

private fun Int.toTime(): LocalTime = LocalTime.of(this / 60 % 24, this % 60)

private fun LocalTime.toMinutes(): Int = hour * 60 + minute

fun ReminderSettingEntity.toModel(): ReminderSetting? {
    val type = type.toEnumOrNull<ReminderType>() ?: return null
    return ReminderSetting(
        type = type,
        enabled = enabled,
        intervalMinutes = intervalMinutes ?: type.defaultIntervalMinutes,
        time = timeMinutes?.toTime(),
        days = days.toEnumSet<DayOfWeek>(),
    )
}

fun ReminderSetting.toEntity() = ReminderSettingEntity(
    type = type.name,
    enabled = enabled,
    intervalMinutes = intervalMinutes,
    timeMinutes = time?.toMinutes(),
    days = days.sortedBy { it.ordinal }.joinToString(",") { it.name },
)

fun ReminderWindowEntity?.toModel() = ReminderWindow(
    activeStart = this?.activeStart?.toTime(),
    activeEnd = this?.activeEnd?.toTime(),
    quietStart = this?.quietStart?.toTime(),
    quietEnd = this?.quietEnd?.toTime(),
)

fun ReminderWindow.toEntity() = ReminderWindowEntity(
    activeStart = activeStart?.toMinutes(),
    activeEnd = activeEnd?.toMinutes(),
    quietStart = quietStart?.toMinutes(),
    quietEnd = quietEnd?.toMinutes(),
)
