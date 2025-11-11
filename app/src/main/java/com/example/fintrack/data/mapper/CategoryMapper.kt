package com.example.fintrack.data.mapper

import com.example.fintrack.data.local.model.CategoryEntity
import com.example.fintrack.domain.model.Category

// --- Category Mappers ---

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        name = this.name,
        iconName = this.iconName,
        colorHex = this.colorHex,
        isDefault = this.isDefault
    )
}

fun CategoryEntity.toDomain(): Category {
    return Category(
        name = this.name,
        iconName = this.iconName,
        colorHex = this.colorHex,
        isDefault = this.isDefault
    )
}