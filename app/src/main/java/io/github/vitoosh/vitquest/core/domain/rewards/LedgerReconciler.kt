package io.github.vitoosh.vitquest.core.domain.rewards

import io.github.vitoosh.vitquest.core.model.RewardCurrency
import io.github.vitoosh.vitquest.core.model.RewardReason
import java.time.LocalDate

/** A ledger line as the reconciler sees it. [sourceKey] is unique per currency. */
data class LedgerLine(
    val currency: RewardCurrency,
    val amount: Int,
    val sourceKey: String,
    val reason: RewardReason,
    val date: LocalDate,
)

/**
 * Keeps the ledger in step with the records without ever editing a line.
 *
 * Each earning event gets grant lines `g:<key>#n`. If its activity disappears
 * (for example a workout is deleted) a reversal `r:<key>#n` is appended; if
 * it comes back, grant `#n+1` follows. Running this twice adds nothing the
 * second time, and the unique source keys make concurrent runs safe.
 */
object LedgerReconciler {
    private const val GRANT = "g:"
    private const val REVERSAL = "r:"

    fun isReversal(sourceKey: String): Boolean = sourceKey.startsWith(REVERSAL)

    fun reconcile(events: List<RewardEvent>, existing: List<LedgerLine>, windowStart: LocalDate): List<LedgerLine> {
        val added = mutableListOf<LedgerLine>()
        RewardCurrency.entries.forEach { currency ->
            val wanted = events.mapNotNull { event ->
                val amount = if (currency == RewardCurrency.XP) event.rule.xp else event.rule.credits
                if (amount > 0) event.key to (event to amount) else null
            }.toMap()
            val lines = existing.filter { it.currency == currency }
            val grants = lines.filter { it.sourceKey.startsWith(GRANT) }.groupBy { logicalKey(it.sourceKey) }
            val reversals = lines.filter { it.sourceKey.startsWith(REVERSAL) }.groupBy { logicalKey(it.sourceKey) }

            (wanted.keys + grants.keys).forEach { key ->
                val keyGrants = grants[key].orEmpty().sortedBy { sequence(it.sourceKey) }
                val active = keyGrants.size - reversals[key].orEmpty().size
                val event = wanted[key]
                when {
                    event != null && active <= 0 -> added += LedgerLine(
                        currency = currency,
                        amount = event.second,
                        sourceKey = "$GRANT$key#${keyGrants.size + 1}",
                        reason = event.first.rule.reason,
                        date = event.first.date,
                    )
                    event == null && active >= 1 -> {
                        val last = keyGrants.last()
                        // Old activity is left alone so data that ages out never lowers a balance.
                        if (key.startsWith("achievement:") || !last.date.isBefore(windowStart)) {
                            added += LedgerLine(currency, -last.amount, "$REVERSAL$key#${keyGrants.size}", last.reason, last.date)
                        }
                    }
                }
            }
        }
        return added
    }

    private fun logicalKey(sourceKey: String): String = sourceKey.drop(GRANT.length).substringBeforeLast('#')

    private fun sequence(sourceKey: String): Int = sourceKey.substringAfterLast('#').toIntOrNull() ?: 0
}
