package com.smartsolutions.paquetes.repositories

import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*
import org.junit.Rule
import javax.inject.Inject

@HiltAndroidTest
class AppRepositoryTest {

    @get:Rule
    val rule = HiltAndroidRule(this)

    @Inject
    lateinit var appRepository: IAppRepository

    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun getFlow() {

        runBlocking {

            appRepository.flow().collect {

                assertTrue(it.isNotEmpty())

            }
        }
    }
}