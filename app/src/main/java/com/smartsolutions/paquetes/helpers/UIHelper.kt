package com.smartsolutions.paquetes.helpers

import android.content.Context
import android.content.res.Configuration.*
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.TypedValue
import android.view.PixelCopy
import android.view.View
import android.view.Window
import android.widget.CompoundButton
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.repositories.models.IApp
import kotlinx.coroutines.runBlocking
import kotlin.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class UIHelper(
    private val context: Context
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
     * Obtiene el id de un recurso drawable.
     *
     * @param resourceName - Nombre del recurso a encontrar
     */
    fun getResource(resourceName: String): Int? {
        return try {
            val res = context.resources.getIdentifier(
                resourceName,
                "drawable",
                context.packageName
            )
            if (res != 0)
                res
            else
                null
            /*R.drawable::class.java
                .getDeclaredField(
                    resourceName
                )
                .getInt(drawable::class.java)*/
        }catch (e: Exception) {
            null
        }
    }

    /**
     * Obtiene los colores del tema aplicado.
     *
     * @param resId - Id del atributo del color a obtener. Ej: R.attr.colorPrimary
     *
     * @return [Int] Color resuelto o null si no se pudo resolver.
     * */
    @ColorInt
    fun getColorTheme(@AttrRes resId: Int): Int? {
        val theme = context.theme

        val colorTypedValue = TypedValue()
        theme.resolveAttribute(resId, colorTypedValue, true)

        if (colorTypedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
                colorTypedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return ContextCompat.getColor(context, colorTypedValue.resourceId)
        }
        return null
    }

    /**
     * Asigna el evento onCheckedChange al checkBox y establece la propiedad
     * [CompoundButton.isChecked] de manera segura sin lanzar el evento
     * accidentalmente. Si la app tiene mensajes de advertencia, se muestran en
     * un alertDialog antes de asignarle el nuevo valor.
     *
     * @param app - App que se le cambiará el acceso.
     * @param checkBox - CheckBox que se le asignará el evento.
     * @param callback - Se lanza cuando se cambia el acceso de la app.
     * */
    fun setVpnAccessCheckBoxListener(
        app: IApp,
        checkBox: CompoundButton,
        callback: (() -> Unit)? = null
    ) {
        checkBox.setOnCheckedChangeListener(null)
        checkBox.isChecked = app.access
        checkBox.setOnCheckedChangeListener { _,_ ->
            handleWarningMessages(app, checkBox, callback)
        }
    }

    /**
     * Maneja los mensajes de advertencia si los hay y cambia el acceso a la app.
     * */
    private fun handleWarningMessages(
        app: IApp,
        checkBox: CompoundButton,
        callback: (() -> Unit)?
    ) {

        //Diálogo que se mostrará cuando exista un mensaje de advertencia
        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.warning_title)
            .setNegativeButton(R.string.btn_cancel
            ) { _,_ ->
                /*Si se oprime el botón cancelar se llama al método
                * setCheckBoxListener para restablecer el estado anterior del checkBox sin
                * lanzar el evento de este. Este método utiliza la propiedad access de la app,
                * que no se ha cambiado todavía.*/
                setVpnAccessCheckBoxListener(app, checkBox, callback)
            }
            .setPositiveButton(R.string.btn_continue) { _,_ ->
                app.access = checkBox.isChecked
                callback?.invoke()
            }

        if (checkBox.isChecked && app.allowAnnotations != null) {
            dialog.setMessage(app.allowAnnotations)
                .show()
        } else if (!checkBox.isChecked && app.blockedAnnotations != null) {
            dialog.setMessage(app.blockedAnnotations)
                .show()
        } else {
            /*Si no hay ningún mensaje de advertencia. Cambio la propiedad access y
            * notifico que hubo cambios.*/
            app.access = checkBox.isChecked
            callback?.invoke()
        }
    }

    fun blur(inputBitmap: Bitmap, radius: Float = BLUR_RADIUS): Bitmap {

        val outputBitmap = Bitmap.createBitmap(inputBitmap)

        val renderScript = RenderScript.create(context)

        val scriptIntrinsicBlur = ScriptIntrinsicBlur
            .create(renderScript, Element.U8_4(renderScript))

        val tmpIn = Allocation.createFromBitmap(renderScript, inputBitmap)
        val tmpOut = Allocation.createFromBitmap(renderScript, outputBitmap)

        scriptIntrinsicBlur.setRadius(radius)
        scriptIntrinsicBlur.setInput(tmpIn)
        scriptIntrinsicBlur.forEach(tmpOut)
        tmpOut.copyTo(outputBitmap)

        return outputBitmap
    }

    fun getScreenshot(window: Window, view: View, result: (Bitmap?) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val screenshot = Bitmap.createBitmap(
                view.width,
                view.height,
                Bitmap.Config.ARGB_8888
            )

            val locationOfView = IntArray(2)

            view.getLocationInWindow(locationOfView)

            PixelCopy.request(
                window,
                Rect(
                    locationOfView[0],
                    locationOfView[1],
                    locationOfView[0] + view.width,
                    locationOfView[1] + view.height
                ),
                screenshot,
                { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        result(screenshot)
                    } else {
                        result(null)
                    }
                },
                Handler(Looper.getMainLooper())
            )
        }
    }

    companion object {
        const val BLUR_RADIUS = 7.5f
    }
}

/**
 * Crea una instancia [Lazy] de [UIHelper]
 *
 * Ej: val uiHelper by uiHelper()
 * */
fun Context.uiHelper(): Lazy<UIHelper> {
    return lazy {
        UIHelper(this)
    }
}

/**
 * Crea una instancia [Lazy] de [UIHelper]
 *
 * Ej: val uiHelper by uiHelper()
 * */
fun Fragment.uiHelper(): Lazy<UIHelper> {
    return lazy {
        UIHelper(requireContext())
    }
}