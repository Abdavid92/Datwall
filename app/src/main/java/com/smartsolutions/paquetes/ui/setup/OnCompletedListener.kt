package com.smartsolutions.paquetes.ui.setup

interface OnCompletedListener {
    fun onCompleted()

    /**
     * Se termina la configuración sin completar los pasos restantes.
     * */
    fun onSetupSkipped() = onCompleted()
}