package com.smartsolutions.paquetes.serverApis.models

data class AndroidApp(
    val id: Int,
    val name: String,
    val packageName: String,
    val version: Int,
    val versionName: String,
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