package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.managers.contracts.IUpdateManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule

import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class UpdateManagerTest {

    @get:Rule
    val rule = HiltAndroidRule(this)

    @Inject
    lateinit var updateManager: IUpdateManager

    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun findUpdate() {

        runBlocking {

            val update = updateManager.findUpdate()
        }
    }
}