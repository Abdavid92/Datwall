package com.smartsolutions.paquetes.helpers

import com.smartsolutions.paquetes.managers.models.DataUnitBytes.DataUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class DataFormatterTest {

    private val mb = 1024L * 1024

    @Test
    fun bytesUseTheLargestUnitWithTwoDecimals() {
        assertEquals(DataUnit.B, DataFormatter.bytes(512).dataUnit)
        assertEquals(1.5, DataFormatter.bytes(1536).value, 0.0)
        assertEquals(DataUnit.KB, DataFormatter.bytes(1536).dataUnit)
        assertEquals(DataUnit.MB, DataFormatter.bytes(200 * mb).dataUnit)
        assertEquals(4.5, DataFormatter.bytes((4.5 * 1024 * mb).toLong()).value, 0.0)
        assertEquals(DataUnit.GB, DataFormatter.bytes((4.5 * 1024 * mb).toLong()).dataUnit)
    }

    @Test
    fun bytesAsText() {
        assertEquals("200.0 MB", DataFormatter.bytesText(200 * mb))
        assertEquals("0.0 B", DataFormatter.bytesText(0))
    }

    @Test
    fun percentOfTheRest() {
        assertEquals(25, DataFormatter.percent(total = 400, part = 100))
        assertEquals(0, DataFormatter.percent(total = 0, part = 100))
        assertEquals(0, DataFormatter.percent(total = 400, part = 0))
    }

    @Test
    fun expiryDateIsNullWhenUnknown() {
        assertNull(DataFormatter.expiryDate(0L, Locale("es")))
    }

    @Test
    fun expiryDateUsesDayAndMonth() {
        val date = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.NOVEMBER, 6, 23, 59, 59)
        }.timeInMillis

        assertEquals("06 noviembre", DataFormatter.expiryDate(date, Locale("es")))
    }

    @Test
    fun timeLeftInTheLargestUnit() {
        val now = 1_800_000_000_000L
        val day = 24 * 60 * 60 * 1000L

        assertEquals(3 to TimeUnit.DAYS, DataFormatter.timeLeft(now, now + 3 * day + 1000))
        assertEquals(5 to TimeUnit.HOURS, DataFormatter.timeLeft(now, now + 5 * 60 * 60 * 1000L))
        assertEquals(0 to TimeUnit.HOURS, DataFormatter.timeLeft(now, now - 1000))
    }
}
