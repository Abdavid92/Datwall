package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.managers.models.DataUnitBytes.DataUnit
import com.smartsolutions.paquetes.repositories.models.DataBytes.DataType
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class UserDataBytesEditorTest {

    private val future = System.currentTimeMillis() + 10 * 24 * 60 * 60 * 1000L
    private val past = System.currentTimeMillis() - 1000L

    private fun data(type: DataType, bytes: Long, expired: Long = future) =
        UserDataBytes("2", type, bytes, bytes, 5L, expired)

    @Test
    fun convertsValuesWithTheirUnits() {
        val edited = UserDataBytesEditor.edit(
            data(DataType.International, 0),
            rest = "1.5", restUnit = DataUnit.GB,
            initial = "2", initialUnit = DataUnit.GB,
            expiredTime = null
        )!!

        assertEquals(1_610_612_736L, edited.bytes)
        assertEquals(2_147_483_648L, edited.initialBytes)
        assertEquals(future, edited.expiredTime)
        assertEquals(5L, edited.startTime)
    }

    @Test
    fun initialIsNeverLessThanRest() {
        val edited = UserDataBytesEditor.edit(
            data(DataType.International, 0),
            rest = "300", restUnit = DataUnit.MB,
            initial = "100", initialUnit = DataUnit.MB,
            expiredTime = null
        )!!

        assertEquals(edited.bytes, edited.initialBytes)
    }

    @Test
    fun emptyValuesAreZeroAndInvalidOnesAreRejected() {
        val edited = UserDataBytesEditor.edit(
            data(DataType.International, 0), "", DataUnit.MB, null, DataUnit.MB, null
        )!!
        assertEquals(0L, edited.bytes)
        assertEquals(0L, edited.initialBytes)

        assertNull(UserDataBytesEditor.edit(data(DataType.International, 0), "abc", DataUnit.MB, "1", DataUnit.MB, null))
        assertNull(UserDataBytesEditor.edit(data(DataType.International, 0), "1", DataUnit.MB, "-1", DataUnit.MB, null))
    }

    @Test
    fun chosenExpiryReplacesTheCurrentOne() {
        val edited = UserDataBytesEditor.edit(
            data(DataType.International, 0), "1", DataUnit.MB, "1", DataUnit.MB, expiredTime = 123L
        )!!

        assertEquals(123L, edited.expiredTime)
    }

    @Test
    fun expiryIsTheLastMillisecondOfTheChosenDay() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = UserDataBytesEditor.expiryOf(2026, Calendar.NOVEMBER, 6)
        }

        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, cal.get(Calendar.MONTH))
        assertEquals(6, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(999, cal.get(Calendar.MILLISECOND))
    }

    @Test
    fun onlyMissingOrExpiredDataCanBeAdded() {
        val list = listOf(
            data(DataType.International, 100),
            data(DataType.National, 0),
            data(DataType.DailyBag, 100, expired = past)
        )

        assertEquals(
            listOf(DataType.National, DataType.DailyBag),
            UserDataBytesEditor.addable(list).map { it.type }
        )
    }
}
