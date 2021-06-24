package com.smartsolutions.paquetes.helpers

import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.managers.models.DataUnitBytes.Companion.GB
import com.smartsolutions.paquetes.managers.models.DataUnitBytes.Companion.KB
import com.smartsolutions.paquetes.managers.models.DataUnitBytes.Companion.MB

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
fun convertToBytes(dataValue: DataUnitBytes.DataValue): Long {
    return when (dataValue.dataUnit) {
        DataUnitBytes.DataUnit.GB -> (dataValue.value * GB).toLong()
        DataUnitBytes.DataUnit.MB -> (dataValue.value * MB).toLong()
        DataUnitBytes.DataUnit.KB -> (dataValue.value * KB).toLong()
        DataUnitBytes.DataUnit.B -> dataValue.value.toLong()
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

/**
 * Lee la cantidad de bytes de un texto en el que se exprese de
 * la siguiente manera: '1.24 MB' o '1.5 GB'.
 *
 * @param key - Texto que se encuentra justo antes de la cantidad de bytes.
 * @param text - Texto completo donde buscar.
 *
 * @return La cantidad encontrada expresada en bytes o -1 si no es posible
 * obtener la cantidad.
 * */
fun getBytesFromText(key: String, text: String): Long {
    if (!text.contains(key))
        return -1

    val start = text.indexOf(key) + key.length
    var unit: DataUnitBytes.DataUnit = DataUnitBytes.DataUnit.B

    var index = start

    while (index < text.length) {

        when (text[index].toUpperCase()){
            'B' -> {
                unit = DataUnitBytes.DataUnit.B
                break
            }
            'K' -> {
                unit = DataUnitBytes.DataUnit.KB
                break
            }
            'M' -> {
                unit = DataUnitBytes.DataUnit.MB
                break
            }
            'G' -> {
                unit = DataUnitBytes.DataUnit.GB
                break
            }
        }
        index++
    }

    when (text[index].toUpperCase()) {
        'K', 'M', 'G' -> {
            if (text.length < index + 1 || !text[index + 1].toUpperCase().equals('B', true))
                return -1
        }
        'B' -> {
            if (text.length > index + 1 && text[index + 1].isLetter())
                return -1
        }
    }

    return try {
        val value = text.substring(start, index).trimStart().trimEnd().toFloat()
        when (unit) {
            DataUnitBytes.DataUnit.KB -> (value * KB).toLong()
            DataUnitBytes.DataUnit.MB -> (value * MB).toLong()
            DataUnitBytes.DataUnit.GB -> (value * GB).toLong()
            else -> value.toLong()
        }
    } catch (e: Exception) {
        -1
    }
}