package com.smartsolutions.datwall.repositories.models

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "user_data_package", foreignKeys = [ForeignKey(
    entity = DataPackage::class,
    parentColumns = ["id"],
    childColumns = ["data_package_id"]
)])
data class UserDataPackage(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val bytes: Long,
    @ColumnInfo(name = "bono_bytes")
    val bonoBytes: Long,
    @ColumnInfo(name = "bono_cu_bytes")
    val bonoCuBytes: Long,
    val start: Long,
    val finish: Long,
    val active: Boolean,
    @ColumnInfo(name = "data_package_id")
    val dataPackageId: Int
): Parcelable
