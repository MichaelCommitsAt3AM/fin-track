package com.example.fintrack.core.data.local

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
        // Handle null or blank strings - return null instead of a list with empty string
        return value?.takeIf { it.isNotBlank() }?.split(",")?.map { it.trim() }
    }

    // LocalDateTime converters
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
    }
}