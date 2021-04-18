package com.smartsolutions.datwall.helpers

import com.google.gson.Gson
import com.smartsolutions.datwall.repositories.models.DataPackage
import com.smartsolutions.datwall.webApis.DatwallWebApi
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class HttpConnectionHelperTest {

    private val TAG = javaClass.name

    @get:Rule
    val rule = HiltAndroidRule(this)

    @Inject
    lateinit var helper: HttpConnectionHelper

    @Inject
    lateinit var webApi: DatwallWebApi

    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun getPackages() {

        val response = helper.sendRequest { webApi.getPackages() }

        assertNotNull(response)

        val gson = Gson()

        val data = gson.fromJson(response, Array<DataPackage>::class.java)

        assertNotNull(data)
    }
}