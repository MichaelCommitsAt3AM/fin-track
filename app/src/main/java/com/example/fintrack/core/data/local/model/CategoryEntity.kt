package com.example.fintrack.core.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Defines the "categories" table
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val name: String, // e.g., "Food", "Rent"
    val iconName: String, // Name of a drawable icon
    val colorHex: String, // Store color as a hex string (e.g., "#FF5733")
    val type: String,
    val isDefault: Boolean = false // To distinguish default from user-created
)