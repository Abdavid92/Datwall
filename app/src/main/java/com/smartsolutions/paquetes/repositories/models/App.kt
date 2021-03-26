package com.smartsolutions.paquetes.repositories.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.smartsolutions.paquetes.managers.models.Traffic
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "apps")
class App(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    var packageName: String,
    override var uid: Int,
    override var name: String,
    var version: String,
    override var access: Boolean,
    @ColumnInfo(name = "foreground_access")
    var foregroundAccess: Boolean,
    @ColumnInfo(name = "temp_access")
    var tempAccess: Boolean,
    var internet: Boolean,
    var executable: Boolean,
    var ask: Boolean,
    var national: Boolean,
    @ColumnInfo(name = "allow_annotations")
    override var allowAnnotations: String?,
    @ColumnInfo(name = "blocked_annotations")
    override var blockedAnnotations: String?,
    @Ignore
    var traffic: Traffic?
) : IApp {


    constructor(): this(
        "",
        0,
        "",
        "",
        false,
        false,
        false,
        false,
        true,
        true,
        false,
        null,
        null,
        null
    )

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + uid
        result = 31 * result + version.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as App

        if (packageName != other.packageName) return false
        if (uid != other.uid) return false
        if (version != other.version) return false

        return true
    }
}