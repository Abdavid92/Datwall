package com.smartsolutions.paquetes.serverApis

import android.os.Build
import com.smartsolutions.paquetes.serverApis.contracts.IRegistrationClient
import com.smartsolutions.paquetes.serverApis.models.Device
import com.smartsolutions.paquetes.serverApis.models.DeviceApp
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*
import org.junit.Rule
import java.sql.Date
import javax.inject.Inject

@HiltAndroidTest
class RegistrationClientTest {

    @get:Rule
    val rule = HiltAndroidRule(this)

    @Inject
    lateinit var client: IRegistrationClient

    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun registerDevice() {

        runBlocking {
            val deviceId = Build.SERIAL
            val packageName = "com.smartsolutions.paquetes"

            val device = Device(
                deviceId,
                Build.MANUFACTURER,
                Build.MODEL,
                Build.VERSION.SDK_INT
            )

            val deviceApp = DeviceApp(
                id = DeviceApp.buildDeviceAppId(packageName, deviceId),
                purchased = false,
                restored = false,
                trialPeriod = true,
                lastQuery = Date(System.currentTimeMillis()),
                transaction = null,
                phone = null,
                waitingPurchase = false,
                deviceId = deviceId,
                androidAppPackageName = packageName
            )

            device.deviceApps = listOf(deviceApp)

            val result = client.registerDevice(device)

            assertTrue(result.isSuccess)
        }
    }
}