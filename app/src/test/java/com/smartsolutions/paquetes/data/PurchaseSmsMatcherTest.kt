package com.smartsolutions.paquetes.data

import com.smartsolutions.paquetes.data.DataPackages.PackageId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * SMS reales de ETECSA (spec.md, "Ejemplos reales"), con el saldo oculto.
 * */
class PurchaseSmsMatcherTest {

    private fun sms(plan: String, validity: String, price: String) =
        "ETECSA Plan Nacional: Se ha comprado $plan valido por $validity, se activa con el " +
                "primer uso. Se desconto $price CUP. Saldo actual ****.** CUP"

    private fun idOf(smsBody: String) = DataPackages.findPurchasedPackage(smsBody)?.id

    @Test
    fun dailyPlan() {
        assertEquals(PackageId.DailyBag, idOf(sms("Plan Diario 200MB", "24 horas", "25.00")))
    }

    @Test
    fun thirtyFiveDayPlans() {
        assertEquals(PackageId.Plan_4_5GB, idOf(sms("Plan 4.5GB", "35 dias", "240.00")))
        assertEquals(PackageId.Plan_2GB, idOf(sms("Plan 2GB+15MIN+20SMS", "35 dias", "120.00")))
        assertEquals(PackageId.Plan_4GB, idOf(sms("Plan 4GB+35MIN+40SMS", "35 dias", "240.00")))
        assertEquals(PackageId.Plan_6GB, idOf(sms("Plan 6GB+60MIN+70SMS", "35 dias", "360.00")))
    }

    @Test
    fun spacingAndCaseDoNotMatter() {
        assertEquals(PackageId.Plan_4_5GB, idOf("se ha comprado PLAN 4.5 GB valido por 35 dias"))
    }

    @Test
    fun operatorSenders() {
        assertTrue(DataPackages.isOperatorSender("ETECSA"))
        assertTrue(DataPackages.isOperatorSender("Cubacel"))
        assertFalse(DataPackages.isOperatorSender("55555555"))
    }

    @Test
    fun promotionIsNotAPlan() {
        assertNull(idOf("Su cuenta ha sido recargada en promocion. Bonos: 1 GB y 1 GB LTE"))
    }
}
