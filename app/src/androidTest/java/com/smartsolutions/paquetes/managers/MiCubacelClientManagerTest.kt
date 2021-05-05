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
class MiCubacelClientManagerTest {

    @get:Rule
    val rule = HiltAndroidRule(this)

    @Inject
    lateinit var manager: MiCubacelClientManager

    @Before
    fun setUp() {
        rule.inject()
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