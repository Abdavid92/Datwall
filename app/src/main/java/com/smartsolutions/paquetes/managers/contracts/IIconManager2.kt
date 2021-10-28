package com.smartsolutions.paquetes.managers.contracts

import android.content.pm.PackageInfo
import android.graphics.Bitmap
import kotlinx.coroutines.Job

interface IIconManager2 {
    /**
     * Tamaño predeterminado de los iconos
     */
    val defaultIconSize: Int
        get() = 50

    suspend fun synchronizeIcons(size: Int = defaultIconSize)

    suspend fun synchronizeIcons(infos: List<PackageInfo>, size: Int = defaultIconSize)


    fun getIcon(packageName: String, size: Int = defaultIconSize, onResult: (icon: Bitmap?) -> Unit): Job

    fun getIcon(packageName: String, versionCode: Long, size: Int = defaultIconSize, onResult: (icon: Bitmap?) -> Unit): Job

    suspend fun deleteAll()
}