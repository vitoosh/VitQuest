package io.github.vitoosh.vitquest.feature.nutrition.diary

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.data.photos.FoodPhotoStore
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.DataSourceKind
import io.github.vitoosh.vitquest.core.designsystem.component.DataSourceTag
import io.github.vitoosh.vitquest.core.designsystem.component.IconBubble
import io.github.vitoosh.vitquest.core.designsystem.component.NumberField
import io.github.vitoosh.vitquest.core.designsystem.icon.VqIcons
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.model.FoodLogEntry
import io.github.vitoosh.vitquest.core.model.MealType
import io.github.vitoosh.vitquest.core.model.Nutrients
import io.github.vitoosh.vitquest.core.ui.formatDecimal
import io.github.vitoosh.vitquest.core.ui.formatInteger
import io.github.vitoosh.vitquest.core.ui.formatKcal
import io.github.vitoosh.vitquest.core.ui.formatQuantity
import io.github.vitoosh.vitquest.core.ui.labelRes
import io.github.vitoosh.vitquest.core.ui.localized
import io.github.vitoosh.vitquest.core.ui.messageRes
import io.github.vitoosh.vitquest.feature.nutrition.CalorieSummaryCard
import io.github.vitoosh.vitquest.feature.nutrition.CalorieTargetDialog
import io.github.vitoosh.vitquest.feature.nutrition.DateNavigator
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DiaryTab(
    onAddFood: (LocalDate, MealType) -> Unit,
    onOpenHydration: () -> Unit,
    onEditProfile: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refreshToday()
        onPauseOrDispose { }
    }
    DiaryContent(
        uiState = uiState,
        onPrevious = viewModel::previousDay,
        onNext = viewModel::nextDay,
        onToday = viewModel::goToToday,
        onAddFood = onAddFood,
        onUpdateQuantity = viewModel::updateQuantity,
        onDelete = viewModel::deleteEntry,
        onSaveTarget = viewModel::saveCalorieTarget,
        onUseRecommended = viewModel::useRecommendedTarget,
        onAddWater = viewModel::addWater,
        onOpenHydration = onOpenHydration,
        onEditProfile = onEditProfile,
    )
}

@Composable
fun DiaryContent(
    uiState: DiaryUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onAddFood: (LocalDate, MealType) -> Unit,
    onUpdateQuantity: (FoodLogEntry, String) -> FieldError?,
    onDelete: (FoodLogEntry) -> Unit,
    onSaveTarget: (String) -> FieldError?,
    onUseRecommended: () -> Unit,
    onAddWater: (Int) -> Unit,
    onOpenHydration: () -> Unit,
    onEditProfile: () -> Unit,
) {
    var editingEntryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showTarget by rememberSaveable { mutableStateOf(false) }

    when (uiState) {
        DiaryUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is DiaryUiState.Ready -> CenteredContent {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = VqSpacing.lg, vertical = VqSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(VqSpacing.md),
            ) {
                item {
                    DateNavigator(
                        date = uiState.date,
                        today = uiState.today,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onToday = onToday,
                    )
                }
                item { CalorieSummaryCard(totals = uiState.totals, energy = uiState.energy, onOpenTarget = { showTarget = true }) }
                item {
                    WaterMiniCard(
                        waterMl = uiState.waterMl,
                        targetMl = uiState.waterTargetMl,
                        servingMl = uiState.waterServingMl,
                        onAdd = { onAddWater(uiState.waterServingMl) },
                        onOpen = onOpenHydration,
                    )
                }
                MealType.entries.forEach { meal ->
                    val mealEntries = uiState.entries.filter { it.meal == meal }
                    item(key = "header-$meal") {
                        MealHeader(
                            meal = meal,
                            nutrients = uiState.totals.byMeal[meal],
                            onAdd = { onAddFood(uiState.date, meal) },
                        )
                    }
                    if (mealEntries.isEmpty()) {
                        item(key = "empty-$meal") {
                            Text(
                                text = stringResource(R.string.diary_meal_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = VqSpacing.lg),
                            )
                        }
                    }
                    items(mealEntries, key = { "entry-${it.id}" }) { entry ->
                        DiaryEntryRow(entry = entry, onClick = { editingEntryId = entry.id })
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.diary_estimate_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = VqSpacing.md),
                    )
                }
            }

            uiState.entries.firstOrNull { it.id == editingEntryId }?.let { entry ->
                EditEntryDialog(
                    entry = entry,
                    onSave = { input -> onUpdateQuantity(entry, input).also { if (it == null) editingEntryId = null } },
                    onDelete = {
                        onDelete(entry)
                        editingEntryId = null
                    },
                    onDismiss = { editingEntryId = null },
                )
            }
            if (showTarget) {
                CalorieTargetDialog(
                    energy = uiState.energy,
                    onSaveManual = { input -> onSaveTarget(input).also { if (it == null) showTarget = false } },
                    onUseRecommended = {
                        onUseRecommended()
                        showTarget = false
                    },
                    onEditProfile = {
                        showTarget = false
                        onEditProfile()
                    },
                    onDismiss = { showTarget = false },
                )
            }
        }
    }
}

