package com.smartsolutions.paquetes.helpers

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*
import org.junit.Rule
import javax.inject.Inject

@HiltAndroidTest
class LegacyConfigurationHelperTest {

    @get:Rule
    val rule = HiltAndroidRule(this)

    @Inject
    lateinit var configurationHelper: LegacyConfigurationHelper

    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun isPurchased() {

        runBlocking {

            val isPurchased = configurationHelper.isPurchased()

            assertTrue(isPurchased)
        }
    }

    @Test
    fun getLegacyRules() {

        runBlocking {

            val result = configurationHelper.getLegacyRules()

            assertNotNull(result)
        }
    }
}