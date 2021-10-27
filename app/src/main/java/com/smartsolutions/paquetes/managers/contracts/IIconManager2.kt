package com.smartsolutions.paquetes.managers.contracts

import android.graphics.Bitmap

interface IIconManager2 {
    /**
     * Tamaño predeterminado de los iconos
     */
    val defaultIconSize: Int
        get() = 50

    suspend fun synchronizeIcons(size: Int = defaultIconSize)

    fun getIcon(packageName: String, size: Int = defaultIconSize, onResult: (icon: Bitmap?) -> Unit)

    fun getIcon(packageName: String, versionCode: Long, size: Int = defaultIconSize, onResult: (icon: Bitmap?) -> Unit)

    suspend fun deleteAll()
}