package com.smartsolutions.paquetes.managers

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionRequestPolicyTest {

    @Test
    fun grantedNeedsNothing() {
        assertEquals(
            PermissionAction.Nothing,
            PermissionRequestPolicy.decide(granted = true, requestedBefore = true, canAskAgain = false)
        )
    }

    @Test
    fun neverRequestedAsks() {
        assertEquals(
            PermissionAction.Request,
            PermissionRequestPolicy.decide(granted = false, requestedBefore = false, canAskAgain = false)
        )
    }

    @Test
    fun deniedOnceAsksAgain() {
        assertEquals(
            PermissionAction.Request,
            PermissionRequestPolicy.decide(granted = false, requestedBefore = true, canAskAgain = true)
        )
    }

    @Test
    fun permanentlyDeniedOpensSettings() {
        assertEquals(
            PermissionAction.OpenAppSettings,
            PermissionRequestPolicy.decide(granted = false, requestedBefore = true, canAskAgain = false)
        )
    }
}
