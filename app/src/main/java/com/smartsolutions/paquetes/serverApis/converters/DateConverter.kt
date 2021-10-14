package com.smartsolutions.paquetes.serverApis.converters

import com.google.gson.*
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

class DateConverter : JsonSerializer<Date>, JsonDeserializer<Date> {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    override fun serialize(
        src: Date,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(dateFormat.format(src))
    }

    @ExperimentalStdlibApi
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Date {
        val date = json.asString

        return Date(dateFormat.parse(date)?.time ?: System.currentTimeMillis())
    }
}