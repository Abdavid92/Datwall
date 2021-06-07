package com.smartsolutions.paquetes.helpers

import com.smartsolutions.paquetes.managers.models.DataBytes
import com.smartsolutions.paquetes.managers.models.DataBytes.Companion.GB
import com.smartsolutions.paquetes.managers.models.DataBytes.Companion.KB
import com.smartsolutions.paquetes.managers.models.DataBytes.Companion.MB
import kotlin.math.pow

/**
 * Contruye el código ussd para comprar un
 * paquete de datos.
 *
 * @param index - Índice en donde esta el tipo de paquete (si es 3G o 4G).
 * @param dataPackageIndex - Índice en donde está el paquete. Si este parámetro
 * es -1 se considera que se está construyendo el código ussd de la bolsa diaria
 * y por lo tanto el resultado será diferente.
 * */
fun buildDataPackageUssdCode(index: Int, dataPackageIndex: Int): String {
    return if (dataPackageIndex != -1)
        "*133*1*$index*$dataPackageIndex#"
    else
        "*133*1*$index#"
}

/**
 * Crea un id para un DataPackage.
 * */
fun createDataPackageId(name: String, price: Float): String {
    return name.trim() + price.toString()
}

/**
 * Convierte un DataValue en bytes.
 *
 * @param dataValue - DataValue a convertir.
 * */
fun convertToBytes(dataValue: DataBytes.DataValue): Long {
    return when (dataValue.dataUnit) {
        DataBytes.DataUnit.GB -> (dataValue.value * GB).toLong()
        DataBytes.DataUnit.MB -> (dataValue.value * MB).toLong()
        DataBytes.DataUnit.KB -> (dataValue.value * KB).toLong()
        DataBytes.DataUnit.B -> dataValue.value.toLong()
    }
}

/**
 * Método de extensión que itera por todos los
 * items del arreglo y los concatena en un String.
 * */
fun Array<CharSequence>.string(): String {
    var text = ""

    this.forEach {
        text += it
    }

    return text
}