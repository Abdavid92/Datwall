package com.smartsolutions.paquetes.repositories.models

import androidx.room.ColumnInfo

open class DataBytes(
    @ColumnInfo
    val type: DataType,
    @ColumnInfo
    var bytes: Long,
    @ColumnInfo(name = "expired_time")
    var expiredTime: Long
) {

    enum class DataType {
        International,
        InternationalLte,
        PromoBonusLte,
        PromoBonus,
        National,
        DailyBag,
        MessagingBag
    }
}