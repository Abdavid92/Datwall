package com.smartsolutions.paquetes.repositories

import com.smartsolutions.paquetes.repositories.models.DataBytes.DataType
import org.junit.Assert.assertEquals
import org.junit.Test

class DataBytesTest {

    @Test
    fun internationalNetworksConsumeInternationalAndPromoBonus() {
        assertEquals(
            setOf(DataType.International, DataType.PromoBonus),
            DataType.getDataTypeOfInternationalNetworks().toSet()
        )
    }
}
