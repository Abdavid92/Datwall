package com.smartsolutions.paquetes

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import com.smartsolutions.paquetes.receivers.PackageMonitorReceiver
import dagger.hilt.android.HiltAndroidApp

/**
 * Clase principal de la aplicación
 * */
@HiltAndroidApp
class DatwallApplication : Application()