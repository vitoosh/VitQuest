package io.github.vitoosh.vitquest.feature.nutrition.photo

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.vitoosh.vitquest.core.ai.AiConfig
import io.github.vitoosh.vitquest.core.ai.AnalyzeResult
import io.github.vitoosh.vitquest.core.ai.EstimatedItem
import io.github.vitoosh.vitquest.core.ai.FoodPhotoAnalyzer
import io.github.vitoosh.vitquest.core.ai.ImagePrep
import io.github.vitoosh.vitquest.core.data.repository.NutritionRepository
import io.github.vitoosh.vitquest.core.data.repository.PhotoMealItem
import io.github.vitoosh.vitquest.core.data.repository.UserPreferencesRepository
import io.github.vitoosh.vitquest.core.model.MealType
import io.github.vitoosh.vitquest.core.model.Nutrients
import io.github.vitoosh.vitquest.ui.navigation.FoodPhotoRoute
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One estimated item as the user is checking it. Per-gram values let a changed weight rescale the estimate. */
data class ReviewItem(
    val key: Int,
    val name: String,
    /** The name as the AI gave it, to tell whether the user renamed the item. */
    val originalName: String,
    val nameId: String,
    val nameEn: String,
    val grams: String,
    val kcal: String,
    val kcalPerGram: Double,
    val proteinPerGram: Double?,
    val carbsPerGram: Double?,
    val fatPerGram: Double?,
    val confidence: EstimatedItem.Confidence,
    val error: Boolean = false,
)

enum class PhotoError { UNREADABLE_IMAGE, OFFLINE, DAILY_LIMIT, SERVER, NOT_FOOD }

sealed interface FoodPhotoUiState {
    data object NotConfigured : FoodPhotoUiState
    data object Idle : FoodPhotoUiState
    data class Analyzing(val thumbnail: ByteArray?) : FoodPhotoUiState
    data class Review(val thumbnail: ByteArray, val meal: MealType, val items: List<ReviewItem>, val note: String?) : FoodPhotoUiState {
        val totalKcal: Int get() = items.sumOf { it.kcal.toDoubleOrNull() ?: 0.0 }.roundToInt()
    }
    data class Failed(val error: PhotoError, val canRetry: Boolean) : FoodPhotoUiState
}

