package com.smartsolutions.paquetes.helpers

import android.content.Context
import android.content.res.Configuration.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


class UIHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun isUIDarkTheme(): Boolean {
        when (context.resources.configuration.uiMode and UI_MODE_NIGHT_MASK) {
            UI_MODE_NIGHT_YES -> return true
            UI_MODE_NIGHT_NO -> return false
        }
        return false
    }


}