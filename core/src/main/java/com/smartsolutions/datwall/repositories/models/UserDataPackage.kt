package com.smartsolutions.datwall.repositories.models

import android.os.Parcelable
import androidx.room.*
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Paquete de datos que ha sido adquirido por el usuario.
 * */
@Parcelize
@Entity(tableName = "user_data_package", foreignKeys = [ForeignKey(
    entity = DataPackage::class,
    parentColumns = ["id"],
    childColumns = ["data_package_id"]
)], indices = [
    Index("data_package_id")
])
data class UserDataPackage(
    /**
     * Id auto-incrementable que se usa para
     * identificar el paquete en base de datos.
     * */
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    /**
     * Cantidad de bytes que dispone el paquete actualmente.
     * */
    val bytes: Long,
    @ColumnInfo(name = "bonus_bytes")
    /**
     * Bono en bytes
     * */
    val bonusBytes: Long,
    @ColumnInfo(name = "bonus_cu_bytes")
    /**
     * Bono nacional en bytes
     * */
    val bonusCuBytes: Long,
    /**
     * Fecha en que se empezó a usar.
     * */
    val start: Long,
    /**
     * Fecha de expiración.
     * */
    val finish: Long,
    /**
     * Indica si el paquete está activo actualmente.
     * */
    val active: Boolean,
    /**
     * Índice de la linea por la que fue adquirido
     * el paquete.
     * */
    val simIndex: Int,
    /**
     * Clave foránea que enlaza este paquete con uno
     * de los tipos de paquetes que están o estuvieron
     * disponibles.
     * */
    @ColumnInfo(name = "data_package_id")
    val dataPackageId: Int,
): Parcelable {

    /**
     * Relación foranea con una instancia de DataPackage.
     * */
    @Ignore
    @IgnoredOnParcel
    var dataPackage: DataPackage? = null
}
