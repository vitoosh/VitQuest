package io.github.vitoosh.vitquest.feature.rewards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.IconBubble
import io.github.vitoosh.vitquest.core.designsystem.component.SectionHeader
import io.github.vitoosh.vitquest.core.designsystem.icon.VqIcons
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.domain.rewards.LevelProgress
import io.github.vitoosh.vitquest.core.domain.rewards.RewardRule
import io.github.vitoosh.vitquest.core.model.RewardCurrency
import io.github.vitoosh.vitquest.core.model.RewardTransaction
import io.github.vitoosh.vitquest.core.ui.dayLabel
import io.github.vitoosh.vitquest.core.ui.formatInteger
import java.time.LocalDate

private const val RECENT_COUNT = 8

@Composable
fun OverviewTab(state: RewardsUiState, onOpenCatalog: () -> Unit) {
    CenteredContent {
        LazyColumn(contentPadding = PaddingValues(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
            item { LevelCard(state.level) }
            item { CreditsCard(state.credits, onOpenCatalog) }
            item { SectionHeader(stringResource(R.string.rewards_rules_title)) }
            item { RulesCard() }
            item { SectionHeader(stringResource(R.string.rewards_recent)) }
            if (state.transactions.isEmpty()) {
                item { Text(stringResource(R.string.rewards_no_transactions), style = MaterialTheme.typography.bodyMedium) }
            }
            items(state.transactions.take(RECENT_COUNT), key = { it.id }) { transaction -> TransactionRow(transaction, state.today) }
        }
    }
}

@Composable
fun LevelCard(level: LevelProgress, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = modifier) {
        Column(Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
                IconBubble(icon = VqIcons.Trophy, size = 40)
                Column {
                    Text(stringResource(R.string.rewards_level, formatInteger(level.level)), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.rewards_total_xp, xpText(level.totalXp)), style = MaterialTheme.typography.bodyMedium)
                }
            }
            LinearProgressIndicator(progress = { level.fraction }, modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(
                    R.string.rewards_level_progress,
                    formatInteger(level.xpIntoLevel),
                    formatInteger(level.xpForNext),
                    formatInteger(level.level + 1),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CreditsCard(credits: Int, onOpenCatalog: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
            Text(stringResource(R.string.rewards_credit_balance), style = MaterialTheme.typography.titleMedium)
            Text(creditsText(credits), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.rewards_credits_not_money), style = MaterialTheme.typography.bodySmall)
            if (credits < 0) Text(stringResource(R.string.rewards_negative_balance), style = MaterialTheme.typography.bodySmall)
            Button(onClick = onOpenCatalog) { Text(stringResource(R.string.rewards_open_catalog)) }
        }
    }
}

@Composable
private fun RulesCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
            RewardRule.entries.forEach { rule ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(rule.reason.labelRes), style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(rule.limitRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(ruleRewardText(rule), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = stringResource(R.string.rewards_rules_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun TransactionRow(transaction: RewardTransaction, today: LocalDate) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(transaction.reason.labelRes), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = dayLabel(transaction.date, today) + if (transaction.isReversal) " · " + stringResource(R.string.rewards_correction) else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val sign = if (transaction.amount > 0) "+" else "−"
        val magnitude = kotlin.math.abs(transaction.amount)
        Text(
            text = sign + if (transaction.currency == RewardCurrency.XP) xpText(magnitude) else creditsText(magnitude),
            style = MaterialTheme.typography.titleSmall,
            color = if (transaction.amount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
