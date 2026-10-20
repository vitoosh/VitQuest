package io.github.vitoosh.vitquest.feature.nutrition.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.core.data.repository.NutritionRepository
import io.github.vitoosh.vitquest.core.data.repository.RecipeRepository
import io.github.vitoosh.vitquest.core.model.MealType
import io.github.vitoosh.vitquest.core.model.Recipe
import io.github.vitoosh.vitquest.core.model.RecipeTag
import io.github.vitoosh.vitquest.core.time.TimeSource
import io.github.vitoosh.vitquest.ui.navigation.RecipeDetailRoute
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecipesUiState(
    val query: String = "",
    val tag: RecipeTag? = null,
    val recipes: List<Recipe> = emptyList(),
    val loading: Boolean = true,
)

@HiltViewModel
class RecipesViewModel @Inject constructor(repository: RecipeRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val tag = MutableStateFlow<RecipeTag?>(null)

    val uiState: StateFlow<RecipesUiState> = combine(repository.recipes(), query, tag) { recipes, q, t ->
        RecipesUiState(query = q, tag = t, recipes = filter(recipes, q, t), loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipesUiState())

    fun onQueryChange(text: String) {
        query.value = text
    }

    fun onTagSelected(value: RecipeTag?) {
        tag.value = value
    }

    companion object {
        /** Matches either language, ingredient names included, so "tempe" finds tempeh dishes. */
        fun filter(recipes: List<Recipe>, query: String, tag: RecipeTag?): List<Recipe> {
            val q = query.trim().lowercase()
            return recipes.filter { recipe ->
                (tag == null || tag in recipe.tags) &&
                    (
                        q.isEmpty() || recipe.nameId.lowercase().contains(q) || recipe.nameEn.lowercase().contains(q) ||
                            recipe.ingredients.any { it.nameId.lowercase().contains(q) || it.nameEn.lowercase().contains(q) }
                        )
            }
        }
    }
}

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val nutritionRepository: NutritionRepository,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val recipeId = savedStateHandle.toRoute<RecipeDetailRoute>().recipeId
    private val _messages = MutableSharedFlow<RecipeMessage>(extraBufferCapacity = 1)
    val messages: SharedFlow<RecipeMessage> = _messages.asSharedFlow()

    val recipe: StateFlow<Recipe?> = recipeRepository.recipe(recipeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val today: LocalDate get() = timeSource.today()

    fun logToDiary(meal: MealType, servings: Double) {
        val current = recipe.value ?: return
        viewModelScope.launch {
            nutritionRepository.logRecipe(timeSource.today(), meal, current, servings)
            _messages.emit(RecipeMessage.Logged)
        }
    }

    fun addToPlan(date: LocalDate, meal: MealType, servings: Double) {
        viewModelScope.launch {
            recipeRepository.addToPlan(date, meal, recipeId, servings)
            _messages.emit(RecipeMessage.Planned)
        }
    }
}

enum class RecipeMessage { Logged, Planned }
