package com.smartsolutions.paquetes.serverApis.converters

import com.google.gson.*
import java.lang.reflect.Type

class BooleanConverter : JsonSerializer<Boolean>, JsonDeserializer<Boolean> {
    override fun serialize(value: Boolean?, type: Type?, context: JsonSerializationContext?): JsonElement {
        value?.let { v ->
            return if (v)
                JsonPrimitive(1)
            else
                JsonPrimitive(0)
        }
        return JsonPrimitive(0)
    }

    override fun deserialize(element: JsonElement?, type: Type?, context: JsonDeserializationContext?): Boolean {
        element?.let {
            return try {
                it.asInt == 1
            } catch (e: Exception) {
                it.asBoolean
            }
        }
        return false
    }
}