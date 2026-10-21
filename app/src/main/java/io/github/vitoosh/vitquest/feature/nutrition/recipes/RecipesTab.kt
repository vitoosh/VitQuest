package io.github.vitoosh.vitquest.feature.nutrition.recipes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.model.Recipe
import io.github.vitoosh.vitquest.core.model.RecipeTag
import io.github.vitoosh.vitquest.core.ui.formatKcal
import io.github.vitoosh.vitquest.core.ui.labelRes
import io.github.vitoosh.vitquest.core.ui.localized

@Composable
fun RecipesTab(
    onOpenRecipe: (Long) -> Unit,
    viewModel: RecipesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RecipesContent(uiState, viewModel::onQueryChange, viewModel::onTagSelected, onOpenRecipe)
}

@Composable
fun RecipesContent(
    uiState: RecipesUiState,
    onQueryChange: (String) -> Unit,
    onTagSelected: (RecipeTag?) -> Unit,
    onOpenRecipe: (Long) -> Unit,
) {
    CenteredContent {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.recipe_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = VqSpacing.lg, vertical = VqSpacing.sm),
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = VqSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm),
            ) {
                RecipeTag.entries.forEach { tag ->
                    FilterChip(
                        selected = uiState.tag == tag,
                        onClick = { onTagSelected(if (uiState.tag == tag) null else tag) },
                        label = { Text(stringResource(tag.labelRes)) },
                    )
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(VqSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(VqSpacing.md),
            ) {
                if (uiState.recipes.isEmpty() && !uiState.loading) {
                    item { Text(stringResource(R.string.recipe_search_empty)) }
                }
                items(uiState.recipes, key = { it.id }) { recipe ->
                    RecipeCard(recipe = recipe, onClick = { onOpenRecipe(recipe.id) })
                }
                item {
                    Text(
                        text = stringResource(R.string.recipe_estimate_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeCard(recipe: Recipe, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.xs)) {
            Text(localized(recipe.nameId, recipe.nameEn), style = MaterialTheme.typography.titleMedium)
            Text(
                text = localized(recipe.descriptionId, recipe.descriptionEn),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.recipe_card_meta,
                    recipe.servings,
                    formatKcal(recipe.nutrientsPerServing.kcal),
                    recipe.totalMinutes,
                    recipe.servings,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
