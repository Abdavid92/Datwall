package com.smartsolutions.paquetes.managers.contracts

import com.smartsolutions.paquetes.serverApis.models.Device
import com.smartsolutions.paquetes.serverApis.models.DeviceApp
import com.smartsolutions.paquetes.serverApis.models.Result

interface IActivationManager {

    /**
     * Indica si la aplicación puede trabajar dependiendo de
     * si está comprada o si está en periodo de prueba.
     * */
    suspend fun canWork(): Pair<Boolean, ApplicationStatuses>

    /**
     * Revisa con el dataStore si la aplicación sigue en periodo de prueba.
     * */
    suspend fun isInTrialPeriod(): Boolean

    /**
     * Obtiene el dispositivo del servidor.
     * */
    suspend fun getDevice(): Result<Device>

    /**
     * Obtiene la aplicación instalada en este dispòsitivo del servidor.
     * */
    suspend fun getDeviceApp(): Result<DeviceApp>

    /**
     * Obtiene el deviceApp guardado en el dataStore
     * */
    suspend fun getSaveDeviceApp(): DeviceApp?

    /**
     * Verifica el estado de la aplicación y lanza el evento correspondiente.
     *
     * @param listener - Listener de eventos.
     * */
    fun getApplicationStatus(listener: ApplicationStatusListener)

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


    interface ApplicationStatusListener {
        /**
         * La aplicación ya ha sido comprada y no está obsoleta.
         * */
        fun onPurchased(deviceApp: DeviceApp)
        /**
         * Ha sido descontinuada y no ha sido comprada.
         * */
        fun onDiscontinued(deviceApp: DeviceApp)
        /**
         * Está obsoleta. Hay que actualizar.
         * */
        fun onDeprecated(deviceApp: DeviceApp)
        /**
         * Está en periodo de prueba.
         *
         * @param isTrialPeriod - Indica si el periodo de prueba no ha expirado.
         * */
        fun onTrialPeriod(deviceApp: DeviceApp, isTrialPeriod: Boolean)
        /**
         * Falló la conexión.
         * */
        fun onFailed(th: Throwable)
    }

    /**
     * Estados de la aplicación.
     * */
    enum class ApplicationStatuses {
        /**
         * Ha sido comprada.
         * */
        Purchased,
        /**
         * Está en periodo de prueba o expiró el periodo de
         * prueba.
         * */
        TrialPeriod,
        /**
         * Ha sido descontinuada.
         * */
        Discontinued,
        /**
         * Obsoleto.
         * */
        Deprecated,
        /**
         * Desconocido.
         * */
        Unknown,
    }
}