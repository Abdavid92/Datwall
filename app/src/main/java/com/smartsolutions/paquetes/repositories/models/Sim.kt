package com.smartsolutions.paquetes.repositories.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.smartsolutions.paquetes.annotations.Networks
import kotlin.reflect.KProperty

@Entity(tableName = "sims")
data class Sim(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "setup_date")
    val setupDate: Long,
    @Networks
    val networks: Networks
) {

    @ColumnInfo(name = "index_3g")
    var index3G: Int = -1

    @ColumnInfo(name = "index_4g")
    var index4G: Int = -1

    @Ignore
    var packages = emptyList<DataPackage>()

    @Ignore
    var miCubacelAccount: MiCubacelAccount? = null
}