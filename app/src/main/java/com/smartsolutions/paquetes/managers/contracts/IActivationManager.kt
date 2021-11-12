package com.smartsolutions.paquetes.managers.contracts

import androidx.lifecycle.LiveData
import com.smartsolutions.paquetes.serverApis.models.AndroidApp
import com.smartsolutions.paquetes.serverApis.models.License
import com.smartsolutions.paquetes.serverApis.models.Result

interface IActivationManager {

    /**
     * [LiveData] que contiene el resultado de la confirmación de la compra.
     * */
    val onConfirmPurchase: LiveData<Result<Unit>>

    /**
     * Indica si la aplicación puede trabajar dependiendo del
     * estado. Este método es seguro para llamarlo constantemente porque usa
     * una cache que se llena la primera vez que se usa y se mantiene actualizada por
     * el método [getLicense]
     *
     * @return [Pair]
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


    suspend fun isWaitingPurchase(): Boolean


    suspend fun setWaitingPurchase(value: Boolean)

    /**
     * Obtiene la licencia del servidor y la guarda en el dataStore.
     *
     * @return [Result]
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