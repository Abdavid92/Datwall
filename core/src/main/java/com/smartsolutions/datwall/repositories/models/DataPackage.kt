package com.smartsolutions.datwall.repositories.models

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.smartsolutions.datwall.jsonAdapters.BooleanAdapter
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
@Entity(tableName = "data_packages")
data class DataPackage(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val description: String,
    val price: Int,
    val bytes: Long,
    @ColumnInfo(name = "bono_bytes")
    @SerializedName("bono_bytes")
    val bonoBytes: Long,
    @ColumnInfo(name = "bono_cu_bytes")
    @SerializedName("bono_cu_bytes")
    val bonoCuBytes: Long,
    val network: String,
    val ussd: String,
    val url: String,@JsonAdapter(BooleanAdapter::class)
    val active: Boolean,
): Parcelable {

    @Ignore
    @IgnoredOnParcel
    var userDataPackages: LiveData<List<UserDataPackage>>? = null
}