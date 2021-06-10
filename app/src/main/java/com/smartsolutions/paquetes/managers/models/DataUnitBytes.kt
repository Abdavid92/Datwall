package com.smartsolutions.paquetes.managers.models

import com.smartsolutions.paquetes.helpers.convertToBytes
import kotlin.math.pow

class DataUnitBytes(val bytes: Long) {

    /**
     * Procesa y obtiene la unidad más optima para los bytes dados.
     *
     * @param bytes - Bytes que se van a procesar
     * @param dataUnit - Parametro opcional en caso de que se quiera especificar la unidad de medida.
     * */
    fun getValue(bytes: Long, dataUnit: DataUnit? = null) : DataValue {

        var unit = dataUnit

        if (unit == null) {
            unit = when {
                bytes >= GB -> {
                    DataUnit.GB
                }
                bytes >= MB -> {
                    DataUnit.MB
                }
                bytes >= KB -> {
                    DataUnit.KB
                }
                else -> {
                    DataUnit.B
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
            DataUnit.KB -> {
                bytes / KB
            }
            else -> {
                bytes.toDouble()
            }
        }

        return DataValue(value, unit)
    }

    companion object {

        const val GB = 1073741824.0 //1024.0.pow(3.0)
        const val MB = 1048576.0 //1024.0.pow(2.0)
        const val KB = 1024.0
    }

    /**
     * Unidad que contiene los bytes y la unidad de medida.
     *
     * @constructor
     * @param value - Bytes relativos a la unidad de medida.
     * Por ejemplo: 2.5 GB que serían 2684354560 bytes.
     *
     * @param dataUnit - Unidad de medida en la que están expresadas los bytes.
     * */
    data class DataValue(val value: Double, val dataUnit: DataUnit) {

        /**
         * Retorna el valor de este [DataValue] llevado a bytes,
         * la unidad de medida más pequeña.
         * */
        fun toBytes(): Long {
            return convertToBytes(this)
        }
    }

    /**
     * Unidades de medidas.
     * */
    enum class DataUnit {
        /**
         * Bytes
         * */
        B,
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
}