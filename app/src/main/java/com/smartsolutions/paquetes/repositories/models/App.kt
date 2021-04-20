package com.smartsolutions.paquetes.repositories.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.smartsolutions.paquetes.managers.models.Traffic
import kotlinx.parcelize.Parcelize

/**
 * Representa una aplicación guardada en base de datos.
 * */
@Parcelize
@Entity(tableName = "apps")
class App(
    /**
     * Nombre de paquete
     * */
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    var packageName: String,
    /**
     * Identificador único (uid)
     * */
    override var uid: Int,
    /**
     * Nombre de la aplicación
     * */
    override var name: String,
    /**
     * Número de versión
     * */
    var version: Long,
    /**
     * Acceso permanente
     * */
    override var access: Boolean,
    /**
     * Acceso en primer plano
     * */
    @ColumnInfo(name = "foreground_access")
    var foregroundAccess: Boolean,
    /**
     * Acceso temporal
     * */
    @ColumnInfo(name = "temp_access")
    var tempAccess: Boolean,
    /**
     * Indica si tiene el permiso de internet
     * */
    var internet: Boolean,
    /**
     * Indica si es ejecutable
     * */
    var executable: Boolean,
    /**
     * Indica si el modo dinámico del firewall puede preguntar por
     * esta aplicación cuando entre en primer plano y no
     * tenge acceso permanente o acseso en primer plano.
     * */
    var ask: Boolean,
    /**
     * Indica si es una aplicación de consumo nacional
     * */
    var national: Boolean,
    /**
     * Anotación de advertencia que se muestra cuando se intenta conceder
     * el acceso permanente.
     * */
    @ColumnInfo(name = "allow_annotations")
    override var allowAnnotations: String?,
    /**
     * Anotación de advertencia que se muestra cuando se intenta bloquear
     * el acceso permanente.
     * */
    @ColumnInfo(name = "blocked_annotations")
    override var blockedAnnotations: String?,
    /**
     * Tráfico que ha consumido esta aplicación en un espacio de tiempo.
     * */
    @Ignore
    var traffic: Traffic?
) : IApp {


    constructor(): this(
        "",
        0,
        "",
        0,
        false,
        false,
        false,
        false,
        true,
        true,
        false,
        null,
        null,
        null
    )

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + uid
        result = 31 * result + version.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as App

        if (packageName != other.packageName) return false
        if (uid != other.uid) return false
        if (version != other.version) return false

        return true
    }

    /**
     * @return Un número construido basandose en el acceso permanente (access) y el acceso temporal (tempAccess)
     * */
    override fun accessHashCode(): Long {
        val access = if (this.access) 1 else 0
        val tempAccess = if (this.access) 1 else 0

        return "$access$tempAccess".toLong()
    }
}