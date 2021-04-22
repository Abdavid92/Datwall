package com.smartsolutions.paquetes.repositories.models

import android.os.Parcelable
import androidx.annotation.StringDef
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
    @Networks
    val network: String,
    val ussd: String,
    val active: Boolean,
    @Ignore
    var url: String? = null
): Parcelable {

    @Ignore
    @IgnoredOnParcel
    var userDataPackages: LiveData<List<UserDataPackage>>? = null

    companion object {

        const val NETWORK_3G_4G = "3G_4G"
        const val NETWORK_4G = "4G"
    }

    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(NETWORK_3G_4G, NETWORK_4G)
    annotation class Networks
}