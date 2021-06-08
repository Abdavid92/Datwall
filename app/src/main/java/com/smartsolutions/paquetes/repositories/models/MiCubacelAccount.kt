package com.smartsolutions.paquetes.repositories.models

import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.Serializable

/**
 * Cuenta de mi.cubacel.net
 * */
@Entity(
    tableName = "mi_cubacel_accounts",
    foreignKeys = [
        ForeignKey(
            entity = Sim::class,
            parentColumns = ["id"],
            childColumns = ["sim_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@TypeConverters(MiCubacelAccount.MapConverter::class)
data class MiCubacelAccount(
    /**
     * Id de la sim al que pertenece la cuenta.
     * */
    @PrimaryKey
    @ColumnInfo(name = "sim_id")
    var simId: String,
    /**
     * Número de teléfono.
     * */
    var phone: String,
    /**
     * Contraseña.
     * */
    var password: String,
    /**
     * Cookies de inicio de sesión.
     * */
    var cookies: Map<String, String>
): Serializable {

    @Ignore
    lateinit var sim: Sim

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MiCubacelAccount

        if (simId != other.simId) return false
        if (phone != other.phone) return false
        if (password != other.password) return false

        return true
    }

    override fun hashCode(): Int {
        var result = simId.hashCode()
        result = 31 * result + phone.hashCode()
        result = 31 * result + password.hashCode()
        return result
    }

    class MapConverter {

        private val gson = Gson()

        @TypeConverter
        fun toMap(json: String): Map<String, String> {
            val typeToken = object : TypeToken<Map<String, String>>() {}.type
            return gson.fromJson(json, typeToken)
        }

        @TypeConverter
        fun fromMap(map: Map<String, String>): String {
            return gson.toJson(map)
        }
    }
}
