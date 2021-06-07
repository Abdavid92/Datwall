package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before

import org.junit.Rule
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
}