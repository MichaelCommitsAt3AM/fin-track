package com.fintrack.app.presentation.settings

import com.fintrack.app.core.util.AppLogger
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.core.domain.model.Category
import com.fintrack.app.core.domain.model.CategoryType
import com.fintrack.app.core.domain.repository.CategoryRepository
import com.google.firebase.auth.FirebaseAuth
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
    val originalName: String? = null,
    val error: String? = null // ADD THIS
)

sealed class CategoryDetailEvent {
    object NavigateBack : CategoryDetailEvent()
    data class ShowError(val message: String) : CategoryDetailEvent() // ADD THIS
}

@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val firebaseAuth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(CategoryDetailState())
    val state = _state.asStateFlow()

    private val _eventChannel = Channel<CategoryDetailEvent>()
    val events = _eventChannel.receiveAsFlow()

    val expenseIcons = listOf(
        "shopping_cart", "restaurant", "directions_bus", "home",
        "receipt_long", "movie", "fitness_center", "flight",
        "school", "pets", "local_gas_station", "build"
    )

    val incomeIcons = listOf(
        "paid", "savings", "trending_up", "work",
        "card_giftcard", "sell", "account_balance", "request_quote",
        "currency_exchange", "wallet", "redeem", "add_business"
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
            try {
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
            } catch (e: Exception) {
                AppLogger.e("CategoryDetailVM", "Error loading category: ${e.message}")
                _eventChannel.send(CategoryDetailEvent.ShowError("Failed to load category"))
            }
        }
    }

    fun onNameChange(name: String) {
        _state.value = _state.value.copy(name = name, error = null)
    }

    fun onTypeChange(type: CategoryType) {
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

        // Validate name
        if (currentState.name.isBlank()) {
            viewModelScope.launch {
                _eventChannel.send(CategoryDetailEvent.ShowError("Please enter a category name"))
            }
            return
        }

        // Get userId
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            AppLogger.e("CategoryDetailVM", "User not logged in!")
            viewModelScope.launch {
                _eventChannel.send(CategoryDetailEvent.ShowError("User not logged in. Please log in again."))
            }
            return
        }

        AppLogger.d("CategoryDetailVM", "Saving category: ${currentState.name} for user: $userId")

        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)

                // If editing and name changed, delete old category
                if (currentState.isEditMode &&
                    currentState.originalName != currentState.name &&
                    currentState.originalName != null) {

                    AppLogger.d("CategoryDetailVM", "Deleting old category: ${currentState.originalName}")
                    val oldCategory = Category(
                        name = currentState.originalName,
                        userId = userId,
                        iconName = currentState.selectedIcon,
                        colorHex = currentState.selectedColor,
                        type = currentState.type,
                        isDefault = false
                    )
                    categoryRepository.deleteCategory(oldCategory)
                }

                // Save new/updated category
                val category = Category(
                    name = currentState.name,
                    userId = userId,
                    iconName = currentState.selectedIcon,
                    colorHex = currentState.selectedColor,
                    type = currentState.type,
                    isDefault = false
                )

                AppLogger.d("CategoryDetailVM", "Inserting category: $category")
                categoryRepository.insertCategory(category)

                _state.value = _state.value.copy(isLoading = false)
                AppLogger.d("CategoryDetailVM", "Category saved successfully!")
                _eventChannel.send(CategoryDetailEvent.NavigateBack)

            } catch (e: Exception) {
                AppLogger.e("CategoryDetailVM", "Error saving category: ${e.message}", e)
                _state.value = _state.value.copy(isLoading = false)
                _eventChannel.send(CategoryDetailEvent.ShowError("Failed to save category: ${e.message}"))
            }
        }
    }
}
