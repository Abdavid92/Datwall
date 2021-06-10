package com.smartsolutions.paquetes.managers

import android.text.format.DateUtils
import android.util.Log
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*
import org.junit.Rule
import javax.inject.Inject

@HiltAndroidTest
class NetworkUsageManagerDefaultTest {

    private val TAG = "NetworkUsageManagerDefaultTest"

    @get:Rule
    val rule = HiltAndroidRule(this)

    @Inject
    lateinit var networkUsageManager: NetworkUsageManager

    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun getAppUsage() {

        runBlocking {

            val uid = 10187

            for (i in 0..20) {

                val start = System.currentTimeMillis() - DateUtils.SECOND_IN_MILLIS
                val finish = System.currentTimeMillis()

                val traffic = networkUsageManager.getAppUsage(uid, start, finish)

                Log.i(TAG, "getAppUsage: ${traffic.totalBytes.getValue().value}")

                delay(1000)
            }
        }
    }

    @Test
    fun getAppsUsage() {
    }

    @Test
    fun getUsageTotal() {
    }
}