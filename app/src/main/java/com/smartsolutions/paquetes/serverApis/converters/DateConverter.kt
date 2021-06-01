package com.smartsolutions.paquetes.serverApis.converters

import com.google.gson.*
import java.lang.reflect.Type
import java.sql.Date

class DateConverter : JsonSerializer<Date>, JsonDeserializer<Date> {

    override fun serialize(
        src: Date,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src.time)
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Date {
        return Date(json.asLong)
    }
}