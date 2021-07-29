package com.smartsolutions.paquetes.helpers

import android.R.drawable
import android.content.Context
import android.content.res.Configuration.*
import android.graphics.Color
import android.graphics.drawable.Drawable
import com.smartsolutions.paquetes.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.Exception
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

    fun getTextColorByTheme(): Int {
       return if (isUIDarkTheme()) {
            Color.WHITE
        } else {
            Color.BLACK
        }
    }


    fun getDrawableResourceByTheme(partialResourceName: String): Drawable? {
        val resource = if (isUIDarkTheme()) {
            getResource(partialResourceName + "_dark")
        } else {
            getResource(partialResourceName + "_light")
        }
        return if (resource == null) {
            null
        }else {
            context.resources.getDrawable(resource, context.theme)
        }
    }

    fun getImageResourceByTheme(partialResourceName: String): Drawable? {
        val resource = if (!isUIDarkTheme()) {
            getResource(partialResourceName + "_dark")
        } else {
            getResource(partialResourceName + "_light")
        }
        return if (resource == null) {
            null
        }else {
            context.resources.getDrawable(resource, context.theme)
        }
    }

    fun getResource(resourceName: String): Int? {
        return try {
            R.drawable::class.java
                .getDeclaredField(
                    resourceName
                )
                .getInt(drawable::class.java)
        }catch (e: Exception) {
            null
        }
    }


}