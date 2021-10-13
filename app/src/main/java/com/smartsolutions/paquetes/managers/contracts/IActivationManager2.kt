package com.smartsolutions.paquetes.managers.contracts

import com.smartsolutions.paquetes.serverApis.models.AndroidApp
import com.smartsolutions.paquetes.serverApis.models.DeviceApp
import com.smartsolutions.paquetes.serverApis.models.License
import com.smartsolutions.paquetes.serverApis.models.Result

interface IActivationManager2 {

    /**
     * Indica si la aplicación puede trabajar dependiendo del
     * estado.
     * */
    suspend fun canWork(): Pair<Boolean, ApplicationStatuses>

    /**
     * Revisa con el dataStore si la aplicación sigue en periodo de prueba.
     * */
    suspend fun isInTrialPeriod(): Boolean

    /**
     * Verifica el estado de la aplicación y lanza el evento correspondiente.
     *
     * @param listener - Listener de eventos.
     * */
    fun getApplicationStatus(listener: ApplicationStatusListener)

    /**
     * Transfiere el crédito por código ussd.
     *
     * @param key - Clave de transferencia.
     * @param androidApp - [AndroidApp] con el precio y el número a transferir.
     *
     * @return [Result] Resultado de la transferencia.
     * */
    suspend fun transferCreditByUSSD(key: String, license: License): Result<Unit>

    /**
     * Confirma y activa la aplicación.
     * */
    suspend fun confirmPurchase(smsBody: String, phone: String, simIndex: Int): Result<Unit>

    /**
     * Indica si se está esperando la confirmación de la compra de licencia.
     * */
    suspend fun isWaitingPurchased(): Boolean

    /**
     * Obtiene la licencia del servidor
     * */
    suspend fun getLicense(): Result<License>

    /**
     * Obtiene la licencia local guardada en el dataStore
     * */
    suspend fun getLocalLicense(): License?

    interface ApplicationStatusListener {
        /**
         * La aplicación ya ha sido comprada y no está obsoleta.
         * */
        fun onPurchased(license: License)
        /**
         * Ha sido descontinuada y no ha sido comprada.
         * */
        fun onDiscontinued(license: License)
        /**
         * Está obsoleta. Hay que actualizar.
         * */
        fun onDeprecated(license: License)
        /**
         * Está en periodo de prueba.
         *
         * @param isTrialPeriod - Indica si el periodo de prueba no ha expirado.
         * */
        fun onTrialPeriod(license: License, isTrialPeriod: Boolean)
        /**
         * La licensia está demasiado vieja. Hay que conectar al servidor para
         * actualizarlo.
         * */
        fun onTooMuchOld(license: License)
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
        /**
         * DeviceApp demasiado antiguo.
         * */
        TooMuchOld,
    }
}