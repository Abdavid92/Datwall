package com.smartsolutions.paquetes.repositories.models

import java.io.Serializable

data class MiCubacelAccount(
    var simIndex: Int,
    var phone: String,
    var password: String,
    var cookies: Map<String, String>
): Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MiCubacelAccount

        if (simIndex != other.simIndex) return false
        if (phone != other.phone) return false
        if (password != other.password) return false

        return true
    }

    override fun hashCode(): Int {
        var result = simIndex
        result = 31 * result + phone.hashCode()
        result = 31 * result + password.hashCode()
        return result
    }
}
