package com.smartsolutions.paquetes.helpers

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import javax.inject.Inject

@HiltAndroidTest
class SimsHelperTest {

    @get:Rule
    val rule = HiltAndroidRule(this)

    @Inject
    lateinit var simHelper: SimsHelper

    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun isDualSim() {

        val result = simHelper.isDualSim()

        assertTrue(result)
    }

    @Test
    fun getCardId() {
        val list = simHelper.getCardId()

        print(list)
    }
}