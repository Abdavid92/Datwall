package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class ActivationManager2Test {

    @Inject
    lateinit var manager: IActivationManager

    @get:Rule
    val rule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun canWork() {
        runBlocking {

            val can = manager.canWork()

            assertTrue(can.first)
        }
    }

    @Test
    fun isInTrialPeriod() {
        runBlocking {

            val inTrial = manager.isInTrialPeriod()

            assertTrue(inTrial)
        }
    }

    @Test
    fun getApplicationStatus() {
    }

    @Test
    fun transferCreditByUSSD() {
    }

    @Test
    fun confirmPurchase() {
    }

    @Test
    fun isWaitingPurchased() {
    }

    @Test
    fun getLicense() {
    }

    @Test
    fun getLocalLicense() {
    }
}