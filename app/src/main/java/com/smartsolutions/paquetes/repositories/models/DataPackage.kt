package com.smartsolutions.paquetes.repositories.models

import androidx.annotation.StringDef
import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.IgnoredOnParcel

/**
 * Entidad correspondiente a la tabla data_packages
 * */
@Entity(tableName = "data_packages")
data class DataPackage(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val price: Float,
    val bytes: Long,
    @ColumnInfo(name = "bytes_lte")
    val bytesLte: Long,
    @ColumnInfo(name = "bonus_bytes")
    @SerializedName("bonus_bytes")
    val bonusBytes: Long,
    @ColumnInfo(name = "bonus_cu_bytes")
    @SerializedName("bonus_cu_bytes")
    val nationalBytes: Long,
    @Networks
    val network: String,
    val index: Int,
    /*@ColumnInfo(name = "ussd_sim_1")
    var ussdSim1: String? = null,
    @ColumnInfo(name = "ussd_sim_2")
    var ussdSim2: String? = null,
    @ColumnInfo(name = "active_in_sim_1")
    var activeInSim1: Boolean = false,
    @ColumnInfo(name = "active_in_sim_2")
    var activeInSim2: Boolean = false,*/
    var deprecated: Boolean = false
) {
    @Ignore
    var ussd: String? = null

    @Ignore
    var url: String? = null

    @Ignore
    var sims = emptyList<Sim>()

    companion object {
        const val NETWORK_3G_4G = "3G_4G"
        const val NETWORK_4G = "4G"
    }

    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(NETWORK_3G_4G, NETWORK_4G)
    annotation class Networks
}