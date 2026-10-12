package com.smartsolutions.paquetes.managers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionPolicyTest {

    @Test
    fun notRequiredBeforeAndroid13() {
        assertFalse(isNotificationPermissionRequired(sdkInt = 32, granted = false))
    }

    @Test
    fun requiredOnAndroid13WhenNotGranted() {
        assertTrue(isNotificationPermissionRequired(sdkInt = 33, granted = false))
    }

    @Test
    fun notRequiredOnAndroid13WhenGranted() {
        assertFalse(isNotificationPermissionRequired(sdkInt = 33, granted = true))
    }

    @Test
    fun requiredOnAndroid16WhenNotGranted() {
        assertTrue(isNotificationPermissionRequired(sdkInt = 36, granted = false))
    }

    @Test
    fun notRequiredOnAndroid16WhenGranted() {
        assertFalse(isNotificationPermissionRequired(sdkInt = 36, granted = true))
    }
}
