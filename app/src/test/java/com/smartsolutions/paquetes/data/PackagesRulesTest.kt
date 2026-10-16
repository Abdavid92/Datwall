package com.smartsolutions.paquetes.data

import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.data.PackagesRules.Availability
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.Sim
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class PackagesRulesTest {

    private val plan = mockk<DataPackage>()

    private fun sim(setupDate: Long, network: String, plans: List<DataPackage>) =
        Sim("2", setupDate, network).apply { packages = plans }

    @Test
    fun configuredLineWithPlansCanBuy() {
        val sim = sim(1L, Networks.NETWORK_4G, listOf(plan))

        assertEquals(listOf(plan), PackagesRules.visiblePlans(sim))
        assertEquals(Availability.AVAILABLE, PackagesRules.availability(sim))
    }

    @Test
    fun configuredLineWithoutPlansIsABusinessLine() {
        assertEquals(Availability.NO_PLANS, PackagesRules.availability(sim(1L, Networks.NETWORK_4G, emptyList())))
    }

    @Test
    fun lineWithoutNetworkShowsNoPlans() {
        val sim = sim(0L, Networks.NETWORK_NONE, listOf(plan))

        assertEquals(emptyList<DataPackage>(), PackagesRules.visiblePlans(sim))
        assertEquals(Availability.NOT_CONFIGURED, PackagesRules.availability(sim))
    }
}
