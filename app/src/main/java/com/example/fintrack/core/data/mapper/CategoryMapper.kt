package com.example.fintrack.core.data.mapper

import com.example.fintrack.core.data.local.model.CategoryEntity
import com.example.fintrack.core.domain.model.Category
import com.example.fintrack.core.domain.model.CategoryType

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