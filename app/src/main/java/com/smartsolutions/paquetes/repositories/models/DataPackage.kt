package com.smartsolutions.paquetes.repositories.models

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

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
    val url: String,
    val active: Boolean,
): Parcelable {

    @Ignore
    @IgnoredOnParcel
    var userDataPackages: LiveData<List<UserDataPackage>>? = null
}