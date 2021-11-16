package com.smartsolutions.paquetes.repositories.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/*@Entity(tableName = "events")
@TypeConverters(Event.EventTypeConverter::class)
data class Event(
    @PrimaryKey
    val date: Long,
    val type: EventType,
    val title: String,
    val message: String
) {
    class EventTypeConverter {
        @TypeConverter
        fun toType(value: String): EventType =
           EventType.valueOf(value)

        @TypeConverter
        fun fromType(type: EventType): String =
            type.name
    }

    enum class EventType{
        ERROR,
        INFO,
        WARNING
    }
}*/