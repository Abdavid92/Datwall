package com.smartsolutions.paquetes.serverApis

import android.os.Build
import com.smartsolutions.paquetes.serverApis.contracts.IActivationClient
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class ActivationClientImplTest {

    @Inject
    lateinit var client: IActivationClient

    @get:Rule
    val rule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun getLicense() {

        runBlocking {

            val result = client.getLicense(Build.SERIAL)

            assertTrue(result.isSuccess)

            val license = result.getOrNull()

            assertNotNull(license)
        }
    }

    @Test
    fun updateLicense() {

        runBlocking {

            val result = client.getLicense(Build.SERIAL)

            assertTrue(result.isSuccess)

            val license = result.getOrThrow().apply {
                isPurchased = true
            }

            val result2 = client.updateLicense(license)

            assertTrue(result2.isSuccess)
        }
    }
}