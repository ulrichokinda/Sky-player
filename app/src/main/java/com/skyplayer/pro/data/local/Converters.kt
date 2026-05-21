package com.skyplayer.pro.data.local

import androidx.room.TypeConverter
import com.skyplayer.pro.data.model.ContentType
import com.skyplayer.pro.data.model.SourceType

/**
 * Convertisseurs Room pour les enums et types complexes
 */
class Converters {
    
    // ContentType converters
    @TypeConverter
    fun fromContentType(value: ContentType): String {
        return value.name
    }
    
    @TypeConverter
    fun toContentType(value: String): ContentType {
        return ContentType.valueOf(value)
    }
    
    // SourceType converters
    @TypeConverter
    fun fromSourceType(value: SourceType): String {
        return value.name
    }
    
    @TypeConverter
    fun toSourceType(value: String): SourceType {
        return SourceType.valueOf(value)
    }
    
    // List<String> converters pour les catégories
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }
    
    @TypeConverter
    fun toStringList(value: String?): List<String> {
        return value?.split(",") ?: emptyList()
    }
}
