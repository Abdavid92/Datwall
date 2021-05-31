package com.smartsolutions.paquetes.serverApis.models

import java.sql.Date

data class DeviceApp(
    var id: String,
    var purchased: Boolean,
    var restored: Boolean,
    val trialPeriod: Boolean,
    val lastQuery: Date,
    var transaction: String?,
    var phone: String?,
    var waitingPurchase: Boolean
)