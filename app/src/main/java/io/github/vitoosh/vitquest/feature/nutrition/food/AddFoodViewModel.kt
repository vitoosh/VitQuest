package io.github.vitoosh.vitquest.feature.nutrition.food

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.core.data.repository.CustomFoodInput
import io.github.vitoosh.vitquest.core.data.repository.NutritionRepository
import io.github.vitoosh.vitquest.core.data.repository.Portion
import io.github.vitoosh.vitquest.core.domain.FieldError
import io.github.vitoosh.vitquest.core.domain.FieldResult
import io.github.vitoosh.vitquest.core.domain.InputLimits
import io.github.vitoosh.vitquest.core.domain.NumberInput
import io.github.vitoosh.vitquest.core.domain.errorOrNull
import io.github.vitoosh.vitquest.core.model.Food
import io.github.vitoosh.vitquest.core.model.FoodServing
import io.github.vitoosh.vitquest.core.model.MealType
import io.github.vitoosh.vitquest.core.model.Nutrients
import io.github.vitoosh.vitquest.ui.navigation.AddFoodRoute
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AddFoodUiState(
    val query: String = "",
    val favorites: List<Food> = emptyList(),
    val recent: List<Food> = emptyList(),
    val results: List<Food> = emptyList(),
    val loading: Boolean = true,
)

/** Validated form for creating a custom food. */
data class CustomFoodForm(
    val name: String = "",
    val perServing: Boolean = true,
    val servingLabel: String = "",
    val servingGrams: String = "",
    val kcal: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
)

data class CustomFoodErrors(
    val name: Boolean = false,
    val servingGrams: FieldError? = null,
    val kcal: FieldError? = null,
    val protein: FieldError? = null,
    val carbs: FieldError? = null,
    val fat: FieldError? = null,
) {
    val isEmpty get() = !name && servingGrams == null && kcal == null && protein == null && carbs == null && fat == null
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class AddFoodViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NutritionRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<AddFoodRoute>()
    val date: LocalDate = LocalDate.parse(route.date)
    val initialMeal: MealType = MealType.entries.firstOrNull { it.name == route.meal } ?: MealType.SNACK

    private val query = MutableStateFlow("")
    private val _added = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /** Emits the logged food's name so the screen can confirm it. */
    val added: SharedFlow<String> = _added.asSharedFlow()

    private val _queryText = MutableStateFlow("")
    val queryText: StateFlow<String> = _queryText.asStateFlow()

    val uiState: StateFlow<AddFoodUiState> = combine(
        query.debounce(SEARCH_DEBOUNCE_MS).flatMapLatest { repository.searchFoods(it) },
        repository.favoriteFoods(),
        repository.recentFoods(),
        query,
    ) { results, favorites, recent, q ->
        AddFoodUiState(query = q, favorites = favorites, recent = recent, results = results, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddFoodUiState())

    fun onQueryChange(text: String) {
        _queryText.value = text
        query.value = text.trim()
    }

    fun toggleFavorite(food: Food) {
        viewModelScope.launch { repository.setFavorite(food.id, !food.isFavorite) }
    }

    /** Validates the amount and logs the food. Returns an error or null on success. */
    fun log(food: Food, meal: MealType, serving: FoodServing?, amountInput: String): FieldError? {
        val portion = when (val result = parsePortion(serving, amountInput)) {
            is FieldResult.Invalid -> return result.error
            is FieldResult.Valid -> requireNotNull(result.value)
        }
        viewModelScope.launch {
            repository.logFood(date, meal, food, portion)
            _added.emit(food.nameId)
        }
        return null
    }

    fun deleteCustomFood(food: Food) {
        if (food.isCustom) viewModelScope.launch { repository.deleteCustomFood(food.id) }
    }

    fun validateOnly(form: CustomFoodForm): CustomFoodErrors = parseCustomFood(form).second

    /** Saves a custom food and returns it so the portion dialog can open right away. */
    suspend fun saveCustomFood(form: CustomFoodForm): Pair<Food?, CustomFoodErrors> {
        val (input, errors) = parseCustomFood(form)
        if (input == null) return null to errors
        val id = repository.saveCustomFood(input)
        return repository.getFood(id) to errors
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 200L

        /** Validates the form and converts values entered per serving to per 100 g. */
        fun parseCustomFood(form: CustomFoodForm): Pair<CustomFoodInput?, CustomFoodErrors> {
            val grams = NumberInput.parseDecimal(form.servingGrams, InputLimits.FOOD_GRAMS, required = form.perServing)
            val kcalRange = if (form.perServing) 0.0..5_000.0 else InputLimits.KCAL_PER_100G
            val kcal = NumberInput.parseDecimal(form.kcal, kcalRange, required = true)
            val macroRange = if (form.perServing) 0.0..500.0 else InputLimits.MACRO_PER_100G
            val protein = NumberInput.parseDecimal(form.protein, macroRange)
            val carbs = NumberInput.parseDecimal(form.carbs, macroRange)
            val fat = NumberInput.parseDecimal(form.fat, macroRange)
            val errors = CustomFoodErrors(
                name = form.name.isBlank(),
                servingGrams = grams.errorOrNull,
                kcal = kcal.errorOrNull,
                protein = protein.errorOrNull,
                carbs = carbs.errorOrNull,
                fat = fat.errorOrNull,
            )
            if (!errors.isEmpty) return null to errors

            val servingGrams = (grams as FieldResult.Valid).value
            val factor = if (form.perServing) 100.0 / requireNotNull(servingGrams) else 1.0
            val per100g = Nutrients(
                kcal = requireNotNull((kcal as FieldResult.Valid).value) * factor,
                proteinG = (protein as FieldResult.Valid).value?.times(factor),
                carbsG = (carbs as FieldResult.Valid).value?.times(factor),
                fatG = (fat as FieldResult.Valid).value?.times(factor),
            )
            return CustomFoodInput(
                name = form.name,
                per100g = per100g,
                servingLabel = form.servingLabel.takeIf { form.perServing },
                servingGrams = servingGrams.takeIf { form.perServing },
            ) to errors
        }

        /** A serving is multiplied by a quantity; without one the amount is grams. */
        fun parsePortion(serving: FoodServing?, amountInput: String): FieldResult<Portion> = if (serving != null) {
            when (val q = NumberInput.parseDecimal(amountInput, InputLimits.SERVING_QUANTITY, required = true)) {
                is FieldResult.Invalid -> q
                is FieldResult.Valid -> {
                    val quantity = requireNotNull(q.value)
                    FieldResult.Valid(Portion(serving, quantity, serving.grams * quantity))
                }
            }
        } else {
            when (val g = NumberInput.parseDecimal(amountInput, InputLimits.FOOD_GRAMS, required = true)) {
                is FieldResult.Invalid -> g
                is FieldResult.Valid -> FieldResult.Valid(Portion(null, 1.0, requireNotNull(g.value)))
            }
        }
    }
}
