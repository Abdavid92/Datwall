package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.repositories.contracts.IDataPackageRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before

import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

import org.junit.Assert.*

@HiltAndroidTest
class DataPackageManagerTest {

    @get:Rule
    val rule = HiltAndroidRule(this)

    @Inject
    lateinit var dataPackageManager: IDataPackageManager

    @Inject
    lateinit var repository: IDataPackageRepository
    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun createOrUpdateDataPackages() {
        runBlocking {
            dataPackageManager.createOrUpdateDataPackages()

            val packages = repository.all()

            assertFalse(packages.isEmpty())
        }
    }
}