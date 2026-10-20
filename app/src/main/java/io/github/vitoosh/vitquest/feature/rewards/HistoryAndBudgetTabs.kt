package io.github.vitoosh.vitquest.feature.rewards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.DataSourceKind
import io.github.vitoosh.vitquest.core.designsystem.component.DataSourceTag
import io.github.vitoosh.vitquest.core.designsystem.component.SectionHeader
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.model.BudgetEntryKind
import io.github.vitoosh.vitquest.core.model.ObtainedReward
import io.github.vitoosh.vitquest.core.ui.dayLabel

private const val LEDGER_LIMIT = 100

@Composable
fun RewardHistoryTab(state: RewardsUiState, actions: RewardActions) {
    var undoing by rememberSaveable { mutableStateOf<Long?>(null) }
    CenteredContent {
        LazyColumn(contentPadding = PaddingValues(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
            item { SectionHeader(stringResource(R.string.rewards_obtained_title)) }
            if (state.history.isEmpty()) {
                item { Text(stringResource(R.string.rewards_no_obtained), style = MaterialTheme.typography.bodyMedium) }
            }
            items(state.history, key = { "o-${it.id}" }) { entry ->
                ObtainedRow(entry, state, onUndo = { undoing = entry.id })
            }
            item { SectionHeader(stringResource(R.string.rewards_ledger), modifier = Modifier.padding(top = VqSpacing.md)) }
            item {
                Text(
                    text = stringResource(R.string.rewards_ledger_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.transactions.isEmpty()) {
                item { Text(stringResource(R.string.rewards_no_transactions), style = MaterialTheme.typography.bodyMedium) }
            }
            items(state.transactions.take(LEDGER_LIMIT), key = { "t-${it.id}" }) { TransactionRow(it, state.today) }
        }
    }
    undoing?.let { id ->
        val entry = state.history.firstOrNull { it.id == id }
        AlertDialog(
            onDismissRequest = { undoing = null },
            title = { Text(stringResource(R.string.rewards_undo_title)) },
            text = { Text(stringResource(R.string.rewards_undo_message)) },
            confirmButton = {
                TextButton(onClick = {
                    entry?.let(actions.undoObtained)
                    undoing = null
                }) { Text(stringResource(R.string.rewards_undo_obtained)) }
            },
            dismissButton = { TextButton(onClick = { undoing = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun ObtainedRow(entry: ObtainedReward, state: RewardsUiState, onUndo: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.padding(VqSpacing.md), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VqSpacing.xs)) {
                Text(entry.rewardName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = listOfNotNull(
                        dayLabel(entry.date, state.today),
                        entry.pricePaidRupiah?.let { stringResource(R.string.rewards_paid, rupiahText(it)) },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onUndo) { Text(stringResource(R.string.rewards_undo_obtained)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetTab(state: RewardsUiState, actions: RewardActions) {
    var editingBudget by rememberSaveable { mutableStateOf(false) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val priced = state.rewards.filter { it.wishlist && it.priceRupiah != null }
    CenteredContent {
        LazyColumn(contentPadding = PaddingValues(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Text(
                        text = stringResource(R.string.budget_separate_note),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(VqSpacing.lg),
                    )
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.budget_month_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            DataSourceTag(DataSourceKind.MANUAL)
                        }
                        val budget = state.monthlyBudget
                        if (budget == null) {
                            Text(stringResource(R.string.budget_not_set), style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text(stringResource(R.string.budget_spent_of, rupiahText(state.spentThisMonth), rupiahText(budget)), style = MaterialTheme.typography.headlineSmall)
                            LinearProgressIndicator(progress = { (state.spentThisMonth.toFloat() / budget).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                            val left = budget - state.spentThisMonth
                            Text(
                                text = if (left >= 0) stringResource(R.string.budget_left, rupiahText(left)) else stringResource(R.string.budget_over, rupiahText(-left)),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        OutlinedButton(onClick = { editingBudget = true }) { Text(stringResource(R.string.budget_set)) }
                    }
                }
            }
            item { SectionHeader(stringResource(R.string.budget_savings_title)) }
            if (priced.isEmpty()) {
                item { Text(stringResource(R.string.budget_savings_empty), style = MaterialTheme.typography.bodyMedium) }
            }
            items(priced, key = { "s-${it.id}" }) { reward ->
                val price = requireNotNull(reward.priceRupiah)
                val saved = state.savedFor(reward.id)
                Column(verticalArrangement = Arrangement.spacedBy(VqSpacing.xs)) {
                    Text(reward.name, style = MaterialTheme.typography.titleSmall)
                    LinearProgressIndicator(progress = { (saved.toFloat() / price).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Text(savingsText(saved, price), style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                FilledTonalButton(onClick = { adding = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(stringResource(R.string.budget_add_entry), modifier = Modifier.padding(start = VqSpacing.xs))
                }
            }
            item { SectionHeader(stringResource(R.string.budget_entries)) }
            if (state.budgetEntries.isEmpty()) {
                item { Text(stringResource(R.string.budget_no_entries), style = MaterialTheme.typography.bodyMedium) }
            }
            items(state.budgetEntries, key = { "b-${it.id}" }) { entry ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(if (entry.kind == BudgetEntryKind.SAVING) R.string.budget_kind_saving else R.string.budget_kind_spending) + " · " + rupiahText(entry.amountRupiah),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        val reward = entry.rewardId?.let { id -> state.rewards.firstOrNull { it.id == id }?.name }
                        Text(
                            text = listOfNotNull(dayLabel(entry.date, state.today), reward, entry.note).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { actions.deleteBudgetEntry(entry) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                }
            }
        }
    }
    if (editingBudget) {
        var input by rememberSaveable { mutableStateOf(state.monthlyBudget?.toString().orEmpty()) }
        var error by rememberSaveable { mutableStateOf<FieldError?>(null) }
        AlertDialog(
            onDismissRequest = { editingBudget = false },
            title = { Text(stringResource(R.string.budget_month_title)) },
            text = { FormField(input, { input = it.take(15) }, stringResource(R.string.budget_amount), error, KeyboardType.Number, stringResource(R.string.budget_clear_help)) },
            confirmButton = {
                TextButton(onClick = {
                    error = actions.setMonthlyBudget(input)
                    if (error == null) editingBudget = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { editingBudget = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (adding) {
        var kind by rememberSaveable { mutableStateOf(BudgetEntryKind.SAVING) }
        var amount by rememberSaveable { mutableStateOf("") }
        var rewardId by rememberSaveable { mutableStateOf<Long?>(null) }
        var note by rememberSaveable { mutableStateOf("") }
        var error by rememberSaveable { mutableStateOf<FieldError?>(null) }
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text(stringResource(R.string.budget_add_entry)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                        BudgetEntryKind.entries.forEach { option ->
                            FilterChip(
                                selected = kind == option,
                                onClick = { kind = option },
                                label = { Text(stringResource(if (option == BudgetEntryKind.SAVING) R.string.budget_kind_saving else R.string.budget_kind_spending)) },
                            )
                        }
                    }
                    FormField(amount, { amount = it.take(15) }, stringResource(R.string.budget_amount), error, KeyboardType.Number)
                    if (state.rewards.isNotEmpty()) {
                        Text(stringResource(R.string.budget_for_reward), style = MaterialTheme.typography.titleSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                            state.rewards.sortedByDescending { it.wishlist }.take(MAX_REWARD_CHIPS).forEach { reward ->
                                FilterChip(
                                    selected = rewardId == reward.id,
                                    onClick = { rewardId = if (rewardId == reward.id) null else reward.id },
                                    label = { Text(reward.name) },
                                )
                            }
                        }
                    }
                    FormField(note, { note = it.take(60) }, stringResource(R.string.budget_note), null)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    error = actions.addBudgetEntry(kind, amount, rewardId, note)
                    if (error == null) adding = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private const val MAX_REWARD_CHIPS = 8
