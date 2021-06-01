package com.smartsolutions.paquetes.serverApis.models

import com.google.gson.annotations.JsonAdapter
import com.smartsolutions.paquetes.serverApis.converters.UpdatePriorityConverter

data class AndroidApp(
    val id: Int,
    val name: String,
    val packageName: String,
    val version: Int,
    val versionName: String,
    @JsonAdapter(UpdatePriorityConverter::class)
    val updatePriority: UpdatePriority,
    val updateComments: String,
    val key: String,
    val status: String
) {

    enum class UpdatePriority {
        Low,
        Medium,
        High
    }
}