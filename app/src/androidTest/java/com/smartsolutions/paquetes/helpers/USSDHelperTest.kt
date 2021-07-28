package com.smartsolutions.paquetes.helpers

import android.provider.Settings
import androidx.test.platform.app.InstrumentationRegistry
import com.smartsolutions.paquetes.services.UIScannerService
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class USSDHelperTest {

    @get:Rule
    val rule = HiltAndroidRule(this)

    @Inject
    lateinit var helper: USSDHelper

    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun openAccessibilityServicesActivity() {
        helper.openAccessibilityServicesActivity()


    }

    @Test
    fun testPermission() {

        val context = InstrumentationRegistry.getInstrumentation()
            .context

        val pref = Settings.Secure
            .getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)

        val serviceName = UIScannerService::class.qualifiedName?.removeRange(0, "com.smartsolutions.paquetes".length)

        val result = pref != null &&
                pref.contains(context.packageName + "/" + serviceName)

    }

    val firstList = mutableListOf<Int>()
    val secondList = mutableListOf<Int>()

    @Test
    fun testFlows() {

        val flow = MutableSharedFlow<Int>()

        runBlocking {
            flow.emit(1)
        }

        GlobalScope.launch {
            flow.collect {
                val result = it

                firstList.add(result)
            }
        }

        runBlocking {
            flow.emit(2)
        }

        GlobalScope.launch {
            flow.collect {
                val result = it

                secondList.add(result)
            }
        }

        runBlocking {

            flow.emit(3)

            //assertEquals(2, firstList.size)
            assertEquals(1, secondList.size)

            flow.emit(4)

            print(null)
        }
    }
}