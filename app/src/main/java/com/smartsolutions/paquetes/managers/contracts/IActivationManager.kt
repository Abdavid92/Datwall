package com.smartsolutions.paquetes.managers.contracts

import com.smartsolutions.paquetes.serverApis.models.Device
import com.smartsolutions.paquetes.serverApis.models.DeviceApp
import com.smartsolutions.paquetes.serverApis.models.Result

interface IActivationManager {

    /**
     * Obtiene el dispositivo del servidor.
     * */
    suspend fun getDevice(): Result<Device>

    /**
     * Obtiene la aplicación instalada en este dispòsitivo del servidor.
     * */
    suspend fun getDeviceApp(): Result<DeviceApp>

    /**
     * Verifica el estado de la aplicación y lanza el evento corespondiente.
     *
     * @param listener - Listener de eventos.
     * */
    fun getApplicationState(listener: ApplicationStateListener)

    /**
     * Inicia el proceso de activación.
     * Obtiene del servidor el [DeviceApp]. Después verifica el estado de la aplicación.
     * En caso de que la aplicación esté activa, verfica si no hay ninguna otra aplicación en
     * espera de compra. Si la hay tiene que verificar si está instalada o no. Si no está instalada
     * se procede a iniciar la activación. Pero si está instalada, se cancela la activación.
     * */
    suspend fun beginActivation(deviceApp: DeviceApp): Result<Unit>

    /**
     * Transfiere el crédito por código ussd.
     * */
    suspend fun transferCreditByUSSD(key: String, deviceApp: DeviceApp): Result<Unit>

    /**
     * Confirma y activa la aplicación.
     * */
    suspend fun confirmPurchase(smsBody: String, phone: String, simIndex: Int): Result<Unit>

    suspend fun isWaitingPurchased(): Boolean

    interface ApplicationStateListener {
        fun onPurchased(deviceApp: DeviceApp)
        fun onDiscontinued(deviceApp: DeviceApp)
        fun onDeprecated(deviceApp: DeviceApp)
        fun onTrialPeriod(deviceApp: DeviceApp, expired: Boolean)
        fun onFailed(th: Throwable)
    }
}