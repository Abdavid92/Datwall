package com.smartsolutions.paquetes

import org.junit.Assert.*

import org.junit.Test

class PreferencesKeysTest {

    @Test
    fun findPreferenceByKey() {

        val key = PreferencesKeys.findPreferenceByKey<Boolean>("enabled_firewall")

        assertNotNull(key)
    }
}