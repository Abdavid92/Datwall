package com.smartsolutions.paquetes.serverApis.models

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.smartsolutions.paquetes.serverApis.converters.BooleanConverter
import com.smartsolutions.paquetes.serverApis.converters.DateConverter
import org.apache.commons.lang.time.DateUtils
import java.util.*

/**
 * Aplicación instalada en el dispositivo.
 * */
data class DeviceApp(
    /**
     * Id.
     * */
    @SerializedName("id")
    var id: String,

    /**
     * Si fue comprada.
     * */
    @SerializedName("purchased")
    @JsonAdapter(BooleanConverter::class)
    var purchased: Boolean,

    /**
     * Si ha sido restaurada
     * */
    @SerializedName("restored")
    @JsonAdapter(BooleanConverter::class)
    var restored: Boolean,

    /**
     * Si aún está en período de prueba. Esta propiedad
     * no es segura.
     * */
    @Deprecated(
        "Esta propiedad no es segura",
        replaceWith = ReplaceWith("inTrialPeriod()")
    )
    @SerializedName("trial_period")
    @JsonAdapter(BooleanConverter::class)
    val trialPeriod: Boolean,

    /**
     * Última consulta.
     * */
    @SerializedName("last_query")
    @JsonAdapter(DateConverter::class)
    val lastQuery: Date,

    /**
     * Transacción de compra.
     * */
    @SerializedName("transaction")
    var transaction: String?,

    /**
     * Teléfono que compró.
     * */
    @SerializedName("phone")
    var phone: String?,

    /**
     * Si está en espera de confirmación de compra.
     * */
    @SerializedName("waiting_purchase")
    @JsonAdapter(BooleanConverter::class)
    var waitingPurchase: Boolean,

    /**
     * Id del dispositivo relacionado.
     * */
    @SerializedName("device_id")
    val deviceId: String,

    /**
     * Nombre de paquete de la aplicación relacionada.
     * */
    @SerializedName("android_app_package_name")
    val androidAppPackageName: String,

    /**
     * Fecha en la que se registró esta aplicación en el dispositivo.
     * Esta fecha se usa para verificar el periodo de prueba y los
     * días restantes de prueba.
     * */
    @SerializedName("created_at")
    @JsonAdapter(DateConverter::class)
    val createdAt: Date
) {

    /**
     * Aplicación relacionada.
     * */
    @SerializedName("android_app")
    lateinit var androidApp: AndroidApp

    /**
     * Indica si está en periodo de prueba.
     * */
    fun inTrialPeriod(): Boolean {
        val days = daysInUse()

        val trialPeriod = androidApp.trialPeriod

        return days <= trialPeriod
    }

    /**
     * Obtiene la cantidad de días que se ha usado la aplicación en el dispositivo.
     * */
    fun daysInUse() =
        ((System.currentTimeMillis() - createdAt.time) / DateUtils.MILLIS_PER_DAY).toInt()

    companion object {
        /**
         * Construye un id para un [DeviceApp].
         * */
        fun buildDeviceAppId(packageName: String, deviceId: String) =
            "${packageName}_$deviceId"
    }
}