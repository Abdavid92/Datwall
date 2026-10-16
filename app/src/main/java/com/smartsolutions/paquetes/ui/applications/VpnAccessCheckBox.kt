package com.smartsolutions.paquetes.ui.applications

import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.repositories.models.IApp

/**
 * Asigna el evento onCheckedChange al checkBox del acceso de [app] y establece
 * [CompoundButton.isChecked] sin lanzar el evento accidentalmente. Si la app tiene mensajes de
 * advertencia, se muestran en un diálogo antes de cambiar el acceso.
 *
 * @param callback - Se lanza cuando se cambia el acceso de la app.
 * */
fun CompoundButton.bindVpnAccess(app: IApp, callback: (() -> Unit)? = null) {
    setOnCheckedChangeListener(null)
    isChecked = app.access
    setOnCheckedChangeListener { _, _ ->
        handleWarningMessages(app, callback)
    }
}

/**
 * Maneja los mensajes de advertencia si los hay y cambia el acceso a la app.
 * */
private fun CompoundButton.handleWarningMessages(app: IApp, callback: (() -> Unit)?) {

    //Diálogo que se mostrará cuando exista un mensaje de advertencia
    val dialog = AlertDialog.Builder(context)
        .setTitle(R.string.warning_title)
        .setNegativeButton(R.string.btn_cancel) { _, _ ->
            /*Si se oprime el botón cancelar se restablece el estado anterior del checkBox sin
            * lanzar su evento. Se usa la propiedad access de la app, que no ha cambiado.*/
            bindVpnAccess(app, callback)
        }
        .setPositiveButton(R.string.btn_continue) { _, _ ->
            app.access = isChecked
            callback?.invoke()
        }

    if (isChecked && app.allowAnnotations != null) {
        dialog.setMessage(app.allowAnnotations)
            .show()
    } else if (!isChecked && app.blockedAnnotations != null) {
        dialog.setMessage(app.blockedAnnotations)
            .show()
    } else {
        /*Si no hay ningún mensaje de advertencia, se cambia el acceso y se notifica.*/
        app.access = isChecked
        callback?.invoke()
    }
}
