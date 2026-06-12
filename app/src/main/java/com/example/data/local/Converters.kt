package com.example.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromListString(list: List<String>?): String {
        if (list == null) return ""
        return list.joinToString(",")
    }

    @TypeConverter
    fun toListString(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(",")
    }
}
