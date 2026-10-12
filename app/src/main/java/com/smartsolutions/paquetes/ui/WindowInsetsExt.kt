package com.smartsolutions.paquetes.ui

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Suma al padding original de la vista el espacio de las barras del sistema,
 * los recortes de pantalla y el teclado, para que el contenido no quede tapado
 * cuando la app se dibuja a pantalla completa (obligatorio desde Android 15).
 * Consume los insets para que las vistas hijas no los apliquen otra vez.
 * */
fun View.applySystemBarsPadding() {
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.ime()
        )

        view.setPadding(
            initialLeft + bars.left,
            initialTop + bars.top,
            initialRight + bars.right,
            initialBottom + bars.bottom
        )

        WindowInsetsCompat.CONSUMED
    }
}

/**
 * Aplica [applySystemBarsPadding] a la vista raíz del contenido de la activity.
 * */
fun Activity.applySystemBarsPaddingToContent() {
    findViewById<ViewGroup>(android.R.id.content).getChildAt(0)?.applySystemBarsPadding()
}
