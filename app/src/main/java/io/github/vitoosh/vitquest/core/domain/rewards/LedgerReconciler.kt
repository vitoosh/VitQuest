package io.github.vitoosh.vitquest.core.domain.rewards

import io.github.vitoosh.vitquest.core.model.RewardReason
import java.time.LocalDate

/** An XP ledger line as the reconciler sees it. [sourceKey] is unique. */
data class LedgerLine(
    val amount: Int,
    val sourceKey: String,
    val reason: RewardReason,
    val date: LocalDate,
)

/**
 * Keeps the XP ledger in step with the records without ever editing a line.
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
        val wanted = events.filter { it.rule.xp > 0 }.associateBy { it.key }
        val grants = existing.filter { it.sourceKey.startsWith(GRANT) }.groupBy { logicalKey(it.sourceKey) }
        val reversals = existing.filter { it.sourceKey.startsWith(REVERSAL) }.groupBy { logicalKey(it.sourceKey) }
        val added = mutableListOf<LedgerLine>()
        (wanted.keys + grants.keys).forEach { key ->
            val keyGrants = grants[key].orEmpty().sortedBy { sequence(it.sourceKey) }
            val active = keyGrants.size - reversals[key].orEmpty().size
            val event = wanted[key]
            when {
                event != null && active <= 0 ->
                    added += LedgerLine(event.rule.xp, "$GRANT$key#${keyGrants.size + 1}", event.rule.reason, event.date)
                event == null && active >= 1 -> {
                    val last = keyGrants.last()
                    // Old activity is left alone so data that ages out never lowers the total.
                    if (key.startsWith("achievement:") || !last.date.isBefore(windowStart)) {
                        added += LedgerLine(-last.amount, "$REVERSAL$key#${keyGrants.size}", last.reason, last.date)
                    }
                }
            }
        }
        return added
    }

    private fun logicalKey(sourceKey: String): String = sourceKey.drop(GRANT.length).substringBeforeLast('#')

    private fun sequence(sourceKey: String): Int = sourceKey.substringAfterLast('#').toIntOrNull() ?: 0
}
