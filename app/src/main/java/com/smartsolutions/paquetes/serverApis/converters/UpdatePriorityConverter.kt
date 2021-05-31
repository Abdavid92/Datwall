package com.smartsolutions.paquetes.serverApis.converters

import com.google.gson.*
import com.smartsolutions.paquetes.serverApis.models.AndroidApp
import java.lang.reflect.Type

class UpdatePriorityConverter : JsonSerializer<AndroidApp.UpdatePriority>, JsonDeserializer<AndroidApp.UpdatePriority> {
    override fun serialize(value: AndroidApp.UpdatePriority?, type: Type?, context: JsonSerializationContext?): JsonElement {
        value?.let {
            return when(it) {
                AndroidApp.UpdatePriority.Low -> JsonPrimitive(0)
                AndroidApp.UpdatePriority.Medium -> JsonPrimitive(1)
                AndroidApp.UpdatePriority.High -> JsonPrimitive(2)
            }
        }
        return JsonPrimitive(0)
    }

    override fun deserialize(element: JsonElement?, type: Type?, context: JsonDeserializationContext?): AndroidApp.UpdatePriority {
        element?.let {
            return when(element.asInt) {
                0 -> AndroidApp.UpdatePriority.Low
                1 -> AndroidApp.UpdatePriority.Medium
                2 -> AndroidApp.UpdatePriority.High
                else -> AndroidApp.UpdatePriority.Low
            }
        }
        return AndroidApp.UpdatePriority.Low
    }
}