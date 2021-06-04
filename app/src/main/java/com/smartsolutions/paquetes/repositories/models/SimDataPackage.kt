package com.smartsolutions.paquetes.repositories.models

import androidx.room.*
import java.io.Serializable

/**
 * Tabla pivote entre sims y data_packages.
 * */
@Entity(tableName = "sims_data_packages", foreignKeys = [
    ForeignKey(
        entity = Sim::class,
        parentColumns = ["id"],
        childColumns = ["sim_id"],
        onDelete = ForeignKey.CASCADE
    ),
    ForeignKey(
        entity = DataPackage::class,
        parentColumns = ["id"],
        childColumns = ["data_package_id"],
        onDelete = ForeignKey.CASCADE
    )
])
data class SimDataPackage(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    var ussd: String,
    @ColumnInfo(name = "sim_id")
    val simId: String,
    @ColumnInfo(name = "data_package_id")
    val dataPackageId: String
) : Serializable {

    @Ignore
    lateinit var sim: Sim

    @Ignore
    lateinit var dataPackage: DataPackage
}
