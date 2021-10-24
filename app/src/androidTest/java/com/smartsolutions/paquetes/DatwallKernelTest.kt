package com.smartsolutions.paquetes

import com.smartsolutions.paquetes.kernel.DatwallKernel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class DatwallKernelTest {

    @get:Rule
    val rule = HiltAndroidRule(this)

    @Inject
    lateinit var kernel0: DatwallKernel

    @Inject
    lateinit var kernel1: DatwallKernel

    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun equalsKernels() {
        assertEquals(kernel0, kernel1)
    }

    @Test
    fun remainder() {
        val rem = (2.0 % 1 * 10).toInt()

        assert(rem != 0)
    }
}