package com.smartsolutions.datwall.repositories

import android.util.Log
import com.smartsolutions.datwall.repositories.models.App
import com.smartsolutions.datwall.repositories.models.IApp
import com.smartsolutions.datwall.watcher.PackageMonitor
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*
import org.junit.Rule
import javax.inject.Inject

@HiltAndroidTest
class AppRepositoryTest {

    @get:Rule
    var rule = HiltAndroidRule(this)

    @Inject
    lateinit var appRepository: IAppRepository

    @Inject
    lateinit var packageMonitor: PackageMonitor

    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun seedDatabase() {
        runBlocking {
            packageMonitor.forceSynchronization()
        }
    }

    @Test
    fun getAll() {
        val result = appRepository.all

        assertNotNull(result)
    }

    private val observableTag = "ObservableTag"

    @Test
    fun observable() {

        appRepository.registerObserver(object : Observer {
            override fun change(apps: List<IApp>) {
                assertTrue(apps.isNotEmpty())
                Log.i(observableTag, "La lista contiene ${apps.size} elementos")
            }
        })

        runBlocking {
            val app = appRepository.get(10077)

            if (app is App)
                appRepository.delete(app)
        }
    }
}