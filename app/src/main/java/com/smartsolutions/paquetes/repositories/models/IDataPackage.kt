package com.smartsolutions.paquetes.repositories.models

import android.os.Parcelable
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.data.DataPackages

interface IDataPackage {
    /**
     * Id del paquete.
     * */
    val id: DataPackages.PackageId

    /**
     * Nombre.
     * */
    val name: String

    /**
     * Descripción.
     * */
    val description: String

    /**
     * Precio de compra.
     * */
    val price: Float

    /**
     * Bytes disponibles en todas la redes.
     * */
    val bytes: Long

    /**
     * Bytes disponibles en la red 4G
     * */
    val bytesLte: Long

    /**
     * Bytes de navegación nacional.
     * */
    val nationalBytes: Long

    /**
     * Redes en la que está disponible este paquete.
     * */
    @Networks
    val network: String

    /**
     * Índice en el menu de compra. Esto se usa para contruir el
     * código ussd.
     * */
    val index: Int

    /**
     * Duración en dias.
     * */
    val duration: Int

    /**
     * Clave que identifica el paquete en el sms de compra.
     * */
    val smsKey: String

    /**
     * Indica si este paquete está obsoleto.
     * */
    var deprecated: Boolean

    /**
     * Minutos que incluye si es un plan combinado
     */
    val minutes: Int

    /**
     * SMS que incluye si es un plan combinado
     */
    val sms: Int
}