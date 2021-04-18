package com.smartsolutions.datwall.jsonAdapters

import com.google.gson.*
import java.lang.reflect.Type

class BooleanAdapter: JsonSerializer<Boolean>, JsonDeserializer<Boolean> {

    override fun serialize(src: Boolean, typeOfSrc: Type?, context: JsonSerializationContext?
    ): JsonElement {
        return if (src) JsonPrimitive(1) else JsonPrimitive(0)
    }

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?
    ): Boolean {
        return json.asInt == 1
    }
}