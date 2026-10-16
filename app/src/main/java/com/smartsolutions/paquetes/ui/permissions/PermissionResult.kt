package com.smartsolutions.paquetes.ui.permissions

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment

/**
 * Cómo se piden los permisos desde una pantalla y cómo vuelve la respuesta.
 *
 * Las hojas de permiso y de accesibilidad se abren en el `childFragmentManager` de la pantalla de
 * origen y devuelven su resultado con la API de resultados de fragmentos: la hoja no conoce a quien
 * la abrió ni este se guarda ningún objeto de respuesta, y el resultado llega una sola vez aunque se
 * gire el teléfono (el gestor de fragmentos lo guarda hasta que el oyente esté iniciado).
 * */
private const val PERMISSION_RESULT_KEY = "permission_result"
private const val ACCESSIBILITY_RESULT_KEY = "accessibility_result"
private const val EXTRA_REQUEST_CODE = "request_code"
private const val EXTRA_GRANTED = "granted"

private const val TAG_PERMISSION_SHEET = "PermissionsFragment"
private const val TAG_ACCESSIBILITY_SHEET = "AccessibilityFragment"

/**
 * Escucha el resultado de la hoja de un permiso. Se llama en `onCreate` de la pantalla de origen, para
 * que el oyente también exista al recrearse.
 * */
fun Fragment.onPermissionResult(onResult: (requestCode: Int, granted: Boolean) -> Unit) {
    childFragmentManager.setFragmentResultListener(PERMISSION_RESULT_KEY, this) { _, result ->
        onResult(result.getInt(EXTRA_REQUEST_CODE), result.getBoolean(EXTRA_GRANTED))
    }
}

/**
 * Muestra la hoja que explica y pide el permiso [requestCode].
 * */
fun Fragment.showPermissionSheet(requestCode: Int) {
    SinglePermissionFragment.newInstance(requestCode).show(childFragmentManager, TAG_PERMISSION_SHEET)
}

/**
 * Escucha el resultado de la hoja del servicio de accesibilidad. Se llama en `onCreate`.
 * */
fun Fragment.onAccessibilityResult(onResult: (granted: Boolean) -> Unit) {
    childFragmentManager.setFragmentResultListener(ACCESSIBILITY_RESULT_KEY, this) { _, result ->
        onResult(result.getBoolean(EXTRA_GRANTED))
    }
}

/**
 * Muestra la hoja que lleva al usuario a activar el servicio de accesibilidad.
 * */
fun Fragment.showAccessibilitySheet() {
    StartAccessibilityServiceFragment().show(childFragmentManager, TAG_ACCESSIBILITY_SHEET)
}

internal fun Fragment.sendPermissionResult(requestCode: Int, granted: Boolean) {
    parentFragmentManager.setFragmentResult(
        PERMISSION_RESULT_KEY,
        bundleOf(EXTRA_REQUEST_CODE to requestCode, EXTRA_GRANTED to granted)
    )
}

internal fun Fragment.sendAccessibilityResult(granted: Boolean) {
    parentFragmentManager.setFragmentResult(ACCESSIBILITY_RESULT_KEY, bundleOf(EXTRA_GRANTED to granted))
}
