package com.smartsolutions.datwall.webApis

import org.junit.Assert.*
import org.junit.Test

class CubacelClientImplTest {

    private val client = CubacelClientImpl()


    @Test
    fun test() {

        client.login("55055870", "Abel.2021")

        val products = client.products


        assertFalse(products.isEmpty())

        val username = client.userName
        val credit = client.credit
        val creditBonus = client.creditBonus
        val buys = client.buys
        val phone = client.phoneNumber
        val balance = client.payableBalance
    }
}