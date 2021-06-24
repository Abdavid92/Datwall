package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*
import org.junit.Rule
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import javax.inject.Inject

@HiltAndroidTest
class ActivationManagerTest {

    @get:Rule
    val rule = HiltAndroidRule(this)

    @Inject
    lateinit var manager: IActivationManager

    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun getDevice() {

        runBlocking {
            val result = manager.getDevice()

            assertTrue(result.isSuccess)

            val device = result.getOrThrow()

            assertNotNull(device)
        }
    }

    @Test
    fun confirmPurchased() {

        val text = DecimalFormat("0.00").format(50)

        assertNotNull(text)
    }
}