@HiltViewModel
class FoodPhotoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    config: AiConfig,
    private val analyzer: FoodPhotoAnalyzer,
    private val nutritionRepository: NutritionRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<FoodPhotoRoute>()
    val date: LocalDate = LocalDate.parse(route.date)
    private val initialMeal = MealType.entries.firstOrNull { it.name == route.meal } ?: MealType.SNACK

    private val _state = MutableStateFlow(if (config.isConfigured) FoodPhotoUiState.Idle else FoodPhotoUiState.NotConfigured)
    val state: StateFlow<FoodPhotoUiState> = _state.asStateFlow()

    val consent: StateFlow<Boolean?> = preferences.userPreferences.map { it.aiPhotoConsent }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private var prepared: ImagePrep.Prepared? = null

    fun giveConsent() {
        viewModelScope.launch { preferences.setAiPhotoConsent(true) }
    }

    /** Where the camera app writes the full photo. Only shared with the camera for one capture. */
    val captureUri: Uri by lazy { FileProvider.getUriForFile(context, "${context.packageName}.camera", captureFile) }

    private val captureFile: File get() = File(context.cacheDir, "camera/capture.jpg").apply { parentFile?.mkdirs() }

    /** The camera wrote [captureUri]; the full photo is deleted once the small copies are made. */
    fun onCaptured() = onImage(captureUri, deleteAfter = captureFile)

    fun onImage(uri: Uri) = onImage(uri, deleteAfter = null)

    private fun onImage(uri: Uri, deleteAfter: File?) {
        _state.value = FoodPhotoUiState.Analyzing(null)
        viewModelScope.launch {
            val image = try {
                withContext(Dispatchers.IO) { ImagePrep.prepare(context, uri) }
            } catch (_: IOException) {
                null
            } catch (_: SecurityException) {
                null
            } finally {
                withContext(Dispatchers.IO) { deleteAfter?.delete() }
            }
            if (image == null) {
                _state.value = FoodPhotoUiState.Failed(PhotoError.UNREADABLE_IMAGE, canRetry = false)
                return@launch
            }
            prepared = image
            analyze(image)
        }
    }

    fun retry() {
        prepared?.let { image -> viewModelScope.launch { analyze(image) } } ?: run { _state.value = FoodPhotoUiState.Idle }
    }

    fun startOver() {
        prepared = null
        _state.value = FoodPhotoUiState.Idle
    }

    private suspend fun analyze(image: ImagePrep.Prepared) {
        _state.value = FoodPhotoUiState.Analyzing(image.thumbnail)
        _state.value = when (val result = analyzer.analyze(image.upload, language())) {
            is AnalyzeResult.Success -> if (result.estimate.isFood) {
                FoodPhotoUiState.Review(image.thumbnail, initialMeal, result.estimate.items.mapIndexed(::toReview), result.estimate.note)
            } else {
                FoodPhotoUiState.Failed(PhotoError.NOT_FOOD, canRetry = false)
            }
            AnalyzeResult.NotConfigured -> FoodPhotoUiState.NotConfigured
            AnalyzeResult.Offline -> FoodPhotoUiState.Failed(PhotoError.OFFLINE, canRetry = true)
            AnalyzeResult.DailyLimitReached -> FoodPhotoUiState.Failed(PhotoError.DAILY_LIMIT, canRetry = false)
            AnalyzeResult.ServerError -> FoodPhotoUiState.Failed(PhotoError.SERVER, canRetry = true)
        }
    }

    fun setMeal(meal: MealType) = updateReview { it.copy(meal = meal) }

    fun setName(key: Int, name: String) = updateItem(key) { it.copy(name = name.take(MAX_NAME), error = false) }

    /** A new weight rescales the energy, keeping the AI's energy per gram. */
    fun setGrams(key: Int, input: String) = updateItem(key) { item ->
        val grams = input.replace(',', '.').toDoubleOrNull()
        item.copy(
            grams = input.take(6),
            kcal = grams?.let { formatNumber(it * item.kcalPerGram) } ?: item.kcal,
            error = false,
        )
    }

    fun setKcal(key: Int, input: String) = updateItem(key) { it.copy(kcal = input.take(6), error = false) }

    fun remove(key: Int) = updateReview { review -> review.copy(items = review.items.filterNot { it.key == key }) }

    /** Saves when every item is valid; otherwise marks the ones to fix. */
    fun save() {
        val review = _state.value as? FoodPhotoUiState.Review ?: return
        val checked = review.items.map { it.copy(error = parse(it) == null) }
        if (checked.isEmpty() || checked.any { it.error }) {
            _state.value = review.copy(items = checked)
            return
        }
        val items = checked.map { requireNotNull(parse(it)) }
        viewModelScope.launch {
            nutritionRepository.logPhotoMeal(date, review.meal, items, review.thumbnail)
            _saved.value = true
        }
    }

    private fun parse(item: ReviewItem): PhotoMealItem? {
        val name = item.name.trim().takeIf { it.isNotEmpty() } ?: return null
        val grams = item.grams.replace(',', '.').toDoubleOrNull()?.takeIf { it in 1.0..MAX_GRAMS } ?: return null
        val kcal = item.kcal.replace(',', '.').toDoubleOrNull()?.takeIf { it in 0.0..MAX_KCAL } ?: return null
        // The English name is only kept when the user did not rename the item.
        val nameEn = if (name == item.originalName.trim() && item.nameEn.isNotBlank()) item.nameEn else name
        val nameId = if (name == item.originalName.trim()) item.nameId else name
        return PhotoMealItem(
            nameId = nameId,
            nameEn = nameEn,
            grams = grams,
            nutrients = Nutrients(kcal, item.proteinPerGram?.times(grams), item.carbsPerGram?.times(grams), item.fatPerGram?.times(grams)),
        )
    }

    private fun toReview(index: Int, item: EstimatedItem): ReviewItem {
        val localName = if (language() == "en") item.nameEn else item.nameId
        return ReviewItem(
            key = index,
            name = localName,
            originalName = localName,
            nameId = item.nameId,
            nameEn = item.nameEn,
            grams = formatNumber(item.grams),
            kcal = formatNumber(item.kcal),
            kcalPerGram = item.kcal / item.grams,
            proteinPerGram = item.proteinG?.div(item.grams),
            carbsPerGram = item.carbsG?.div(item.grams),
            fatPerGram = item.fatG?.div(item.grams),
            confidence = item.confidence,
        )
    }

    private fun updateReview(transform: (FoodPhotoUiState.Review) -> FoodPhotoUiState.Review) =
        _state.update { state -> if (state is FoodPhotoUiState.Review) transform(state) else state }

    private fun updateItem(key: Int, transform: (ReviewItem) -> ReviewItem) =
        updateReview { review -> review.copy(items = review.items.map { if (it.key == key) transform(it) else it }) }

    private fun language(): String =
        (AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()).language.let { if (it == "en") "en" else "id" }

    private companion object {
        const val MAX_NAME = 80
        const val MAX_GRAMS = 3_000.0
        const val MAX_KCAL = 5_000.0

        fun formatNumber(value: Double): String = value.roundToInt().toString()
    }
}
