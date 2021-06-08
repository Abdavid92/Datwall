package com.smartsolutions.paquetes.micubacel.models

import com.smartsolutions.paquetes.repositories.models.UserDataBytes

data class DataType(
    val type: UserDataBytes.DataType,
    val value: Long,
    val dateExpired: Long?
)