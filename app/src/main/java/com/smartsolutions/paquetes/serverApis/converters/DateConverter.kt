package com.smartsolutions.paquetes.serverApis.converters

import com.google.gson.*
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

class DateConverter : JsonSerializer<Date>, JsonDeserializer<Date> {

    override fun serialize(
        src: Date,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src.time)
    }

    @ExperimentalStdlibApi
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Date {
        return if (typeOf<Long>().javaType == typeOfT)
            Date(json.asLong)
        else
            try {
                Date(json.asLong)
            } catch (e: Exception) {
                Date(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    .parse(json.asString)?.time ?: System.currentTimeMillis())
            }
    }
}