package com.fintrack.app.core.domain.use_case

import com.fintrack.app.core.domain.model.Category
import com.fintrack.app.core.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// This use case has one job: get all categories.
class GetCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    operator fun invoke(): Flow<List<Category>> {
        return repository.getAllCategories()
    }
}