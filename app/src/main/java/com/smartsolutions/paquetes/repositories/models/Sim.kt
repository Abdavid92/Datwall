package com.smartsolutions.paquetes.repositories.models

import android.graphics.Bitmap
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.smartsolutions.paquetes.annotations.Networks
import kotlin.reflect.KProperty

/**
 * Tabla de las lineas instaladas en el dispositivo.
 * */
@Entity(tableName = "sims")
data class Sim(
    /**
     * Id de la linea. Puede ser el cardId(android 30 o superior) o
     * el iccId(android 29 o menor) del subscritionInfo
     * utilizado para obtener la linea.
     * */
    @PrimaryKey
    val id: String,
    /**
     * Fecha en la que se configuró la linea para obtener los tipos de
     * redes disponibles para ella. Si su valor es cero significa
     * que no se ha configurado.
     * */
    @ColumnInfo(name = "setup_date")
    var setupDate: Long,
    /**
     * Tipos de redes disponibles para la linea.
     * */
    @Networks
    var network: String
) {

    /**
     * Indica si esta linea es la predeterminada para llamadas.
     * */
    @ColumnInfo(name = "default_voice")
    var defaultVoice: Boolean = false

    /**
     * Indica si esta linea es la predeterminada para los datos.
     * */
    @ColumnInfo(name = "default_data")
    var defaultData: Boolean = false

    /**
     * Número de teléfono Si es null significa que no está disponible.
     * */
    var phone: String? = null

    /**
     * Ícono de la linea.
     * */
    @Ignore
    var icon: Bitmap? = null

    /**
     * Paquetes disponibles para esta linea.
     * */
    @Ignore
    var packages = emptyList<DataPackage>()

    /**
     * Cuenta de mi.cubacel.net. Si es null significa que no tiene cuenta.
     * */
    @Ignore
    var miCubacelAccount: MiCubacelAccount? = null
}