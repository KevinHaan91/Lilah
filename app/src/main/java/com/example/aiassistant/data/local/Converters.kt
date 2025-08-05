package com.example.aiassistant.data.local

import androidx.room.TypeConverter
import com.example.aiassistant.data.models.LLMType

class Converters {
    @TypeConverter
    fun fromLLMType(type: LLMType): String {
        return type.name
    }

    @TypeConverter
    fun toLLMType(type: String): LLMType {
        return LLMType.valueOf(type)
    }
}

