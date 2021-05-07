package com.smartsolutions.paquetes.managers.models

import com.smartsolutions.paquetes.helpers.GB
import com.smartsolutions.paquetes.helpers.MB

class DataBytes(val bytes: Long) {

    fun getDataValue(dataUnit: DataUnit? = null) = processValue(bytes, dataUnit)

    /**
     * Procesa y obtiene la unidad más optima para los bytes dados.
     *
     * @param bytes - Bytes que se van a procesar
     * @param dataUnit - Parametro opcional en caso de que se quiera especificar la unidad de medida.
     * */
    private fun processValue(bytes: Long, dataUnit: DataUnit? = null) : DataValue {

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
     * Unidad que contiene los bytes y la unidad de medida.
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
}