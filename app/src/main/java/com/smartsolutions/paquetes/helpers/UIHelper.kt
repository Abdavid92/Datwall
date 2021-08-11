package com.smartsolutions.paquetes.helpers

import android.R.drawable
import android.content.Context
import android.content.res.Configuration.*
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import com.smartsolutions.paquetes.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.Exception


class UIHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Verifica que tema está configurado en los ajustes del sistema
     */
    fun isUIDarkTheme(): Boolean {
        when (context.resources.configuration.uiMode and UI_MODE_NIGHT_MASK) {
            UI_MODE_NIGHT_YES -> return true
            UI_MODE_NIGHT_NO -> return false
        }
        return false
    }

    /**
     * Devuelve el color blanco o negro segun el tema del sistema
     */
    fun getTextColorByTheme(): Int {
       return if (isUIDarkTheme()) {
            Color.WHITE
        } else {
            Color.BLACK
        }
    }


    /**
     * Devuelve el recurso encontrado segun el tema del sistema. Es necesario que se guarde el
     * recurso con el nombre y el final termine en _dark o _light para cada uno de los temas
     * correspondiente
     * @param partialResourceName - Nombre del recurso sin incluir _dark o _light
     *
     * @return El recurso convertido a drawable mediante ResourcesCompat
     */
    fun getDrawableResourceByTheme(partialResourceName: String): Drawable? {
        val resource = if (isUIDarkTheme()) {
            getResource(partialResourceName + "_dark")
        } else {
            getResource(partialResourceName + "_light")
        }
        return if (resource == null) {
            null
        }else {
            try {
                ResourcesCompat.getDrawable(context.resources, resource, context.theme)
            }catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Obtiene una imagen segun el tema del sistema. Es necesario que se guarde la imagen terminando
     * en _dark o _light para cada uno de los temas
     * @param partialResourceName - Nombre del recurso sin incluir _dark o _light
     *
     * @return El recurso convertido a drawable mediante ResourcesCompat
     */
    fun getImageResourceByTheme(partialResourceName: String): Drawable? {
        val resource = if (!isUIDarkTheme()) {
            getResource(partialResourceName + "_dark")
        } else {
            getResource(partialResourceName + "_light")
        }
        return if (resource == null) {
            null
        }else {
            try {
                ResourcesCompat.getDrawable(context.resources, resource, context.theme)
            }catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Utiliza reflexion para obtener el recurso
     * @param resourceName - Nombre del recurso a encontrar
     */
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