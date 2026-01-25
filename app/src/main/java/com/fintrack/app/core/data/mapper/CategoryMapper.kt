package com.fintrack.app.core.data.mapper

import com.fintrack.app.core.data.local.model.CategoryEntity
import com.fintrack.app.core.domain.model.Category
import com.fintrack.app.core.domain.model.CategoryType

// --- Category Mappers ---

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        name = this.name,
        userId = this.userId,
        iconName = this.iconName,
        colorHex = this.colorHex,
        type = this.type.name,
        isDefault = this.isDefault
    )
}

fun CategoryEntity.toDomain(): Category {
    return Category(
        name = this.name,
        userId = this.userId,
        iconName = this.iconName,
        colorHex = this.colorHex,
        type = CategoryType.valueOf(this.type),
        isDefault = this.isDefault
    )
}