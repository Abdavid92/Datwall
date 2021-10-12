package com.smartsolutions.paquetes.serverApis

import android.os.Build
import com.smartsolutions.paquetes.serverApis.contracts.ISmartSolutionsApps
import com.smartsolutions.paquetes.serverApis.models.License
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import java.util.*
import javax.inject.Inject


@HiltAndroidTest
class ISmartSolutionsAppsTest {

    @Inject
    lateinit var api: ISmartSolutionsApps

    @get:Rule
    val rule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun getLicense() {

        runBlocking {

            try {
                val license = api.getLicense(Build.SERIAL)

                assertNotNull(license)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Test
    fun postLicense() {

        val license = License(
            0,
            Build.SERIAL,
            false,
            false,
            Build.MANUFACTURER,
            Build.MODEL,
            Date(),
            "com.smartsolutions.paquetes"
        )

        runBlocking {

            try {
                api.postLicense(license)
            } catch (e: Exception) {
                if (e is HttpException) {
                    val error = e.response()?.errorBody()?.string()

                    print(error)
                }
                throw e
            }
        }
    }
}