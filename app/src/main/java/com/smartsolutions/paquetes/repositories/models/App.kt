package com.smartsolutions.paquetes.repositories.models

import androidx.room.*
import com.smartsolutions.paquetes.managers.models.Traffic
import kotlinx.parcelize.Parcelize

/**
 * Representa una aplicación guardada en base de datos.
 * */
@Parcelize
@Entity(tableName = "apps")
@TypeConverters(App.TrafficTypeConverter::class)
class App(
    /**
     * Nombre de paquete
     * */
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    override var packageName: String,
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
     * Indica si este IApp pertenece al sistema.
     * */
    override var system: Boolean,
    /**
     * Indica si el modo dinámico del firewall puede preguntar por
     * esta aplicación cuando entre en primer plano y no
     * tiene acceso permanente o acceso en primer plano.
     * */
    var ask: Boolean,
    /**
     * Tipo de tráfico de la aplicación (Por defecto International)
     * */
    @ColumnInfo(name = "traffic_type")
    var trafficType: TrafficType,
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
        false,
        true,
        TrafficType.International,
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
        val tempAccess = if (this.tempAccess) 1 else 0

        return "$access$tempAccess".toLong()
    }

    override fun toString(): String {
        return "$name - $packageName"
    }

    class TrafficTypeConverter {

        @TypeConverter
        fun fromTrafficType(trafficType: TrafficType): String =
            trafficType.name

        @TypeConverter
        fun toTrafficType(name: String): TrafficType =
            TrafficType.valueOf(name)
    }
}

enum class TrafficType {
    International,
    National,
    Free
}