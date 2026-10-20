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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import io.github.vitoosh.vitquest.core.designsystem.component.SectionHeader
import io.github.vitoosh.vitquest.core.designsystem.component.VqBadge
import io.github.vitoosh.vitquest.core.designsystem.icon.VqIcons
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.model.Reward
import io.github.vitoosh.vitquest.core.model.RewardType
import io.github.vitoosh.vitquest.core.ui.formatInteger
import io.github.vitoosh.vitquest.core.ui.messageRes
import java.util.UUID

private const val NEW_REWARD = -1L

@Composable
fun CatalogTab(state: RewardsUiState, actions: RewardActions) {
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }
    var redeeming by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleting by rememberSaveable { mutableStateOf<Long?>(null) }
    val wishlist = state.rewards.filter { it.wishlist }
    val others = state.rewards.filterNot { it.wishlist }
    CenteredContent {
        LazyColumn(contentPadding = PaddingValues(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
            item {
                Text(
                    text = stringResource(R.string.rewards_catalog_intro, creditsText(state.credits)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            item {
                FilledTonalButton(onClick = { editing = NEW_REWARD }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(stringResource(R.string.rewards_add), modifier = Modifier.padding(start = VqSpacing.xs))
                }
            }
            if (state.rewards.isEmpty()) {
                item { Text(stringResource(R.string.rewards_catalog_empty), style = MaterialTheme.typography.bodyMedium) }
            }
            if (wishlist.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.rewards_wishlist)) }
                items(wishlist, key = { "w-${it.id}" }) { reward ->
                    RewardCard(reward, state.credits, onRedeem = { redeeming = reward.id }, onEdit = { editing = reward.id }, onDelete = { deleting = reward.id }, onWishlist = { actions.setWishlist(reward, it) })
                }
            }
            RewardType.entries.forEach { type ->
                val ofType = others.filter { it.type == type }
                if (ofType.isEmpty()) return@forEach
                item(key = "h-$type") { SectionHeader(stringResource(type.labelRes)) }
                items(ofType, key = { it.id }) { reward ->
                    RewardCard(reward, state.credits, onRedeem = { redeeming = reward.id }, onEdit = { editing = reward.id }, onDelete = { deleting = reward.id }, onWishlist = { actions.setWishlist(reward, it) })
                }
            }
            item {
                Text(
                    text = stringResource(R.string.rewards_catalog_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    editing?.let { id ->
        val existing = state.rewards.firstOrNull { it.id == id }
        RewardDialog(existing, onSave = { form -> actions.saveReward(existing?.id, form).also { if (it.isEmpty) editing = null } }, onDismiss = { editing = null })
    }
    redeeming?.let { id ->
        state.rewards.firstOrNull { it.id == id }?.let { reward ->
            RedeemDialog(reward, state.credits, onConfirm = { requestId ->
                actions.redeem(requestId, reward)
                redeeming = null
            }, onDismiss = { redeeming = null })
        }
    }
    deleting?.let { id ->
        val reward = state.rewards.firstOrNull { it.id == id }
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.rewards_delete_title)) },
            text = { Text(stringResource(R.string.rewards_delete_message, reward?.name.orEmpty())) },
            confirmButton = {
                TextButton(onClick = {
                    reward?.let(actions.deleteReward)
                    deleting = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RewardCard(
    reward: Reward,
    balance: Int,
    onRedeem: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onWishlist: (Boolean) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(VqSpacing.md), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VqSpacing.xs)) {
                    Text(reward.name, style = MaterialTheme.typography.titleMedium)
                    reward.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                IconButton(onClick = { onWishlist(!reward.wishlist) }) {
                    Icon(
                        imageVector = if (reward.wishlist) Icons.Filled.Star else VqIcons.StarBorder,
                        contentDescription = stringResource(if (reward.wishlist) R.string.rewards_wishlist_remove else R.string.rewards_wishlist_add),
                        tint = if (reward.wishlist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm), verticalArrangement = Arrangement.spacedBy(VqSpacing.xs)) {
                VqBadge(creditsText(reward.creditCost), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                VqBadge(stringResource(reward.type.labelRes), MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurface)
                reward.priceRupiah?.let {
                    VqBadge(stringResource(R.string.rewards_price_estimate, rupiahText(it)), MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
            val enough = balance >= reward.creditCost
            if (!enough) {
                LinearProgressIndicator(progress = { (balance.coerceAtLeast(0).toFloat() / reward.creditCost).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Text(
                    text = stringResource(R.string.rewards_credits_to_go, creditsText(reward.creditCost - balance.coerceAtLeast(0))),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onRedeem, enabled = enough) { Text(stringResource(R.string.rewards_redeem)) }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.rewards_edit)) }
                    IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete)) }
                }
            }
        }
    }
}

@Composable
private fun RedeemDialog(reward: Reward, balance: Int, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    // One id per confirmation: tapping twice, or a retry after a crash, cannot redeem twice.
    val requestId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rewards_redeem_title, reward.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                Text(stringResource(R.string.rewards_redeem_message, creditsText(reward.creditCost), creditsText(balance - reward.creditCost)))
                Text(stringResource(R.string.rewards_redeem_note), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(requestId) }) { Text(stringResource(R.string.rewards_redeem)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RewardDialog(existing: Reward?, onSave: (RewardForm) -> RewardFormErrors, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(existing?.description.orEmpty()) }
    var type by rememberSaveable { mutableStateOf(existing?.type ?: RewardType.PHYSICAL) }
    var cost by rememberSaveable { mutableStateOf(existing?.creditCost?.toString().orEmpty()) }
    var price by rememberSaveable { mutableStateOf(existing?.priceRupiah?.toString().orEmpty()) }
    var errors by rememberSaveable(stateSaver = RewardFormErrorsSaver) { mutableStateOf(RewardFormErrors()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.rewards_add else R.string.rewards_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                FormField(name, { name = it.take(60) }, stringResource(R.string.rewards_name), errors.name)
                FormField(description, { description = it.take(120) }, stringResource(R.string.rewards_description), null)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                    RewardType.entries.forEach { option ->
                        FilterChip(selected = type == option, onClick = { type = option }, label = { Text(stringResource(option.labelRes)) })
                    }
                }
                FormField(cost, { cost = it.filter(Char::isDigit).take(6) }, stringResource(R.string.rewards_cost), errors.cost, KeyboardType.Number)
                FormField(price, { price = it.take(15) }, stringResource(R.string.rewards_price), errors.price, KeyboardType.Number, stringResource(R.string.rewards_price_help))
            }
        },
        confirmButton = {
            TextButton(onClick = { errors = onSave(RewardForm(name, description, type, cost, price)) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: FieldError?,
    keyboardType: KeyboardType = KeyboardType.Text,
    help: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = (error?.let { stringResource(it.messageRes) } ?: help)?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

private val RewardFormErrorsSaver = androidx.compose.runtime.saveable.listSaver<RewardFormErrors, String?>(
    save = { listOf(it.name?.name, it.cost?.name, it.price?.name) },
    restore = { list ->
        fun error(name: String?) = FieldError.entries.firstOrNull { it.name == name }
        RewardFormErrors(error(list[0]), error(list[1]), error(list[2]))
    },
)

/** Used by the budget tab to show how a reward's price compares with savings. */
@Composable
fun savingsText(saved: Long, price: Long): String = stringResource(R.string.budget_saved_of_price, rupiahText(saved), rupiahText(price), formatInteger((saved * 100 / price).toInt().coerceAtMost(100)))
