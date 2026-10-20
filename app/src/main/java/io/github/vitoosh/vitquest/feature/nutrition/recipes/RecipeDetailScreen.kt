package io.github.vitoosh.vitquest.feature.nutrition.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.DataSourceKind
import io.github.vitoosh.vitquest.core.designsystem.component.DataSourceTag
import io.github.vitoosh.vitquest.core.designsystem.component.SectionHeader
import io.github.vitoosh.vitquest.core.designsystem.component.SingleChoiceChips
import io.github.vitoosh.vitquest.core.designsystem.component.VqTopAppBar
import io.github.vitoosh.vitquest.core.designsystem.icon.VqIcons
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.model.MealType
import io.github.vitoosh.vitquest.core.model.Recipe
import io.github.vitoosh.vitquest.core.ui.dayLabel
import io.github.vitoosh.vitquest.core.ui.formatDecimal
import io.github.vitoosh.vitquest.core.ui.formatKcal
import io.github.vitoosh.vitquest.core.ui.formatQuantity
import io.github.vitoosh.vitquest.core.ui.isIndonesian
import io.github.vitoosh.vitquest.core.ui.labelRes
import io.github.vitoosh.vitquest.core.ui.localized
import io.github.vitoosh.vitquest.feature.nutrition.MacroRow
import java.time.LocalDate

private const val MAX_SERVINGS = 12

@Composable
fun RecipeDetailRoute(
    onNavigateBack: () -> Unit,
    viewModel: RecipeDetailViewModel = hiltViewModel(),
) {
    val recipe by viewModel.recipe.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(
                resources.getString(if (message == RecipeMessage.Logged) R.string.recipe_logged else R.string.recipe_planned),
            )
        }
    }
    RecipeDetailScreen(
        recipe = recipe,
        today = viewModel.today,
        snackbarHostState = snackbarHostState,
        onLog = viewModel::logToDiary,
        onPlan = viewModel::addToPlan,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun RecipeDetailScreen(
    recipe: Recipe?,
    today: LocalDate,
    snackbarHostState: SnackbarHostState,
    onLog: (MealType, Double) -> Unit,
    onPlan: (LocalDate, MealType, Double) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var servings by rememberSaveable(recipe?.id) { mutableIntStateOf(recipe?.servings ?: 1) }
    var logging by rememberSaveable { mutableStateOf(false) }
    var planning by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            VqTopAppBar(
                title = recipe?.let { localized(it.nameId, it.nameEn) } ?: stringResource(R.string.nutrition_tab_recipes),
                onNavigateBack = onNavigateBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (recipe == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val factor = servings.toDouble() / recipe.servings
        CenteredContent(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(VqSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(VqSpacing.lg),
            ) {
                Text(localized(recipe.descriptionId, recipe.descriptionEn), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.recipe_time, recipe.prepMinutes, recipe.cookMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(modifier = Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.recipe_per_serving),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            DataSourceTag(DataSourceKind.ESTIMATE)
                        }
                        Text(formatKcal(recipe.nutrientsPerServing.kcal), style = MaterialTheme.typography.headlineSmall)
                        MacroRow(recipe.nutrientsPerServing)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
                    Text(stringResource(R.string.recipe_servings), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    FilledTonalIconButton(onClick = { if (servings > 1) servings-- }, enabled = servings > 1) {
                        Icon(VqIcons.Remove, contentDescription = stringResource(R.string.recipe_servings_decrease))
                    }
                    Text(
                        text = servings.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.width(32.dp),
                    )
                    FilledTonalIconButton(onClick = { if (servings < MAX_SERVINGS) servings++ }, enabled = servings < MAX_SERVINGS) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.recipe_servings_increase))
                    }
                }

                SectionHeader(stringResource(R.string.recipe_ingredients))
                Column(verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                    recipe.ingredients.forEach { item ->
                        Column {
                            // Amounts already in grams or ml don't repeat the weight in brackets.
                            val unit = localized(item.unitId, item.unitEn)
                            Text(
                                text = if (item.unitId == "g" || item.unitId == "ml") {
                                    stringResource(
                                        R.string.recipe_ingredient_line_simple,
                                        formatQuantity(item.quantity * factor),
                                        unit,
                                        localized(item.nameId, item.nameEn),
                                    )
                                } else {
                                    stringResource(
                                        R.string.recipe_ingredient_line,
                                        formatQuantity(item.quantity * factor),
                                        unit,
                                        localized(item.nameId, item.nameEn),
                                        formatDecimal(item.grams * factor),
                                    )
                                },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            val substitute = item.substituteId?.let { localized(it, item.substituteEn ?: it) }
                            if (substitute != null) {
                                Text(
                                    text = stringResource(R.string.recipe_substitute, substitute),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()
                SectionHeader(stringResource(R.string.recipe_steps))
                Column(verticalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
                    val steps = if (isIndonesian()) recipe.stepsId else recipe.stepsEn
                    steps.forEachIndexed { index, step ->
                        Row(horizontalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(step, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.recipe_estimate_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(VqSpacing.md), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { logging = true }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.recipe_log))
                    }
                    OutlinedButton(onClick = { planning = true }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.recipe_plan))
                    }
                }
            }
        }
    }

    if (logging) {
        RecipeActionDialog(
            title = stringResource(R.string.recipe_log),
            today = today,
            showDates = false,
            onConfirm = { _, meal, eaten ->
                onLog(meal, eaten)
                logging = false
            },
            onDismiss = { logging = false },
        )
    }
    if (planning) {
        RecipeActionDialog(
            title = stringResource(R.string.recipe_plan),
            today = today,
            showDates = true,
            onConfirm = { date, meal, planned ->
                onPlan(date, meal, planned)
                planning = false
            },
            onDismiss = { planning = false },
        )
    }
}

/** Picks the meal, the number of servings and (for planning) the day. */
@Composable
private fun RecipeActionDialog(
    title: String,
    today: LocalDate,
    showDates: Boolean,
    onConfirm: (LocalDate, MealType, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var meal by rememberSaveable { mutableStateOf(MealType.LUNCH) }
    var servings by rememberSaveable { mutableDoubleStateOf(1.0) }
    var dayOffset by rememberSaveable { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(VqSpacing.md),
            ) {
                if (showDates) {
                    SingleChoiceChips(
                        title = stringResource(R.string.plan_day),
                        options = (0..6).toList(),
                        selected = dayOffset,
                        onSelect = { if (it != null) dayOffset = it },
                        optionLabel = { dayLabel(today.plusDays(it.toLong()), today) },
                    )
                }
                SingleChoiceChips(
                    title = stringResource(R.string.portion_meal),
                    options = MealType.entries,
                    selected = meal,
                    onSelect = { if (it != null) meal = it },
                    optionLabel = { stringResource(it.labelRes) },
                )
                SingleChoiceChips(
                    title = stringResource(R.string.recipe_servings_eaten),
                    options = listOf(0.5, 1.0, 1.5, 2.0, 3.0, 4.0),
                    selected = servings,
                    onSelect = { if (it != null) servings = it },
                    optionLabel = { formatQuantity(it) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(today.plusDays(dayOffset.toLong()), meal, servings) }) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
