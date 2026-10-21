package io.github.vitoosh.vitquest.feature.nutrition.photo

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.ai.EstimatedItem
import io.github.vitoosh.vitquest.core.designsystem.component.CenteredContent
import io.github.vitoosh.vitquest.core.designsystem.component.VqBadge
import io.github.vitoosh.vitquest.core.designsystem.component.VqTopAppBar
import io.github.vitoosh.vitquest.core.designsystem.icon.VqIcons
import io.github.vitoosh.vitquest.core.designsystem.theme.VqSpacing
import io.github.vitoosh.vitquest.core.model.MealType
import io.github.vitoosh.vitquest.core.ui.formatInteger
import io.github.vitoosh.vitquest.core.ui.labelRes

data class FoodPhotoActions(
    val giveConsent: () -> Unit = {},
    val takePhoto: () -> Unit = {},
    val pickPhoto: () -> Unit = {},
    val retry: () -> Unit = {},
    val startOver: () -> Unit = {},
    val setMeal: (MealType) -> Unit = {},
    val setName: (Int, String) -> Unit = { _, _ -> },
    val setGrams: (Int, String) -> Unit = { _, _ -> },
    val setKcal: (Int, String) -> Unit = { _, _ -> },
    val remove: (Int) -> Unit = {},
    val save: () -> Unit = {},
)

@Composable
fun FoodPhotoRoute(onNavigateBack: () -> Unit, onSaved: () -> Unit, viewModel: FoodPhotoViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val consent by viewModel.consent.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    LaunchedEffect(saved) { if (saved) onSaved() }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        if (taken) viewModel.onCaptured()
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let(viewModel::onImage)
    }

    FoodPhotoScreen(
        state = state,
        consent = consent,
        onNavigateBack = onNavigateBack,
        actions = FoodPhotoActions(
            giveConsent = viewModel::giveConsent,
            takePhoto = { camera.launch(viewModel.captureUri) },
            pickPhoto = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            retry = viewModel::retry,
            startOver = viewModel::startOver,
            setMeal = viewModel::setMeal,
            setName = viewModel::setName,
            setGrams = viewModel::setGrams,
            setKcal = viewModel::setKcal,
            remove = viewModel::remove,
            save = viewModel::save,
        ),
    )
}

