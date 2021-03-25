package com.smartsolutions.paquetes.managers

import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.TestCase

class IconManagerTest : TestCase() {

    private lateinit var iconManager: IconManager

    public override fun setUp() {
        super.setUp()

        iconManager = IconManager(InstrumentationRegistry.getInstrumentation().context)
    }

    fun testGet() {

        val packageName = InstrumentationRegistry.getInstrumentation().context.packageName

        val icon = iconManager.get(packageName, "1.0")

    }
}