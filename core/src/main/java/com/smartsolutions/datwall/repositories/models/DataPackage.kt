package com.smartsolutions.datwall.repositories.models

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "data_packages")
data class DataPackage(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val price: Int,
    val bytes: Long,
    @ColumnInfo(name = "bono_bytes")
    val bonoBytes: Long,
    @ColumnInfo(name = "bono_cu_bytes")
    val bonoCuBytes: Long,
    val active: Boolean
): Parcelable