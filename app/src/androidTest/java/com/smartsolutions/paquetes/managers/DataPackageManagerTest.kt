package com.smartsolutions.paquetes.managers

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class DataPackageManagerTest {

    @get:Rule
    val rule = HiltAndroidRule(this)

    @Inject
    lateinit var dataPackageManager: IDataPackageManager

    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun getActiveSimIndex() {
        val index = (dataPackageManager as DataPackageManager).getActiveSimIndex()

        print(index)
    }
}