package com.smartsolutions.paquetes.managers

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*

class MiCubacelClientManagerTest {

    private val manager = MiCubacelClientManager()

    @Before
    fun setUp() {
    }

    @Test
    fun getProducts() {

        runBlocking {

            manager.signIn("55055870", "Abel.2021")

            val result = manager.getProducts()

            assertNotNull(result)
        }
    }
}