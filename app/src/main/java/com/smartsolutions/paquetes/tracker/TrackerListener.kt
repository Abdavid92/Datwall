package com.smartsolutions.paquetes.tracker

import com.smartsolutions.paquetes.repositories.models.App

interface TrackerListener {
    fun onChangeAppInForeground(oldApp: App, newApp: App)
    fun onDelayForeground(app: App)
    fun onLaunchInterval()
}