package com.smartsolutions.paquetes.managers.models

import android.net.Uri

data class Update(
    val name: String,
    val version: String,
    val uri: Uri
)
