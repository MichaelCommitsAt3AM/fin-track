package com.example.fintrack.presentation.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.core.domain.model.Category
import com.example.fintrack.core.domain.model.CategoryType
import com.example.fintrack.core.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryDetailState(
    val name: String = "",
    val selectedIcon: String = "shopping_cart",
    val selectedColor: String = "#7C5DFA",
    val type: CategoryType = CategoryType.EXPENSE,
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val originalName: String? = null
)

sealed class CategoryDetailEvent {
    object NavigateBack : CategoryDetailEvent()
}

@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(CategoryDetailState())
    val state = _state.asStateFlow()

    private val _eventChannel = Channel<CategoryDetailEvent>()
    val events = _eventChannel.receiveAsFlow()

    // --- ICONS TAILORED FOR EXPENSES ---
    val expenseIcons = listOf(
        "shopping_cart",    // Groceries
        "restaurant",       // Dining
        "directions_bus",   // Transport (commute)
        "home",             // Rent
        "receipt_long",     // Bills
        "movie",            // Entertainment
        "fitness_center",   // Health/Gym
        "flight",           // Travel
        "school",           // Education
        "pets",             // Pets
        "local_gas_station",// Fuel
        "build"             // Maintenance
    )

    // --- ICONS TAILORED FOR INCOME ---
    val incomeIcons = listOf(
        "paid",             // Salary
        "savings",          // Savings
        "trending_up",      // Investments
        "work",             // Freelance/Job
        "card_giftcard",    // Gifts
        "sell",             // Selling items
        "account_balance",  // Bank interest
        "request_quote",    // Invoices
        "currency_exchange",// Dividends/Exchange
        "wallet",           // General
        "redeem",           // Bonus
        "add_business"      // Side hustle
    )

    val availableColors = listOf(
        "#FF6B6B", "#FFD166", "#06D6A0", "#118AB2",
        "#7C5DFA", "#EF476F", "#F7B801", "#4B3F72"
    )

    init {
        val categoryName = savedStateHandle.get<String>("categoryName")
        if (categoryName != null) {
            loadCategory(categoryName)
        }
    }

    private fun loadCategory(name: String) {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { list ->
                val category = list.find { it.name == name }
                if (category != null) {
                    _state.value = _state.value.copy(
                        name = category.name,
                        selectedIcon = category.iconName,
                        selectedColor = category.colorHex,
                        type = category.type,
                        isEditMode = true,
                        originalName = category.name
                    )
                }
            }
        }
    }

    fun onNameChange(name: String) {
        _state.value = _state.value.copy(name = name)
    }

    fun onTypeChange(type: CategoryType) {
        // When switching type, reset to a safe default icon for that type
        val defaultIcon = if (type == CategoryType.EXPENSE) expenseIcons.first() else incomeIcons.first()
        _state.value = _state.value.copy(type = type, selectedIcon = defaultIcon)
    }

    fun onIconSelected(icon: String) {
        _state.value = _state.value.copy(selectedIcon = icon)
    }

    fun onColorSelected(color: String) {
        _state.value = _state.value.copy(selectedColor = color)
    }

    fun onSaveCategory() {
        val currentState = _state.value
        if (currentState.name.isBlank()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            if (currentState.isEditMode && currentState.originalName != currentState.name && currentState.originalName != null) {
                // In a real app, handle renaming (delete old, create new) here
                // For now, we just insert the new version
            }

            val category = Category(
                name = currentState.name,
                iconName = currentState.selectedIcon,
                colorHex = currentState.selectedColor,
                type = currentState.type,
                isDefault = false
            )

            categoryRepository.insertCategory(category)

            _state.value = _state.value.copy(isLoading = false)
            _eventChannel.send(CategoryDetailEvent.NavigateBack)
        }
    }
}