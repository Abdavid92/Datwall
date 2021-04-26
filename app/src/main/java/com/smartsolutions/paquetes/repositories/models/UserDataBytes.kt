package com.smartsolutions.paquetes.repositories.models

import java.util.*

data class UserDataBytes(
    val type: DataType,
    var bytes: Long,
    var bytesLte: Long,
    var startTime: Long,
    var expiredTime: Long,
    var simIndex: Int
) {

    enum class DataType {
        International,
        Bonus,
        PromoBonus,
        National,
        BagDaily
    }

    fun isEmpty() = bytes == 0L && bytesLte == 0L

    fun isExpired() = Date().after(Date(expiredTime))

    override fun equals(other: Any?): Boolean {
        if (other is UserDataBytes && other.type == type)
            return true
        return false
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }
}