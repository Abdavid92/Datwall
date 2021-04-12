package com.smartsolutions.datwall.managers

import com.smartsolutions.micubacel_client.MCubacelClient
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*

class MiCubacelClientManagerTest {

    private val manager = MiCubacelClientManager(MCubacelClient())

    @Before
    fun setUp() {
    }

    @Test
    fun loadHomePage() {
        runBlocking {
            manager.loadHomePage()
        }
        Thread.sleep(15000)
    }
}