package io.github.vitoosh.vitquest.feature.nutrition.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.VqTopAppBar
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.model.ShoppingItem
import io.github.vitoosh.vitquest.core.ui.formatQuantity
import io.github.vitoosh.vitquest.core.ui.localized

@Composable
fun ShoppingListRoute(
    onNavigateBack: () -> Unit,
    viewModel: ShoppingListViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    ShoppingListScreen(
        items = items,
        onAdd = viewModel::add,
        onToggle = viewModel::toggle,
        onDelete = viewModel::delete,
        onClearChecked = viewModel::clearChecked,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun ShoppingListScreen(
    items: List<ShoppingItem>?,
    onAdd: (String) -> Unit,
    onToggle: (ShoppingItem) -> Unit,
    onDelete: (ShoppingItem) -> Unit,
    onClearChecked: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    var newItem by rememberSaveable { mutableStateOf("") }
    val submit = {
        onAdd(newItem)
        newItem = ""
    }
    Scaffold(
        topBar = {
            VqTopAppBar(title = stringResource(R.string.shopping_title), onNavigateBack = onNavigateBack) {
                if (items.orEmpty().any { it.checked }) {
                    TextButton(onClick = onClearChecked) { Text(stringResource(R.string.shopping_clear_checked)) }
                }
            }
        },
        modifier = Modifier.imePadding(),
    ) { padding ->
        if (items == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        CenteredContent(modifier = Modifier.padding(padding)) {
            LazyColumn(contentPadding = PaddingValues(vertical = VqSpacing.sm)) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = VqSpacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm),
                    ) {
                        OutlinedTextField(
                            value = newItem,
                            onValueChange = { newItem = it },
                            label = { Text(stringResource(R.string.shopping_add_hint)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submit() }),
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = submit, enabled = newItem.isNotBlank()) {
                            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add))
                        }
                    }
                }
                if (items.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.shopping_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(VqSpacing.lg),
                        )
                    }
                }
                items(items, key = { it.id }) { item ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = localized(item.nameId, item.nameEn),
                                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                            )
                        },
                        supportingContent = item.quantity?.let { quantity ->
                            { Text("${formatQuantity(quantity)} ${localized(item.unitId.orEmpty(), item.unitEn.orEmpty())}") }
                        },
                        leadingContent = { Checkbox(checked = item.checked, onCheckedChange = null) },
                        trailingContent = {
                            IconButton(onClick = { onDelete(item) }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                            }
                        },
                        modifier = Modifier.toggleable(value = item.checked, role = Role.Checkbox, onValueChange = { onToggle(item) }),
                    )
                }
            }
        }
    }
}
