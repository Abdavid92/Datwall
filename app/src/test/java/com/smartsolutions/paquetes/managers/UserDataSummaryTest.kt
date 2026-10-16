package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.managers.models.UserDataOrder
import com.smartsolutions.paquetes.repositories.models.DataBytes.DataType
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class UserDataSummaryTest {

    private val future = System.currentTimeMillis() + 10 * 24 * 60 * 60 * 1000L
    private val past = System.currentTimeMillis() - 1000L

    private fun data(type: DataType, initial: Long, bytes: Long, expired: Long = future) =
        UserDataBytes("2", type, initial, bytes, 0L, expired)

    @Test
    fun totalsExcludeNationalAndMessaging() {
        val summary = UserDataSummary.of(
            listOf(
                data(DataType.International, initial = 400, bytes = 100),
                data(DataType.DailyBag, initial = 100, bytes = 100),
                data(DataType.National, initial = 300, bytes = 300),
                data(DataType.MessagingBag, initial = 600, bytes = 600)
            )
        )

        assertEquals(500L, summary.totalBytes)
        assertEquals(200L, summary.restBytes)
        assertEquals(300L, summary.usedBytes)
        assertEquals(40, summary.percent)
    }

    @Test
    fun emptyListIsAllZero() {
        val summary = UserDataSummary.of(emptyList())

        assertEquals(0L, summary.totalBytes)
        assertEquals(0, summary.percent)
    }

    @Test
    fun visibleKeepsOnlyExistingAndNotExpiredData() {
        val visible = UserDataSummary.visible(
            listOf(
                data(DataType.International, 400, 100),
                data(DataType.National, 300, 0),
                data(DataType.DailyBag, 200, 200, expired = past),
                data(DataType.PromoBonus, 100, 50, expired = 0L)
            ),
            UserDataOrder.NORMAL
        )

        assertEquals(listOf(DataType.International, DataType.PromoBonus), visible.map { it.type })
    }

    @Test
    fun ordersBySizeAndExpiration() {
        val a = data(DataType.International, 400, 100, expired = future + 2)
        val b = data(DataType.National, 400, 300, expired = future + 1)
        val c = data(DataType.PromoBonus, 400, 200, expired = future + 3)
        val list = listOf(a, b, c)

        assertEquals(listOf(a, c, b), UserDataSummary.visible(list, UserDataOrder.SIZE_ASC))
        assertEquals(listOf(b, c, a), UserDataSummary.visible(list, UserDataOrder.SIZE_DESC))
        assertEquals(listOf(b, a, c), UserDataSummary.visible(list, UserDataOrder.EXPIRE_ASC))
        assertEquals(listOf(c, a, b), UserDataSummary.visible(list, UserDataOrder.EXPIRE_DESC))
        assertEquals(list, UserDataSummary.visible(list, UserDataOrder.NORMAL))
    }

    @Test
    fun itemHasUsageRestPercentAndTimeLeft() {
        val now = 1_000_000_000L
        val item = UserDataSummary.item(
            data(DataType.International, initial = 400, bytes = 100, expired = now + 3 * 24 * 60 * 60 * 1000L),
            now
        )

        assertEquals(300L, item.usedBytes)
        assertEquals(100L, item.restBytes)
        assertEquals(25, item.percent)
        assertEquals(3 to TimeUnit.DAYS, item.timeLeft)
    }
}
