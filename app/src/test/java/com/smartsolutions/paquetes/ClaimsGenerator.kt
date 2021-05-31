package com.smartsolutions.paquetes

import org.junit.Assert.*
import org.junit.Test
import java.util.*

class ClaimsGenerator {

    @Test
    fun generateClaims() {
        val name = "Mis Datos"
        val audience = "https://smartsolutions-apps.infinityfreeapp.com"
        val key = "WIwzfl2mFbZa3fRCEQUcLeSASxrERxt1C23oPpWcu53n43tZ3tcY4ZAApNl7uJrm1AhV9DLAt29liGq8"
        val secretKey = "6NiWta0Vp0UZdhRD6CJLdwuXrpOdOGemsXhNctp5WgUmLTm1BWFVKCeeOH562UZJ"

        val s = "$name;$audience;$key"

        val bytes = Base64.getEncoder().encode(s.toByteArray())

        val result = String(bytes)

        print(result)
    }

    @Test
    fun generateSecretKey() {
        val secretKey = "6NiWta0Vp0UZdhRD6CJLdwuXrpOdOGemsXhNctp5WgUmLTm1BWFVKCeeOH562UZJ"

        val bytes = Base64.getEncoder().encode(secretKey.toByteArray())

        val result = String(bytes)

        print(result)
    }
}