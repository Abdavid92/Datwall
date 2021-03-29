package com.smartsolutions.datwall.repositories.models

import android.os.Parcelable

interface IApp: Parcelable {
    var uid: Int
    var name: String
    var access: Boolean
    var allowAnnotations: String?
    var blockedAnnotations: String?
}