@Composable
fun FoodPhotoScreen(state: FoodPhotoUiState, consent: Boolean?, onNavigateBack: () -> Unit, actions: FoodPhotoActions) {
    Scaffold(topBar = { VqTopAppBar(title = stringResource(R.string.photo_title), onNavigateBack = onNavigateBack) }) { padding ->
        CenteredContent(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(VqSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(VqSpacing.md),
            ) {
                when {
                    state is FoodPhotoUiState.NotConfigured -> InfoCard(stringResource(R.string.photo_not_configured))
                    consent == null -> Unit
                    !consent -> ConsentCard(actions.giveConsent)
                    else -> when (state) {
                        FoodPhotoUiState.NotConfigured -> Unit
                        FoodPhotoUiState.Idle -> ChooseCard(actions)
                        is FoodPhotoUiState.Analyzing -> Analyzing(state.thumbnail)
                        is FoodPhotoUiState.Review -> Review(state, actions)
                        is FoodPhotoUiState.Failed -> Failed(state, actions)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(VqSpacing.lg))
    }
}

@Composable
private fun ConsentCard(onAgree: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(Modifier.padding(VqSpacing.lg), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
            Text(stringResource(R.string.photo_consent_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.photo_consent_text), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onAgree) { Text(stringResource(R.string.photo_consent_agree)) }
        }
    }
}

@Composable
private fun ChooseCard(actions: FoodPhotoActions) {
    Text(stringResource(R.string.photo_intro), style = MaterialTheme.typography.bodyMedium)
    Button(onClick = actions.takePhoto, modifier = Modifier.fillMaxWidth()) {
        Icon(VqIcons.PhotoCamera, contentDescription = null)
        Text(stringResource(R.string.photo_take), modifier = Modifier.padding(start = VqSpacing.sm))
    }
    OutlinedButton(onClick = actions.pickPhoto, modifier = Modifier.fillMaxWidth()) {
        Icon(VqIcons.PhotoLibrary, contentDescription = null)
        Text(stringResource(R.string.photo_pick), modifier = Modifier.padding(start = VqSpacing.sm))
    }
    Text(stringResource(R.string.photo_tips), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Analyzing(thumbnail: ByteArray?) {
    thumbnail?.let { Photo(it) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VqSpacing.md)) {
        CircularProgressIndicator()
        Text(stringResource(R.string.photo_analyzing), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Failed(state: FoodPhotoUiState.Failed, actions: FoodPhotoActions) {
    InfoCard(
        stringResource(
            when (state.error) {
                PhotoError.UNREADABLE_IMAGE -> R.string.photo_error_image
                PhotoError.OFFLINE -> R.string.photo_error_offline
                PhotoError.DAILY_LIMIT -> R.string.photo_error_limit
                PhotoError.SERVER -> R.string.photo_error_server
                PhotoError.NOT_FOOD -> R.string.photo_error_not_food
            },
        ),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
        if (state.canRetry) FilledTonalButton(onClick = actions.retry) { Text(stringResource(R.string.action_retry)) }
        OutlinedButton(onClick = actions.startOver) { Text(stringResource(R.string.photo_another)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Review(state: FoodPhotoUiState.Review, actions: FoodPhotoActions) {
    Photo(state.thumbnail)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(Modifier.padding(VqSpacing.md), horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
            Icon(VqIcons.AutoAwesome, contentDescription = null)
            Text(stringResource(R.string.photo_estimate_note), style = MaterialTheme.typography.bodySmall)
        }
    }
    state.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
        MealType.entries.forEach { meal ->
            FilterChip(selected = state.meal == meal, onClick = { actions.setMeal(meal) }, label = { Text(stringResource(meal.labelRes)) })
        }
    }
    state.items.forEach { item -> ItemCard(item, actions) }
    if (state.items.isEmpty()) Text(stringResource(R.string.photo_no_items), style = MaterialTheme.typography.bodyMedium)
    Text(stringResource(R.string.photo_total, formatInteger(state.totalKcal)), style = MaterialTheme.typography.titleMedium)
    Button(onClick = actions.save, enabled = state.items.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.photo_save))
    }
    TextButton(onClick = actions.startOver) { Text(stringResource(R.string.photo_another)) }
}

@Composable
private fun ItemCard(item: ReviewItem, actions: FoodPhotoActions) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(VqSpacing.md), verticalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { actions.setName(item.key, it) },
                    label = { Text(stringResource(R.string.photo_item_name)) },
                    singleLine = true,
                    isError = item.error,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { actions.remove(item.key) }) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.photo_item_remove, item.name))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(VqSpacing.sm)) {
                OutlinedTextField(
                    value = item.grams,
                    onValueChange = { actions.setGrams(item.key, it) },
                    label = { Text(stringResource(R.string.photo_item_grams)) },
                    singleLine = true,
                    isError = item.error,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = item.kcal,
                    onValueChange = { actions.setKcal(item.key, it) },
                    label = { Text(stringResource(R.string.photo_item_kcal)) },
                    singleLine = true,
                    isError = item.error,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            VqBadge(
                text = stringResource(
                    when (item.confidence) {
                        EstimatedItem.Confidence.HIGH -> R.string.photo_confidence_high
                        EstimatedItem.Confidence.MEDIUM -> R.string.photo_confidence_medium
                        EstimatedItem.Confidence.LOW -> R.string.photo_confidence_low
                    },
                ),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
            if (item.error) {
                Text(stringResource(R.string.photo_item_invalid), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun Photo(jpeg: ByteArray) {
    val image = remember(jpeg) { BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)?.asImageBitmap() } ?: return
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Image(
            bitmap = image,
            contentDescription = stringResource(R.string.photo_description),
            contentScale = ContentScale.Crop,
            modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(16.dp)),
        )
    }
}
