package io.github.vitoosh.vitquest.feature.nutrition.plan

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.SectionHeader
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.model.MealPlanEntry
import io.github.vitoosh.vitquest.core.model.MealType
import io.github.vitoosh.vitquest.core.ui.dayLabel
import io.github.vitoosh.vitquest.core.ui.formatKcal
import io.github.vitoosh.vitquest.core.ui.formatQuantity
import io.github.vitoosh.vitquest.core.ui.labelRes
import io.github.vitoosh.vitquest.core.ui.localized

@Composable
fun PlanTab(
    onBrowseRecipes: () -> Unit,
    onOpenRecipe: (Long) -> Unit,
    onOpenShoppingList: () -> Unit,
    viewModel: PlanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.generated.collect { onOpenShoppingList() }
    }
    val state = uiState
    if (state == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    PlanContent(
        state = state,
        onSelectDate = viewModel::selectDate,
        onRemove = viewModel::remove,
        onBrowseRecipes = onBrowseRecipes,
        onOpenRecipe = onOpenRecipe,
        onGenerateShoppingList = viewModel::generateShoppingList,
        onOpenShoppingList = onOpenShoppingList,
    )
}

@Composable
fun PlanContent(
    state: PlanUiState,
    onSelectDate: (java.time.LocalDate) -> Unit,
    onRemove: (MealPlanEntry) -> Unit,
    onBrowseRecipes: () -> Unit,
    onOpenRecipe: (Long) -> Unit,
    onGenerateShoppingList: () -> Unit,
    onOpenShoppingList: () -> Unit,
) {
    val dayEntries = state.entries.filter { it.date == state.selectedDate }
    CenteredContent {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(VqSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VqSpacing.md),
        ) {
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm),
                ) {
                    state.days.forEach { day ->
                        val count = state.entries.count { it.date == day }
                        FilterChip(
                            selected = day == state.selectedDate,
                            onClick = { onSelectDate(day) },
                            label = {
                                Text(
                                    if (count > 0) "${dayLabel(day, state.today)} · $count" else dayLabel(day, state.today),
                                )
                            },
                        )
                    }
                }
            }
            if (dayEntries.isEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                        Text(stringResource(R.string.plan_empty), style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(onClick = onBrowseRecipes) { Text(stringResource(R.string.plan_browse_recipes)) }
                    }
                }
            } else {
                val dayKcal = dayEntries.sumOf { it.nutrientsPerServing.kcal * it.servings }
                item {
                    Text(
                        text = stringResource(R.string.plan_day_total, formatKcal(dayKcal)),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                MealType.entries.forEach { meal ->
                    val mealEntries = dayEntries.filter { it.meal == meal }
                    if (mealEntries.isNotEmpty()) {
                        item(key = "meal-$meal") { SectionHeader(stringResource(meal.labelRes)) }
                        items(mealEntries, key = { it.id }) { entry ->
                            Card(
                                onClick = { onOpenRecipe(entry.recipeId) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            ) {
                                ListItem(
                                    headlineContent = { Text(localized(entry.recipeNameId, entry.recipeNameEn)) },
                                    supportingContent = {
                                        Text(
                                            stringResource(
                                                R.string.plan_entry_meta,
                                                formatQuantity(entry.servings),
                                                formatKcal(entry.nutrientsPerServing.kcal * entry.servings),
                                            ),
                                        )
                                    },
                                    trailingContent = {
                                        IconButton(onClick = { onRemove(entry) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.plan_remove))
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(modifier = Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.shopping_title), style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = stringResource(R.string.shopping_generate_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = onGenerateShoppingList, enabled = state.entries.isNotEmpty()) {
                                Text(stringResource(R.string.shopping_generate))
                            }
                            OutlinedButton(onClick = onOpenShoppingList) { Text(stringResource(R.string.shopping_open)) }
                        }
                    }
                }
            }
        }
    }
}
