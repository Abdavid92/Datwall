package com.smartsolutions.paquetes.repositories.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "usage_general")
@TypeConverters(UserDataBytes.DataTypeConverter::class)
data class UsageGeneral(
    @PrimaryKey
    val date: Long,
    val type: DataBytes.DataType,
    var bytes: Long,
    val simId: String
) {
    operator fun plusAssign(usage: UsageGeneral) {
        bytes += usage.bytes
    }
}