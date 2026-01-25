package com.fintrack.app.presentation.settings.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.core.domain.model.Category
import com.fintrack.app.core.domain.model.CategoryType
import com.fintrack.app.core.domain.repository.CategoryRepository
import com.google.firebase.auth.FirebaseAuth // ADD THIS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageCategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val firebaseAuth: FirebaseAuth // ADD THIS
) : ViewModel() {

    // State for the selected tab (Expense vs Income)
    private val _selectedType = MutableStateFlow(CategoryType.EXPENSE)
    val selectedType: StateFlow<CategoryType> = _selectedType

    // State for the search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // The master list of categories, filtered automatically based on type and search
    val categories = combine(
        categoryRepository.getAllCategories(),
        _selectedType,
        _searchQuery
    ) { allCategories, type, query ->
        allCategories.filter { category ->
            // 1. Match the Income/Expense type
            category.type == type &&
                    // 2. Match the search query (case insensitive)
                    category.name.contains(query, ignoreCase = true)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Companion.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onTabSelected(type: CategoryType) {
        _selectedType.value = type
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }

    // Helper to add a category
    fun addCategory(name: String, type: CategoryType, iconName: String, colorHex: String) {
        val userId = firebaseAuth.currentUser?.uid ?: return // ADD THIS

        viewModelScope.launch {
            val newCat = Category(
                name = name,
                userId = userId, // ADD THIS
                iconName = iconName,
                colorHex = colorHex,
                type = type,
                isDefault = false
            )
            categoryRepository.insertCategory(newCat)
        }
    }
}
