package com.smartsolutions.paquetes.repositories

import com.smartsolutions.paquetes.repositories.contracts.IMiCubacelAccountRepository
import com.smartsolutions.paquetes.repositories.models.MiCubacelAccount
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*
import org.junit.Rule
import javax.inject.Inject

@HiltAndroidTest
class MiCubacelAccountFileRepositoryTest {

    @get:Rule
    val rule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: IMiCubacelAccountRepository

    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun create() {
        runBlocking {
            repository.create(MiCubacelAccount(
                1,
                "55055870",
                "abcd1234",
                mapOf(
                    Pair("a", "olshfoishfoi"),
                    Pair("b", "popasojpaoijs")
                )
            ))
        }
    }

    @Test
    fun update() {
    }

    @Test
    fun delete() {
    }

    @Test
    fun get() {
        runBlocking {
            val r = repository.get(1).single()

            assertNotNull(r)
        }
    }

    @Test
    fun testGet() {
    }

    @Test
    fun getAll() {
    }
}