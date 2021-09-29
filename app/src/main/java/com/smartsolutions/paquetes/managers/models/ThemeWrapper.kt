package com.smartsolutions.paquetes.managers.models

import android.content.res.Resources
import androidx.annotation.StyleRes

data class ThemeWrapper(
    @StyleRes
    val id: Int,
    val theme: Resources.Theme
)