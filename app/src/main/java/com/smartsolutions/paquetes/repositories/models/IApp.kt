package com.smartsolutions.paquetes.repositories.models

import android.os.Parcelable

interface IApp: Parcelable {
    val uid: Int
    val name: String
    var access: Boolean
    val allowAnnotations: String?
    val blockedAnnotations: String?
}