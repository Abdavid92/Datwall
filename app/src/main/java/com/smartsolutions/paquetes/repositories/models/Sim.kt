package com.smartsolutions.paquetes.repositories.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlin.reflect.KProperty

@Entity(tableName = "sims")
data class Sim(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "setup_date")
    val setupDate: Long
) {

    @Ignore
    var packages = emptyList<DataPackage>()

    @Ignore
    var miCubacelAccount: MiCubacelAccount? = null
}