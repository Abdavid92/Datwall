package com.smartsolutions.paquetes.data

import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.data.DataPackages.PackageId
import com.smartsolutions.paquetes.managers.models.DataUnitBytes.DataUnit
import com.smartsolutions.paquetes.managers.models.DataUnitBytes.DataValue
import com.smartsolutions.paquetes.repositories.models.DataPackage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataPackagesTest {

    private fun pkg(id: PackageId): DataPackage = DataPackages.PACKAGES.first { it.id == id }

    private fun bytes(value: Double, unit: DataUnit) = DataValue(value, unit).toBytes()

    @Test
    fun activePlansAreTheCurrentOffer() {
        val active = DataPackages.PACKAGES.filter { !it.deprecated }.map { it.id }.toSet()

        assertEquals(
            setOf(
                PackageId.DailyBag,
                PackageId.Plan_4_5GB,
                PackageId.Plan_2GB,
                PackageId.Plan_4GB,
                PackageId.Plan_6GB
            ),
            active
        )
    }

    @Test
    fun obsoletePackagesAreKeptAsDeprecated() {
        val obsolete = listOf(
            PackageId.Combined_Basic,
            PackageId.Combined_Medium,
            PackageId.Combined_Extra,
            PackageId.P_1GB_LTE,
            PackageId.P_2_5GB_LTE,
            PackageId.P_4GB_12GB_LTE,
            PackageId.MessagingBag
        )

        obsolete.forEach { assertTrue("$it debe estar obsoleto", pkg(it).deprecated) }
    }

    @Test
    fun oldOrdinalsDoNotChange() {
        // PackageId se guarda en Room por ordinal: los valores antiguos no pueden moverse.
        assertEquals(0, PackageId.DailyBag.ordinal)
        assertEquals(1, PackageId.Combined_Basic.ordinal)
        assertEquals(2, PackageId.Combined_Medium.ordinal)
        assertEquals(3, PackageId.Combined_Extra.ordinal)
        assertEquals(4, PackageId.P_1GB_LTE.ordinal)
        assertEquals(5, PackageId.P_2_5GB_LTE.ordinal)
        assertEquals(6, PackageId.P_4GB_12GB_LTE.ordinal)
        assertEquals(7, PackageId.MessagingBag.ordinal)
    }

    @Test
    fun dailyPlan() {
        val daily = pkg(PackageId.DailyBag)

        assertEquals(25f, daily.price)
        assertEquals(bytes(200.0, DataUnit.MB), daily.bytes)
        assertEquals(0L, daily.bytesLte)
        assertEquals(0L, daily.nationalBytes)
        assertEquals(Networks.NETWORK_3G_4G, daily.network)
        assertEquals(1, daily.duration)
        assertEquals("Plan Diario 200MB", daily.smsKey)
    }

    @Test
    fun thirtyFiveDayPlans() {
        data class Expected(
            val id: PackageId,
            val price: Float,
            val gb: Double,
            val minutes: Int,
            val sms: Int,
            val index: Int,
            val smsKey: String
        )

        listOf(
            Expected(PackageId.Plan_4_5GB, 240f, 4.5, 0, 0, 1, "Plan 4.5GB"),
            Expected(PackageId.Plan_2GB, 120f, 2.0, 15, 20, 2, "Plan 2GB+15MIN+20SMS"),
            Expected(PackageId.Plan_4GB, 240f, 4.0, 35, 40, 3, "Plan 4GB+35MIN+40SMS"),
            Expected(PackageId.Plan_6GB, 360f, 6.0, 60, 70, 4, "Plan 6GB+60MIN+70SMS")
        ).forEach { e ->
            val plan = pkg(e.id)

            assertEquals(e.price, plan.price)
            assertEquals(bytes(e.gb, DataUnit.GB), plan.bytes)
            assertEquals(0L, plan.bytesLte)
            assertEquals(bytes(300.0, DataUnit.MB), plan.nationalBytes)
            assertEquals(e.minutes, plan.minutes)
            assertEquals(e.sms, plan.sms)
            assertEquals(Networks.NETWORK_3G_4G, plan.network)
            assertEquals(e.index, plan.index)
            assertEquals(35, plan.duration)
            assertEquals(e.smsKey, plan.smsKey)
        }
    }
}
