package com.smartsolutions.paquetes.ui.setup

import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment

/**
 * Resultado con el que un paso de la configuración avisa a la actividad que lo muestra. Sustituye
 * a la interfaz que implementaba la actividad: el paso no necesita saber quién lo muestra.
 * */
private const val STEP_RESULT_KEY = "setup_step_result"
private const val EXTRA_SKIPPED = "skipped"

/**
 * El paso terminó ([skipped] false) o el usuario omitió lo que falta de la configuración.
 * */
fun Fragment.sendSetupStepResult(skipped: Boolean) {
    parentFragmentManager.setFragmentResult(STEP_RESULT_KEY, bundleOf(EXTRA_SKIPPED to skipped))
}

/**
 * Escucha los resultados de los pasos. Se registra en `onCreate`, así que también existe al recrearse
 * la actividad.
 * */
fun AppCompatActivity.onSetupStepResult(onResult: (skipped: Boolean) -> Unit) {
    supportFragmentManager.setFragmentResultListener(STEP_RESULT_KEY, this) { _, result ->
        onResult(result.getBoolean(EXTRA_SKIPPED))
    }
}
