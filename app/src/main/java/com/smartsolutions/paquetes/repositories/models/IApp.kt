package com.smartsolutions.paquetes.repositories.models

import android.os.Parcelable

interface IApp: Parcelable {
    var uid: Int
    var name: String
    var access: Boolean
    var allowAnnotations: String?
    var blockedAnnotations: String?

    fun accessHashCode(): Long
}