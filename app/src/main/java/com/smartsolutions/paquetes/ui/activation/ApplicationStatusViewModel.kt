package com.smartsolutions.paquetes.ui.activation

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.DatwallApplication
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ApplicationStatusViewModel @Inject constructor(
    application: Application,
    private val activationManager: IActivationManager
) : AndroidViewModel(application) {


    fun getApplicationStatus(listener: IActivationManager.ApplicationStatusListener): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !isWifiEnabled()) {
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
    fun isWifiEnabled(): Boolean {
        val wifiManager = ContextCompat.getSystemService(
            getApplication(),
            WifiManager::class.java
        ) ?: throw NullPointerException()

        return wifiManager.isWifiEnabled
    }

    /**
     * Intenta encender la wifi
     * */
    @Suppress("DEPRECATION")
    fun requestEnableWifiPie() {
        val wifiManager = ContextCompat.getSystemService(
            getApplication(),
            WifiManager::class.java
        ) ?: throw NullPointerException()

        wifiManager.isWifiEnabled = true
    }

    fun copyClipboardIdentifierDevice() {
        viewModelScope.launch {
            activationManager.getLocalLicense()?.let {
                val clipboardManager = ContextCompat.getSystemService(
                    getApplication(),
                    ClipboardManager::class.java
                ) ?: throw NullPointerException()

                val clipData = ClipData.newPlainText(
                    "Identificador Mis Datos",
                    it.deviceId
                )

                clipboardManager.setPrimaryClip(clipData)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        "Identificador del dispositivo copiado al portapapeles",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

}