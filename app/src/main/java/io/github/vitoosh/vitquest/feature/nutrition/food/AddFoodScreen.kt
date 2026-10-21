package io.github.vitoosh.vitquest.feature.nutrition.food

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.DataSourceKind
import io.github.vitoosh.vitquest.core.designsystem.component.DataSourceTag
import io.github.vitoosh.vitquest.core.designsystem.component.NumberField
import io.github.vitoosh.vitquest.core.designsystem.component.SectionHeader
import io.github.vitoosh.vitquest.core.designsystem.component.SingleChoiceChips
import io.github.vitoosh.vitquest.core.designsystem.component.VqTopAppBar
import io.github.vitoosh.vitquest.core.designsystem.icon.VqIcons
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.domain.FieldResult
import io.github.vitoosh.vitquest.core.domain.nutrition.PortionCalculator
import io.github.vitoosh.vitquest.core.model.Food
import io.github.vitoosh.vitquest.core.model.FoodServing
import io.github.vitoosh.vitquest.core.model.MealType
import io.github.vitoosh.vitquest.core.ui.dayLabel
import io.github.vitoosh.vitquest.core.ui.formatKcal
import io.github.vitoosh.vitquest.core.ui.labelRes
import io.github.vitoosh.vitquest.core.ui.localized
import io.github.vitoosh.vitquest.core.ui.messageRes
import io.github.vitoosh.vitquest.feature.nutrition.MacroRow
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun AddFoodRoute(
    onNavigateBack: () -> Unit,
    onOpenPhoto: () -> Unit = {},
    viewModel: AddFoodViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val queryText by viewModel.queryText.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var selectedFoodId by rememberSaveable { mutableStateOf<Long?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    // A freshly created custom food may not be in the current list yet, so keep it here.
    var createdFood by remember { mutableStateOf<Food?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.added.collect { name ->
            snackbarHostState.showSnackbar(resources.getString(R.string.food_added, name))
        }
    }

    val allFoods = uiState.results + uiState.favorites + uiState.recent + listOfNotNull(createdFood)
    val selectedFood = allFoods.firstOrNull { it.id == selectedFoodId }

    AddFoodScreen(
        date = viewModel.date,
        meal = viewModel.initialMeal,
        uiState = uiState,
        queryText = queryText,
        snackbarHostState = snackbarHostState,
        onQueryChange = viewModel::onQueryChange,
        onSelectFood = { selectedFoodId = it.id },
        onToggleFavorite = viewModel::toggleFavorite,
        onCreateFood = { creating = true },
        onNavigateBack = onNavigateBack,
        onOpenPhoto = onOpenPhoto,
    )

    if (selectedFood != null) {
        PortionDialog(
            food = selectedFood,
            initialMeal = viewModel.initialMeal,
            onLog = { meal, serving, amount ->
                viewModel.log(selectedFood, meal, serving, amount).also { if (it == null) selectedFoodId = null }
            },
            onDeleteCustom = {
                viewModel.deleteCustomFood(selectedFood)
                selectedFoodId = null
            },
            onDismiss = { selectedFoodId = null },
        )
    }
    if (creating) {
        CustomFoodDialog(
            onSave = { form ->
                scope.launch {
                    val (food, _) = viewModel.saveCustomFood(form)
                    if (food != null) {
                        createdFood = food
                        creating = false
                        selectedFoodId = food.id
                    }
                }
            },
            validate = { form -> viewModel.validateOnly(form) },
            onDismiss = { creating = false },
        )
    }
}

@Composable
fun AddFoodScreen(
    date: LocalDate,
    meal: MealType,
    uiState: AddFoodUiState,
    queryText: String,
    snackbarHostState: SnackbarHostState,
    onQueryChange: (String) -> Unit,
    onSelectFood: (Food) -> Unit,
    onToggleFavorite: (Food) -> Unit,
    onCreateFood: () -> Unit,
    onNavigateBack: () -> Unit,
    onOpenPhoto: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            VqTopAppBar(
                title = stringResource(R.string.add_food_title, stringResource(meal.labelRes)),
                onNavigateBack = onNavigateBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateFood,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.custom_food_create)) },
            )
        },
        modifier = Modifier.imePadding(),
    ) { padding ->
        CenteredContent(modifier = Modifier.padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = dayLabel(date, LocalDate.now()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = VqSpacing.lg),
                )
                OutlinedTextField(
                    value = queryText,
                    onValueChange = onQueryChange,
                    label = { Text(stringResource(R.string.food_search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (queryText.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_clear))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = VqSpacing.lg, vertical = VqSpacing.sm),
                )
                FilledTonalButton(
                    onClick = onOpenPhoto,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = VqSpacing.lg),
                ) {
                    Icon(VqIcons.PhotoCamera, contentDescription = null)
                    Text(stringResource(R.string.photo_entry), modifier = Modifier.padding(start = VqSpacing.sm))
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // Leaves room so the floating button never covers the last row.
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    if (uiState.query.isEmpty()) {
                        foodSection(R.string.food_section_favorites, uiState.favorites, "fav", onSelectFood, onToggleFavorite)
                        foodSection(R.string.food_section_recent, uiState.recent, "recent", onSelectFood, onToggleFavorite)
                        foodSection(R.string.food_section_all, uiState.results, "all", onSelectFood, onToggleFavorite)
                    } else {
                        if (uiState.results.isEmpty() && !uiState.loading) {
                            item {
                                Column(
                                    modifier = Modifier.padding(VqSpacing.lg),
                                    verticalArrangement = Arrangement.spacedBy(VqSpacing.sm),
                                ) {
                                    Text(stringResource(R.string.food_search_empty, uiState.query))
                                    TextButton(onClick = onCreateFood) { Text(stringResource(R.string.custom_food_create)) }
                                }
                            }
                        }
                        foodSection(null, uiState.results, "results", onSelectFood, onToggleFavorite)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.foodSection(
    titleRes: Int?,
    foods: List<Food>,
    keyPrefix: String,
    onSelect: (Food) -> Unit,
    onToggleFavorite: (Food) -> Unit,
) {
    if (foods.isEmpty()) return
    if (titleRes != null) {
        item(key = "title-$keyPrefix") {
            SectionHeader(
                text = stringResource(titleRes),
                modifier = Modifier.padding(start = VqSpacing.lg, end = VqSpacing.lg, top = VqSpacing.md),
            )
        }
    }
    items(foods, key = { "$keyPrefix-${it.id}" }) { food ->
        FoodRow(food = food, onClick = { onSelect(food) }, onToggleFavorite = { onToggleFavorite(food) })
    }
}

@Composable
private fun FoodRow(food: Food, onClick: () -> Unit, onToggleFavorite: () -> Unit) {
    val serving = food.servings.firstOrNull()
    ListItem(
        headlineContent = { Text(localized(food.nameId, food.nameEn)) },
        supportingContent = {
            Text(
                if (serving != null) {
                    stringResource(
                        R.string.food_per_serving,
                        formatKcal(PortionCalculator.nutrients(food.per100g, serving.grams).kcal),
                        localized(serving.labelId, serving.labelEn),
                    )
                } else {
                    stringResource(R.string.food_per_100g, formatKcal(food.per100g.kcal))
                },
            )
        },
        trailingContent = {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (food.isFavorite) Icons.Filled.Star else VqIcons.StarBorder,
                    contentDescription = stringResource(
                        if (food.isFavorite) R.string.food_unfavorite else R.string.food_favorite,
                    ),
                    tint = if (food.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}

/** Choose serving and amount; nutrients update as the user types. */
@Composable
fun PortionDialog(
    food: Food,
    initialMeal: MealType,
    onLog: (MealType, FoodServing?, String) -> FieldError?,
    onDeleteCustom: () -> Unit,
    onDismiss: () -> Unit,
) {
    var meal by rememberSaveable(food.id) { mutableStateOf(initialMeal) }
    // Index into servings; -1 means "grams".
    var servingIndex by rememberSaveable(food.id) { mutableIntStateOf(if (food.servings.isEmpty()) -1 else 0) }
    var amount by rememberSaveable(food.id) { mutableStateOf(if (food.servings.isEmpty()) "100" else "1") }
    var error by rememberSaveable(food.id) { mutableStateOf<FieldError?>(null) }
    val serving = food.servings.getOrNull(servingIndex)
    val portion = (AddFoodViewModel.parsePortion(serving, amount) as? FieldResult.Valid)?.value
    val gramsLabel = stringResource(R.string.unit_grams_option)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localized(food.nameId, food.nameEn)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(VqSpacing.md),
            ) {
                SingleChoiceChips(
                    title = stringResource(R.string.portion_serving),
                    options = food.servings.indices.toList() + -1,
                    selected = servingIndex,
                    onSelect = { index ->
                        if (index != null) {
                            servingIndex = index
                            amount = if (index == -1) formatPlain(serving?.grams ?: 100.0) else "1"
                            error = null
                        }
                    },
                    optionLabel = { index ->
                        food.servings.getOrNull(index)?.let { localized(it.labelId, it.labelEn) } ?: gramsLabel
                    },
                )
                NumberField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        error = null
                    },
                    label = stringResource(if (serving != null) R.string.portion_quantity else R.string.portion_grams),
                    unit = if (serving == null) stringResource(R.string.unit_grams) else null,
                    allowDecimal = true,
                    errorText = error?.let { stringResource(it.messageRes) },
                    imeAction = ImeAction.Done,
                )
                SingleChoiceChips(
                    title = stringResource(R.string.portion_meal),
                    options = MealType.entries,
                    selected = meal,
                    onSelect = { if (it != null) meal = it },
                    optionLabel = { stringResource(it.labelRes) },
                )
                if (portion != null) {
                    val nutrients = PortionCalculator.nutrients(food.per100g, portion.grams)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                        Text(formatKcal(nutrients.kcal), style = MaterialTheme.typography.titleLarge)
                        DataSourceTag(if (food.isCustom) DataSourceKind.MANUAL else DataSourceKind.ESTIMATE)
                    }
                    MacroRow(nutrients)
                }
                Text(
                    text = sourceText(food),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (food.isCustom) {
                    TextButton(onClick = onDeleteCustom) {
                        Text(stringResource(R.string.custom_food_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { error = onLog(meal, serving, amount) }) { Text(stringResource(R.string.action_add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun sourceText(food: Food): String = when (food.source) {
    null -> stringResource(R.string.food_source_custom)
    "TKPI" -> stringResource(R.string.food_source_tkpi)
    "USDA" -> stringResource(R.string.food_source_usda)
    else -> stringResource(R.string.food_source_estimate)
}

private fun formatPlain(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

@Composable
fun CustomFoodDialog(
    onSave: (CustomFoodForm) -> Unit,
    validate: (CustomFoodForm) -> CustomFoodErrors,
    onDismiss: () -> Unit,
) {
    var form by remember { mutableStateOf(CustomFoodForm()) }
    var errors by remember { mutableStateOf(CustomFoodErrors()) }
    fun update(transform: (CustomFoodForm) -> CustomFoodForm) {
        form = transform(form)
        errors = CustomFoodErrors()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_food_create)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(VqSpacing.sm),
            ) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { value -> update { it.copy(name = value) } },
                    label = { Text(stringResource(R.string.custom_food_name)) },
                    isError = errors.name,
                    supportingText = if (errors.name) ({ Text(stringResource(R.string.error_required)) }) else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                SingleChoiceChips(
                    title = stringResource(R.string.custom_food_values_per),
                    options = listOf(true, false),
                    selected = form.perServing,
                    onSelect = { value -> if (value != null) update { it.copy(perServing = value) } },
                    optionLabel = { stringResource(if (it) R.string.custom_food_per_serving else R.string.custom_food_per_100g) },
                )
                if (form.perServing) {
                    OutlinedTextField(
                        value = form.servingLabel,
                        onValueChange = { value -> update { it.copy(servingLabel = value) } },
                        label = { Text(stringResource(R.string.custom_food_serving_label)) },
                        placeholder = { Text(stringResource(R.string.custom_food_serving_label_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    NumberField(
                        value = form.servingGrams,
                        onValueChange = { value -> update { it.copy(servingGrams = value) } },
                        label = stringResource(R.string.custom_food_serving_grams),
                        unit = stringResource(R.string.unit_grams),
                        allowDecimal = true,
                        errorText = errors.servingGrams?.let { stringResource(it.messageRes) },
                    )
                }
                NumberField(
                    value = form.kcal,
                    onValueChange = { value -> update { it.copy(kcal = value) } },
                    label = stringResource(R.string.custom_food_kcal),
                    unit = stringResource(R.string.unit_kcal),
                    allowDecimal = true,
                    errorText = errors.kcal?.let { stringResource(it.messageRes) },
                )
                NumberField(
                    value = form.protein,
                    onValueChange = { value -> update { it.copy(protein = value) } },
                    label = stringResource(R.string.macro_protein_optional),
                    unit = stringResource(R.string.unit_grams),
                    allowDecimal = true,
                    errorText = errors.protein?.let { stringResource(it.messageRes) },
                )
                NumberField(
                    value = form.carbs,
                    onValueChange = { value -> update { it.copy(carbs = value) } },
                    label = stringResource(R.string.macro_carbs_optional),
                    unit = stringResource(R.string.unit_grams),
                    allowDecimal = true,
                    errorText = errors.carbs?.let { stringResource(it.messageRes) },
                )
                NumberField(
                    value = form.fat,
                    onValueChange = { value -> update { it.copy(fat = value) } },
                    label = stringResource(R.string.macro_fat_optional),
                    unit = stringResource(R.string.unit_grams),
                    allowDecimal = true,
                    errorText = errors.fat?.let { stringResource(it.messageRes) },
                    imeAction = ImeAction.Done,
                )
                Text(
                    text = stringResource(R.string.custom_food_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    errors = validate(form)
                    if (errors.isEmpty) onSave(form)
                },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
