package io.github.vitoosh.vitquest.feature.nutrition.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vitoosh.vitquest.core.data.repository.RecipeRepository
import io.github.vitoosh.vitquest.core.model.MealPlanEntry
import io.github.vitoosh.vitquest.core.model.ShoppingItem
import io.github.vitoosh.vitquest.core.time.TimeSource
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanUiState(
    val today: LocalDate,
    val selectedDate: LocalDate,
    /** The coming seven days, today first. */
    val days: List<LocalDate>,
    val entries: List<MealPlanEntry>,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlanViewModel @Inject constructor(
    private val repository: RecipeRepository,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val today = MutableStateFlow(timeSource.today())
    private val selected = MutableStateFlow(timeSource.today())
    private val _generated = MutableSharedFlow<Int>(extraBufferCapacity = 1)

    /** Emits the number of shopping lines after generating the list. */
    val generated: SharedFlow<Int> = _generated.asSharedFlow()

    val uiState: StateFlow<PlanUiState?> = today.flatMapLatest { start ->
        combine(repository.plan(start, start.plusDays(PLAN_DAYS - 1)), selected) { entries, date ->
            PlanUiState(
                today = start,
                selectedDate = date,
                days = (0 until PLAN_DAYS).map { start.plusDays(it) },
                entries = entries,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectDate(date: LocalDate) {
        selected.value = date
    }

    fun remove(entry: MealPlanEntry) {
        viewModelScope.launch { repository.removeFromPlan(entry.id) }
    }

    fun generateShoppingList() {
        val start = today.value
        viewModelScope.launch { _generated.emit(repository.generateShoppingList(start, start.plusDays(PLAN_DAYS - 1))) }
    }

    private companion object {
        const val PLAN_DAYS = 7L
    }
}

@HiltViewModel
class ShoppingListViewModel @Inject constructor(private val repository: RecipeRepository) : ViewModel() {

    val items: StateFlow<List<ShoppingItem>?> = repository.shoppingList()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun add(name: String) {
        viewModelScope.launch { repository.addShoppingItem(name) }
    }

    fun toggle(item: ShoppingItem) {
        viewModelScope.launch { repository.setShoppingItemChecked(item.id, !item.checked) }
    }

    fun delete(item: ShoppingItem) {
        viewModelScope.launch { repository.deleteShoppingItem(item.id) }
    }

    fun clearChecked() {
        viewModelScope.launch { repository.clearCheckedShoppingItems() }
    }
}
