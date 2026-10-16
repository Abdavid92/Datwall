package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.helpers.DateCalendarUtils
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import java.util.Calendar
import java.util.Date

/**
 * Reglas de la edición manual de los datos de una línea.
 * */
object UserDataBytesEditor {

    /**
     * Datos que se pueden agregar: los que no existen o ya vencieron.
     * */
    fun addable(data: List<UserDataBytes>): List<UserDataBytes> =
        data.filter { !it.exists() || it.isExpired() }

    /**
     * Vencimiento al final del día elegido.
     * */
    fun expiryOf(year: Int, month: Int, dayOfMonth: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month, dayOfMonth)
        return DateCalendarUtils.getLastHour(Date(cal.timeInMillis)).time
    }

    /**
     * Copia de [data] con los valores escritos por el usuario. Un valor vacío es 0. Devuelve
     * null si algún valor no es un número válido. El inicial nunca queda por debajo del
     * restante, y si no se eligió fecha se conserva la actual.
     * */
    fun edit(
        data: UserDataBytes,
        rest: String?,
        restUnit: DataUnitBytes.DataUnit,
        initial: String?,
        initialUnit: DataUnitBytes.DataUnit,
        expiredTime: Long?
    ): UserDataBytes? {
        val bytes = toBytes(rest, restUnit) ?: return null
        val initialBytes = toBytes(initial, initialUnit) ?: return null

        return UserDataBytes(
            data.simId,
            data.type,
            maxOf(initialBytes, bytes),
            bytes,
            data.startTime,
            expiredTime ?: data.expiredTime
        )
    }

    private fun toBytes(value: String?, unit: DataUnitBytes.DataUnit): Long? {
        if (value.isNullOrBlank())
            return 0L

        val number = value.trim().toDoubleOrNull() ?: return null
        if (number < 0 || number.isNaN() || number.isInfinite())
            return null

        return DataUnitBytes.DataValue(number, unit).toBytes()
    }
}
