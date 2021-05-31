package com.smartsolutions.paquetes.repositories.models

import java.util.*

data class UserDataBytes(
    val type: DataType,
    var initialBytes: Long,
    var bytes: Long,
    var bytesLte: Long,
    var startTime: Long,
    var expiredTime: Long,
    var simIndex: Int
) {

    val priority: Int
        get() = when (type) {
            DataType.National -> 0
            DataType.BagDaily -> 1
            DataType.Bonus -> 2
            DataType.PromoBonus -> 3
            DataType.International -> 4
        }

    enum class DataType {
        International,
        Bonus,
        PromoBonus,
        National,
        BagDaily
    }

    fun exists() = bytes != 0L || bytesLte != 0L

    fun isExpired() = expiredTime != 0L && Date().after(Date(expiredTime))

    override fun equals(other: Any?): Boolean {
        if (other is UserDataBytes && other.type == type && other.simIndex == simIndex)
            return true
        return false
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + simIndex
        return result
    }
}