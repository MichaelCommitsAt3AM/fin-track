package com.example.fintrack.core.data.local

import androidx.room.TypeConverter

class Converters {

    // Tells Room how to convert a List<String> to a single String for storage
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        // We can join the list with a comma. e.g., ["tag1", "tag2"] -> "tag1,tag2"
        return value?.joinToString(",")
    }

    // Tells Room how to convert a String from the database back to a List<String>
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        // We split the string by the comma
        return value?.split(",")?.map { it.trim() }
    }
}