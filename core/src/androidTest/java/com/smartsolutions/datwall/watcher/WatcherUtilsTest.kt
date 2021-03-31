package com.smartsolutions.datwall.watcher

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*
import org.junit.Rule
import javax.inject.Inject

@HiltAndroidTest
class WatcherUtilsTest {

    @get:Rule
    val rule = HiltAndroidRule(this)

    @Inject
    lateinit var watcherUtils: WatcherUtils

    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun getLastApp() {

        val lastApp = watcherUtils.getLastApp()

        assertNotNull(lastApp)
    }
}