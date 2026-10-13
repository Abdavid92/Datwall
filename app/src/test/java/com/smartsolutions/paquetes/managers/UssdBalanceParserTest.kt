package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.managers.models.DataUnitBytes.DataUnit
import com.smartsolutions.paquetes.managers.models.DataUnitBytes.DataValue
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.DataBytes.DataType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Textos reales de ETECSA (spec.md, "Ejemplos reales").
 * */
class UssdBalanceParserTest {

    // 2026-10-13 10:00:00
    private val now = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.OCTOBER, 13, 10, 0, 0)
    }.timeInMillis

    private fun mb(value: Double) = DataValue(value, DataUnit.MB).toBytes()

    private fun endOfDay(year: Int, month: Int, day: Int) = Calendar.getInstance().apply {
        clear()
        set(year, month, day, 23, 59, 59)
    }.timeInMillis

    private fun List<DataBytes>.of(type: DataType) = firstOrNull { it.type == type }

    @Test
    fun noActivePlan() {
        val data = UssdBalanceParser.parsePackages(
            "Tarifa: No activa. Ud debe adquirir una oferta. Para una nueva compra marque *133#",
            now
        )

        assertTrue(data.all { it.bytes == 0L })
    }

    @Test
    fun allNetworksDataWithDaysLeft() {
        val data = UssdBalanceParser.parsePackages(
            "Tarifa: No activa. Datos: 130.08 MB validos 24 dias.",
            now
        )

        val international = data.of(DataType.International)!!
        assertEquals(mb(130.08), international.bytes)
        assertEquals(endOfDay(2026, Calendar.NOVEMBER, 6), international.expiredTime)
    }

    @Test
    fun unusedDailyPlanHasNoExpiration() {
        val data = UssdBalanceParser.parsePackages(
            "Tarifa: No activa. Diaria: 200 MB no activos.",
            now
        )

        val daily = data.of(DataType.DailyBag)!!
        assertEquals(mb(200.0), daily.bytes)
        assertEquals(0L, daily.expiredTime)
        assertNull(data.of(DataType.International))
    }

    @Test
    fun businessLineBalance() {
        // *111# de una línea empresarial: sin vigencia.
        val data = UssdBalanceParser.parsePackages(
            "Voz: 148 MIN. SMS: 8. Datos: 16700 MB. Roaming:",
            now
        )

        val international = data.of(DataType.International)!!
        assertEquals(mb(16700.0), international.bytes)
        assertEquals(0L, international.expiredTime)
    }

    @Test
    fun nationalBonus() {
        val data = UssdBalanceParser.parseBonus("Datos.cu 273 MB vence 06-11-26.")

        val national = data.of(DataType.National)!!
        assertEquals(mb(273.0), national.bytes)
        assertEquals(endOfDay(2026, Calendar.NOVEMBER, 6), national.expiredTime)
        assertNull(data.of(DataType.PromoBonus))
    }
}
