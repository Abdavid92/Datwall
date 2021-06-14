package com.smartsolutions.paquetes.serverApis.models

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.smartsolutions.paquetes.serverApis.converters.BooleanConverter
import com.smartsolutions.paquetes.serverApis.converters.DateConverter
import java.sql.Date

/**
 * Aplicación instalada en el dispositivo.
 * */
data class DeviceApp(
    /**
     * Id.
     * */
    var id: String,
    /**
     * Si fue comprada.
     * */
    @JsonAdapter(BooleanConverter::class)
    var purchased: Boolean,
    /**
     * Si ha sido restaurada
     * */
    @JsonAdapter(BooleanConverter::class)
    var restored: Boolean,
    /**
     * Si aún está en período de prueba.
     * */
    @SerializedName("trial_period")
    @JsonAdapter(BooleanConverter::class)
    val trialPeriod: Boolean,
    /**
     * Transacción de compra.
     * */
    var transaction: String?,
    /**
     * Teléfono que compró.
     * */
    var phone: String?,
    /**
     * Si está en espera de confirmación de compra.
     * */
    @SerializedName("waiting_purchase")
    @JsonAdapter(BooleanConverter::class)
    var waitingPurchase: Boolean,
    @SerializedName("device_id")
    /**
     * Id del dispositivo relacionado.
     * */
    var deviceId: String,
    /**
     * Nombre de paquete de la aplicación relacionada.
     * */
    @SerializedName("android_app_package_name")
    var androidAppPackageName: String
) {

    /**
     * Aplicación relacionada.
     * */
    @SerializedName("android_app")
    var androidApp: AndroidApp? = null

    companion object {
        /**
         * Construye un id para un [DeviceApp].
         * */
        fun buildDeviceAppId(packageName: String, deviceId: String) =
            "${packageName}_$deviceId"
    }
}