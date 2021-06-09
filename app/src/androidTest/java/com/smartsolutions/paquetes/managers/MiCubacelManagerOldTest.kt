package com.smartsolutions.paquetes.managers

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*
import org.junit.Rule
import javax.inject.Inject

@HiltAndroidTest
class MiCubacelManagerOldTest {

    @get:Rule
    val rule = HiltAndroidRule(this)

    @Inject
    lateinit var managerOld: MiCubacelManagerOld

    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun getProducts() {

        runBlocking {

            managerOld.signIn("55055870", "Abel.2021")

            val result = managerOld.getProducts()

            assertNotNull(result)
        }
    }
}