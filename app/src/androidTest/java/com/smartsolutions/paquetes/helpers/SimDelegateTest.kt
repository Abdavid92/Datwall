package com.smartsolutions.paquetes.helpers

import android.util.Log
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import javax.inject.Inject

@HiltAndroidTest
class SimDelegateTest {

    @get:Rule
    val rule = HiltAndroidRule(this)

    @Inject
    lateinit var simDelegate: SimDelegate

    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun isDualSim() {

        val result = simDelegate.isInstalledSeveralSim()

        assertTrue(result)
    }

    @Test
    fun getCardId() {
        val list = simDelegate.getActiveDataSimId()

        Log.i("EJV", list.toString())
    }
}