@Composable
private fun WaterMiniCard(waterMl: Int, targetMl: Int?, servingMl: Int, onAdd: () -> Unit, onOpen: () -> Unit) {
    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(VqSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VqSpacing.md),
        ) {
            IconBubble(icon = VqIcons.WaterDrop, size = 36)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VqSpacing.xs)) {
                Text(
                    text = if (targetMl != null) {
                        stringResource(R.string.water_of_target, formatInteger(waterMl), formatInteger(targetMl))
                    } else {
                        stringResource(R.string.value_ml, formatInteger(waterMl))
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                if (targetMl != null && targetMl > 0) {
                    LinearProgressIndicator(progress = { (waterMl.toFloat() / targetMl).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                }
            }
            FilledTonalButton(onClick = onAdd) {
                Text(stringResource(R.string.water_add_amount, formatInteger(servingMl)))
            }
        }
    }
}

@Composable
private fun MealHeader(meal: MealType, nutrients: Nutrients?, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = VqSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(meal.labelRes), style = MaterialTheme.typography.titleMedium)
            if (nutrients != null && nutrients.kcal > 0) {
                Text(
                    text = formatKcal(nutrients.kcal),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val description = stringResource(R.string.diary_add_food_description, stringResource(meal.labelRes))
        // Every meal has an "Add" button, so each one gets a description that names its meal.
        TextButton(
            onClick = onAdd,
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            },
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(R.string.diary_add_food))
        }
    }
}

@Composable
private fun DiaryEntryRow(entry: FoodLogEntry, onClick: () -> Unit) {
    var viewingPhoto by rememberSaveable { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(localized(entry.nameId, entry.nameEn)) },
        supportingContent = {
            Text(if (entry.photoId != null) portionText(entry) + " · " + stringResource(R.string.photo_ai_estimate) else portionText(entry))
        },
        leadingContent = entry.photoId?.let { id -> { FoodPhotoThumbnail(id, onClick = { viewingPhoto = true }) } },
        trailingContent = { Text(formatKcal(entry.nutrients.kcal), style = MaterialTheme.typography.titleSmall) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
    if (viewingPhoto && entry.photoId != null) {
        AlertDialog(
            onDismissRequest = { viewingPhoto = false },
            text = { FoodPhotoImage(entry.photoId, Modifier.fillMaxWidth().aspectRatio(4f / 3f)) },
            confirmButton = { TextButton(onClick = { viewingPhoto = false }) { Text(stringResource(R.string.action_close)) } },
        )
    }
}

/** The small copy kept with an entry from a food photo; read from app storage. */
@Composable
private fun FoodPhotoImage(photoId: Long, modifier: Modifier) {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(null, photoId) {
        value = withContext(Dispatchers.IO) {
            val file = File(context.filesDir, FoodPhotoStore.backupPath(photoId))
            if (file.isFile) BitmapFactory.decodeFile(file.path)?.asImageBitmap() else null
        }
    }
    image?.let {
        Image(
            bitmap = it,
            contentDescription = stringResource(R.string.photo_description),
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(12.dp)),
        )
    }
}

@Composable
private fun FoodPhotoThumbnail(photoId: Long, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        FoodPhotoImage(photoId, Modifier.size(40.dp))
    }
}

/**
 * Built-in serving labels already include their weight ("1 piring (150 g)"),
 * so the total weight is only added when it differs from the label.
 */
@Composable
private fun portionText(entry: FoodLogEntry): String {
    val label = entry.servingLabelId?.let { localized(it, entry.servingLabelEn ?: it) }
        ?: return stringResource(R.string.value_grams, formatDecimal(entry.grams))
    val labelShowsWeight = label.contains(" g)") || label.contains(" ml)")
    return when {
        labelShowsWeight && entry.quantity == 1.0 -> label
        labelShowsWeight -> stringResource(R.string.diary_portion_serving, formatQuantity(entry.quantity), label, formatDecimal(entry.grams))
        else -> stringResource(R.string.diary_portion_simple, formatQuantity(entry.quantity), label, formatDecimal(entry.grams))
    }
}

@Composable
private fun EditEntryDialog(
    entry: FoodLogEntry,
    onSave: (String) -> FieldError?,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var input by rememberSaveable(entry.id) { mutableStateOf(formatDecimalPlain(entry.quantity)) }
    var error by rememberSaveable(entry.id) { mutableStateOf<FieldError?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localized(entry.nameId, entry.nameEn)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
                Text(portionText(entry), style = MaterialTheme.typography.bodyMedium)
                NumberField(
                    value = input,
                    onValueChange = {
                        input = it
                        error = null
                    },
                    label = stringResource(R.string.diary_quantity),
                    allowDecimal = true,
                    supportingText = stringResource(R.string.diary_quantity_help),
                    errorText = error?.let { stringResource(it.messageRes) },
                    imeAction = ImeAction.Done,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                    Text(formatKcal(entry.nutrients.kcal), style = MaterialTheme.typography.titleSmall)
                    DataSourceTag(DataSourceKind.ESTIMATE)
                }
            }
        },
        confirmButton = { TextButton(onClick = { error = onSave(input) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}

/** Editable text for a quantity, without grouping separators. */
private fun formatDecimalPlain(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
