package com.fintrack.app.core.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Defines the "categories" table
@Entity(
    tableName = "categories",
    primaryKeys = ["name", "userId"]
)

data class CategoryEntity(
    val name: String, // e.g., "Food", "Rent"
    val userId: String,
    val iconName: String, // Name of a drawable icon
    val colorHex: String, // Store color as a hex string (e.g., "#FF5733")
    val type: String,
    val isDefault: Boolean = false, // To distinguish default from user-created
    val isSynced: Boolean = false // Track if category has been synced to Firestore
)