package com.smartsolutions.paquetes.serverApis.models

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.smartsolutions.paquetes.annotations.ApplicationStatus
import com.smartsolutions.paquetes.serverApis.converters.BooleanConverter
import com.smartsolutions.paquetes.serverApis.converters.UpdatePriorityConverter

data class AndroidApp(

    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("package_name")
    val packageName: String,

    @SerializedName("min_version")
    val minVersion: Int,

    @SerializedName("version")
    val version: Int,

    @SerializedName("version_name")
    val versionName: String,

    @SerializedName("update_priority")
    @JsonAdapter(UpdatePriorityConverter::class)
    val updatePriority: UpdatePriority,

    @SerializedName("update_comments")
    val updateComments: String,

    @ApplicationStatus
    @SerializedName("status")
    val status: String,

    @SerializedName("debug")
    @JsonAdapter(BooleanConverter::class)
    val debug: Boolean,

    @SerializedName("trial_period")
    val trialPeriod: Int,

    @SerializedName("price")
    val price: Int,

    @SerializedName("debit_card")
    val debitCard: String,

    @SerializedName("phone")
    val phone: String,

    @SerializedName("balance_msg")
    val balanceMsg: String,

    @SerializedName("transfermovil_msg")
    val transfermovilMsg: String
) {

    enum class UpdatePriority {
        Low,
        Medium,
        High
    }
}