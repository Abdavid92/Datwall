package com.smartsolutions.paquetes.data

import com.smartsolutions.paquetes.data.DataPackages.PackageId
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildPurchaseUssdTest {

    private fun ussdOf(id: PackageId): String =
        DataPackages.buildPurchaseUssd(DataPackages.PACKAGES.first { it.id == id })

    @Test
    fun dailyPlan() {
        assertEquals("*133*1*3#", ussdOf(PackageId.DailyBag))
    }

    @Test
    fun thirtyFiveDayPlansFollowTheMenuOrder() {
        assertEquals("*133*1*4*1#", ussdOf(PackageId.Plan_4_5GB))
        assertEquals("*133*1*4*2#", ussdOf(PackageId.Plan_2GB))
        assertEquals("*133*1*4*3#", ussdOf(PackageId.Plan_4GB))
        assertEquals("*133*1*4*4#", ussdOf(PackageId.Plan_6GB))
    }
}
