package com.smartsolutions.paquetes.helpers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.pow

private val GB = 1024.0.pow(3.0)
private val MB = 1024.0.pow(2.0)

/**
 * Crea un id para un DataPackage
 * */
fun createDataPackageId(name: String, price: Float): String {
    return name.trim() + price.toString()
}

/**
 * Convierte un DataValue en bytes.
 *
 * @param dataValue - DataValue a convertir.
 * */
fun convertToBytes(dataValue: DataValue): Long {
    return when (dataValue.dataUnit) {
        DataUnit.GB -> (dataValue.value * GB).toLong()
        DataUnit.MB -> (dataValue.value * MB).toLong()
        DataUnit.KB -> (dataValue.value * 1024).toLong()
    }
}

/**
 * Procesa y obtiene la unidad más optima para los bytes dados.
 *
 * @param bytes - Bytes que se van a procesar
 * @param unit - Parametro opcional en caso de que se quiera especificar la unidad de medida.
 * */
fun processValue(bytes: Long, dataUnit: DataUnit? = null) : DataValue {

    var unit = dataUnit

    if (unit == null) {
        unit = when {
            GB <= bytes -> {
                DataUnit.GB
            }
            MB <= bytes -> {
                DataUnit.MB
            }
            else -> {
                DataUnit.KB
            }
        }
    }

    val value = when (unit) {
        DataUnit.GB -> {
            bytes / GB
        }
        DataUnit.MB -> {
            bytes / MB
        }
        else -> {
            bytes / 1024.0
        }
    }

    return DataValue(value, unit)
}

/**
 * Unidad que contiene los bytes y la unidad de medida
 * */
data class DataValue(val value : Double, val dataUnit: DataUnit)

/**
 * Unidades de medidas.
 * */
enum class DataUnit {
    /**
     * Kilobytes
     * */
    KB,
    /**
     * Megabytes
     * */
    MB,
    /**
     * Gigabytes
     * */
    GB
}

fun Array<CharSequence>.string(): String {
    var text = ""

    this.forEach {
        text += it
    }

    return text
}