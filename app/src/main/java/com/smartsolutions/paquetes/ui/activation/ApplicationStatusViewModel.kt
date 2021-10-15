package com.smartsolutions.paquetes.ui.activation

import android.app.Application
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.smartsolutions.paquetes.DatwallApplication
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ApplicationStatusViewModel @Inject constructor(
    application: Application,
    private val activationManager: IActivationManager
) : AndroidViewModel(application) {

    fun getApplicationStatus(listener: IActivationManager.ApplicationStatusListener): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !isWifiEnabled() && !requestEnableWifi()) {
            return false
        }

        activationManager.getApplicationStatus(listener)
        return true
    }

    /**
     * Estos dos métodos son temporales hasta que se implemente el nuevo sistema de compra.
     *
     * Indica si la wifi está encendida.
     * */
    private fun isWifiEnabled(): Boolean {
        val wifiManager = ContextCompat.getSystemService(
            getApplication(),
            WifiManager::class.java
        ) ?: throw NullPointerException()

        return wifiManager.isWifiEnabled
    }

    /**
     * Intenta encender la wifi
     * */
    private fun requestEnableWifi(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent = Intent(Settings.Panel.ACTION_WIFI)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            getApplication<DatwallApplication>().startActivity(intent)

            false
        } else {
            val wifiManager = ContextCompat.getSystemService(
                getApplication(),
                WifiManager::class.java
            ) ?: throw NullPointerException()

            wifiManager.isWifiEnabled = true

            true
        }
    }
}