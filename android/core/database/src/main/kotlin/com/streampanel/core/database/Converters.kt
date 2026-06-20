package com.streampanel.core.database

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class Converters {
    private val mapSerializer = MapSerializer(String.serializer(), String.serializer())
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String = json.encodeToString(mapSerializer, value)

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> =
        if (value.isBlank()) emptyMap() else json.decodeFromString(mapSerializer, value)
}
