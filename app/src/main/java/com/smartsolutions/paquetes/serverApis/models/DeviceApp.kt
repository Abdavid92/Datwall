package com.smartsolutions.paquetes.serverApis.models

import com.google.gson.annotations.JsonAdapter
import com.smartsolutions.paquetes.serverApis.converters.BooleanConverter
import com.smartsolutions.paquetes.serverApis.converters.DateConverter
import java.sql.Date

data class DeviceApp(
    var id: String,
    @JsonAdapter(BooleanConverter::class)
    var purchased: Boolean,
    @JsonAdapter(BooleanConverter::class)
    var restored: Boolean,
    @JsonAdapter(BooleanConverter::class)
    val trialPeriod: Boolean,
    @JsonAdapter(DateConverter::class)
    val lastQuery: Date,
    var transaction: String?,
    var phone: String?,
    @JsonAdapter(BooleanConverter::class)
    var waitingPurchase: Boolean
)