package com.smartsolutions.paquetes.ui.dashboard

import android.view.View

interface IControls {

    fun getRoot(): View

    fun init()

    fun onBackPressed()

    companion object {
        const val CARD_VIEW = "control:card_view"
        const val HEADER = "control:header"
        const val SWITCH = "control:switch"
        const val SUMMARY = "control:summary"
    }
}