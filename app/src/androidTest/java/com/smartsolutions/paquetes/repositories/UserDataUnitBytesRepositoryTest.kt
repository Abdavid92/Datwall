package com.smartsolutions.paquetes.repositories

import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.*
import javax.inject.Inject

@HiltAndroidTest
class UserDataUnitBytesRepositoryTest {

    @Inject
    lateinit var repository: IUserDataBytesRepository

    @Inject
    lateinit var repository2: IUserDataBytesRepository

    @get:Rule
    val rule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun getAll() {
        runBlocking {
            val list = repository.all()

            assertTrue(list.isNotEmpty())
        }
    }

    @Test
    fun update() {

        /*val userDataBytes = UserDataBytes(
            UserDataBytes.DataType.International,
            400,
            500,
            Date().time,
            Date().time + 100000,
            1
        )

        val bagDaily = UserDataBytes(
            UserDataBytes.DataType.DailyBag,
            0,
            200,
            Date().time,
            Date().time + 100000,
            1
        )

        runBlocking {
            assertTrue(repository.update(userDataBytes))
            assertTrue(repository.update(bagDaily))
        }*/
    }
}