package com.smartsolutions.datwall.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.smartsolutions.datwall.repositories.models.DataPackage
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import java.lang.Exception
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidTest
class DataPackageRepositoryTest {

    private val TAG = "DataPackageRepositoryTest"
    
    @Inject
    lateinit var dataPackageRepository: IDataPackageRepository

    @get:Rule
    val rule = HiltAndroidRule(this)


    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun get() {

        runBlocking {
            val dataPackage = dataPackageRepository.get(1)

            assertNotNull(dataPackage)
        }

    }


    @Test
    fun create() {

        val dataPackage = DataPackage(
            0,
            "Paquete de prueba",
            450,
            400,
            500,
            300,
            true
        )

        runBlocking {
            dataPackageRepository.create(dataPackage)
        }
    }
}

inline fun <reified T> LiveData<T>.getAwaitValue(): T? {

    val latch = CountDownLatch(1)

    val data = Array<T?>(1) {
        return@Array null
    }

    val observer = object : Observer<T> {

        override fun onChanged(t: T) {
            data[0] = t

            latch.countDown()

            this@getAwaitValue.removeObserver(this)
        }

    }

    this.observeForever(observer)
    latch.await(2, TimeUnit.SECONDS)

    return data[0]
}