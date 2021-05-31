package com.smartsolutions.paquetes.serverApis.models

import com.google.gson.annotations.SerializedName

data class Device(
    /**
     * Identificador único del dispositivo.
     * */
    var id: String,
    /**
     * Marca.
     * */
    var manufacturer: String,
    /**
     * Modelo.
     * */
    var model: String,
    /**
     * Version del sistema.
     * */
    @SerializedName("sdk_version")
    var sdkVersion: String
)
