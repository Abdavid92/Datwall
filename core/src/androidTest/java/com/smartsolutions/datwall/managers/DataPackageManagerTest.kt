package com.smartsolutions.datwall.managers

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
    lateinit var dataPackageManager: DataPackageManager

    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun foo() {
        dataPackageManager.foo()
    }
}