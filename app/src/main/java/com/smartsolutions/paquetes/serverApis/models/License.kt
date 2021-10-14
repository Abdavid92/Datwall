package com.smartsolutions.paquetes.serverApis.models

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.smartsolutions.paquetes.serverApis.converters.BooleanConverter
import com.smartsolutions.paquetes.serverApis.converters.DateConverter
import org.apache.commons.lang.time.DateUtils
import java.util.*

/**
 * Licencia de la aplicación.
 * */
data class License(
    /**
     * Id del dispositivo.
     * */
    @SerializedName("device_id")
    val deviceId: String,

    /**
     * Si fue comprada.
     * */
    @SerializedName("is_purchased")
    @JsonAdapter(BooleanConverter::class)
    var isPurchased: Boolean,

    /**
     * Si ha sido restaurada.
     * */
    @SerializedName("is_restored")
    @JsonAdapter(BooleanConverter::class)
    var isRestored: Boolean,

    /**
     * Marca.
     * */
    @SerializedName("manufacturer")
    val manufacturer: String,

    /**
     * Modelo.
     * */
    @SerializedName("model")
    val model: String,

    /**
     * Transacción de la compra.
     * */
    @SerializedName("transaction")
    var transaction: String?,

    /**
     * Número de teléfono que realizó la compra.
     * */
    @SerializedName("phone")
    var phone: String?,

    /**
     * Fecha de creación.
     * */
    @SerializedName("created_at")
    @JsonAdapter(DateConverter::class)
    val createdAt: Date,

    @SerializedName("last_query")
    @JsonAdapter(DateConverter::class)
    val lastQuery: Date,

    /**
     * Relación foránea.
     * */
    @SerializedName("package_name")
    var packageName: String?
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
}
