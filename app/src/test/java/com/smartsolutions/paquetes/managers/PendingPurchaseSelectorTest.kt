package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.data.DataPackages.PackageId
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager.ConnectionMode
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingPurchaseSelectorTest {

    private val now = 1_800_000_000_000L
    private val hour = 60 * 60 * 1000L

    private fun purchase(id: Long, ago: Long, plan: PackageId = PackageId.DailyBag, sim: String = "2", pending: Boolean = true) =
        PurchasedPackage(id, now - ago, ConnectionMode.USSD, sim, pending, plan)

    private fun select(vararg purchases: PurchasedPackage) =
        PendingPurchaseSelector.select(purchases.toList(), PackageId.DailyBag, "2", now)?.id

    @Test
    fun mostRecentPendingOfSamePlanAndSim() {
        assertEquals(2L, select(purchase(1, 3 * hour), purchase(2, hour)))
    }

    @Test
    fun ignoresOtherPlanOrSim() {
        assertNull(select(purchase(1, hour, plan = PackageId.Plan_2GB), purchase(2, hour, sim = "1")))
    }

    @Test
    fun ignoresConfirmed() {
        assertNull(select(purchase(1, hour, pending = false)))
    }

    @Test
    fun ignoresOlderThanOneDay() {
        assertNull(select(purchase(1, 25 * hour)))
    }

    @Test
    fun noneWhenEmpty() {
        assertNull(select())
    }
}
