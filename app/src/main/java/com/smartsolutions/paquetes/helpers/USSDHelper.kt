package com.smartsolutions.paquetes.helpers

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.abdavid92.persistentlog.Log
import com.smartsolutions.paquetes.exceptions.USSDRequestException
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.services.UIScannerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.jvm.Throws

/**
 * Clase ayudante que se usa para ejecutar códigos ussd.
 * */
class USSDHelper @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val accessibilityServiceHelper: AccessibilityServiceHelper
) {

    /**
     * Tiempo de espera antes de notificar que
     * falló la ejecución del código ussd. Esta
     * propiedad se usa en api 24 para abajo. De
     * manera predeterminada tiene un valor de 20000
     * milisegundos.
     * */
    var timeout = 20000L


    /**
     * Ejecuta un código ussd.
     *
     * @param ussd - Código ussd.
     * @param sim - Línea por la que se envía. Si es null se usa la del sistema.
     *
     * @return Array<CharSequence> con el cuerpo de la respuesta.
     * */
    @Throws(USSDRequestException::class)
    suspend fun sendUSSDRequest(ussd: String, sim: Sim? = null): Array<CharSequence> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sendUSSDRequestOreo(ussd, sim)
        } else {
            sendUSSDRequestLegacy(ussd, true, sim)!!
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun sendUSSDRequestOreo(ussd: String, sim: Sim?): Array<CharSequence> {

        return suspendCancellableCoroutine {
            ContextCompat.getSystemService(context, TelephonyManager::class.java)?.let { defaultManager ->

                //Si el permiso de llamadas está denegado lanzo un error.
                if (!callPermissionGranted()) {
                    it.resumeWithException(USSDRequestException(DENIED_CALL_PERMISSION))
                    return@suspendCancellableCoroutine
                }

                val telephonyManager = subscriptionIdOf(sim)?.let { subscriptionId ->
                    defaultManager.createForSubscriptionId(subscriptionId)
                } ?: defaultManager

                telephonyManager.sendUssdRequest(ussd, object : TelephonyManager.UssdResponseCallback() {
                    override fun onReceiveUssdResponse(
                        telephonyManager: TelephonyManager?,
                        request: String?,
                        response: CharSequence?
                    ) {
                        if (response != null) {
                            it.resume(arrayOf(response))
                        } else {
                            it.resumeWithException(USSDRequestException(USSD_CODE_FAILED))
                        }
                    }

                    override fun onReceiveUssdResponseFailed(
                        telephonyManager: TelephonyManager?,
                        request: String?,
                        failureCode: Int
                    ) {
                        var code = USSD_CODE_FAILED

                        when (failureCode) {
                            TelephonyManager.USSD_RETURN_FAILURE -> {
                                code = USSD_CODE_FAILED
                            }
                            TelephonyManager.USSD_ERROR_SERVICE_UNAVAIL -> {
                                code = TELEPHONY_SERVICE_UNAVAILABLE
                            }
                        }

                        Log.w("USSD Code Failed", "Code Failure: $failureCode Request: $request")

                        it.resumeWithException(USSDRequestException(code))
                    }
                }, Handler(Looper.getMainLooper()))
            }
        }
    }

    /**
     * Ejecuta un código ussd. Este método usa un servicio de accesibilidad para
     * leer el cuerpo de la respuesta en caso de que se pida.
     *
     * @param ussd - Código ussd a ejecutar.
     * @param readResponse - Indica si se debe leer el cuerpo de la respuesta. En caso
     * de que sea `true`, el servicio de accesibilidad debe estar encendido. En caso
     * contrario, no se verficará si el servicio está encendido y por lo tanto no se producirá
     * un error si este servicio está apagado.
     * @param sim - Línea por la que se marca. Si es null o no se encuentra su cuenta,
     * el sistema decide (o pregunta).
     *
     * @return Un Array de CharSequence con el cuerpo de la respuesta o null si la variable
     * readResponse es `false`, osea, no se quiere leer el cuerpo de la respuesta.
     *
     * @throws USSDRequestException Si se va a leer el cuerpo de la respuesta, el servicio de
     * accesibilidad debe estar encendido o se lanzará una excepción.
     * El permiso de realizar llamadas debe estar concedido para que no se lanze un excepción.
     * Si la respuesta se demora más del tiempo dado en la propiedad timeout se lanza una excepción.
     * */
    @Throws(USSDRequestException::class)
    suspend fun sendUSSDRequestLegacy(
        ussd: String,
        readResponse: Boolean = true,
        sim: Sim? = null
    ): Array<CharSequence>? {
        //Si el permiso de llamadas está denegado lanzo un error.
        if (!callPermissionGranted())
            throw USSDRequestException(DENIED_CALL_PERMISSION)

        //Intent para ejecutar el código ussd.
        val callIntent = Intent(Intent.ACTION_CALL)
            .setData(Uri.parse("tel:" + Uri.encode(ussd)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        phoneAccountOf(sim)?.let {
            callIntent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it)
        }

        val accessibilityServiceEnabled = accessibilityServiceHelper
            .accessibilityServiceEnabled()

        //Si se quiere leer la respuesta.
        if (readResponse) {
            return suspendCancellableCoroutine {

                //Si el servicio de accesibilidad está apagado lanzo un error.
                if (!accessibilityServiceEnabled) {
                    it.resumeWithException(USSDRequestException(ACCESSIBILITY_SERVICE_UNAVAILABLE))
                } else {
                    val intent = Intent(context, UIScannerService::class.java)
                        .setAction(UIScannerService.ACTION_WAIT_USSD_CODE)

                    context.startService(intent)

                    val receiver = object : BroadcastReceiver() {

                        override fun onReceive(context: Context, intent: Intent) {
                            context.unregisterReceiver(this)

                            val result = intent.getBooleanExtra(EXTRA_RESULT, false)
                            val response = intent.getCharSequenceArrayExtra(EXTRA_RESPONSE)

                            if (result && response != null) {
                                it.resume(response)
                            } else {
                                it.resumeWithException(
                                    USSDRequestException(USSD_CODE_FAILED)
                                )
                            }
                        }
                    }

                    registerResponseReceiver(context, receiver)

                    val handler = Handler(Looper.getMainLooper())

                    handler.postDelayed({
                        if (!it.isCompleted) {
                            val cancelIntent = Intent(context, UIScannerService::class.java)
                                .setAction(UIScannerService.ACTION_CANCEL_WAIT_USSD_CODE)

                            context.startService(cancelIntent)

                            context.unregisterReceiver(receiver)

                            it.resumeWithException(
                                USSDRequestException(CONNECTION_TIMEOUT)
                            )
                        }
                    }, timeout)

                    context.startActivity(callIntent)
                }
            }
        } else {
            context.startActivity(callIntent)
            return null
        }
    }

    /**
     * El id de la [Sim] es el subscriptionId. La sim embebida (sin id numérico) no tiene.
     * */
    private fun subscriptionIdOf(sim: Sim?): Int? = sim?.id?.toIntOrNull()

    /**
     * Cuenta de llamadas de la línea, para que ACTION_CALL no use la SIM de voz por defecto.
     * */
    @SuppressLint("MissingPermission")
    private fun phoneAccountOf(sim: Sim?): PhoneAccountHandle? {
        val subscriptionId = subscriptionIdOf(sim) ?: return null

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED)
            return null

        val accounts = ContextCompat.getSystemService(context, TelecomManager::class.java)
            ?.callCapablePhoneAccounts ?: return null

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val telephonyManager = ContextCompat.getSystemService(context, TelephonyManager::class.java)
                ?: return null
            accounts.firstOrNull { telephonyManager.getSubscriptionId(it) == subscriptionId }
        } else {
            // Antes de Android 11 el id de la cuenta suele ser el subscriptionId o el iccId.
            val iccId = ContextCompat.getSystemService(context, SubscriptionManager::class.java)
                ?.getActiveSubscriptionInfo(subscriptionId)?.iccId
            accounts.firstOrNull { it.id == subscriptionId.toString() || it.id == iccId }
        }
    }

    private fun callPermissionGranted(): Boolean =
        ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                PackageManager.PERMISSION_GRANTED

    companion object {

        /**
         * Registra el receiver de la respuesta del código ussd. No es exportado:
         * solo recibe el broadcast de [UIScannerService].
         * */
        fun registerResponseReceiver(context: Context, receiver: BroadcastReceiver) {
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(ACTION_SEND_USSD_REQUEST),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }

        /**
         * Crea el broadcast con la respuesta del código ussd, dirigido solo a esta app.
         * */
        fun createResponseIntent(context: Context, response: Array<CharSequence>): Intent =
            Intent(ACTION_SEND_USSD_REQUEST)
                .setPackage(context.packageName)
                .putExtra(EXTRA_RESULT, true)
                .putExtra(EXTRA_RESPONSE, response)
        /**
         * Broadcast que se lanza con el resultado de la ejecución del
         * código ussd.
         * */
        const val ACTION_SEND_USSD_REQUEST = "com.smartsolutions.paquetes.action.SEND_USSD_REQUEST"

        /**
         * Boolean que indica si se tuvo éxito. En api 24 para abajo no
         * se tiene en cuenta si la operadora telefónica retornó un mensaje
         * de error. Esto se debe manejar en el consumidor del broadcast.
         * */
        const val EXTRA_RESULT = "com.smartsolutions.paquetes.extra.RESULT"

        /**
         * Código de error en caso de no tener éxito (una de las constantes de
         * error de esta clase). Este es un extra de tipo Int.
         * */
        const val EXTRA_ERROR_CODE = "com.smartsolutions.paquetes.extra.ERROR_CODE"

        /**
         * Cuerpo de la respuesta en caso de tener éxito.
         * Este es un extra de tipo Array<CharSequence>.
         * */
        const val EXTRA_RESPONSE = "com.smartsolutions.paquetes.extra.RESPONSE"

        /**
         * Permiso para realizar llamadas denegado.
         * */
        const val DENIED_CALL_PERMISSION = 0
        /**
         * El servicio de telefonía no está disponible.
         * */
        const val TELEPHONY_SERVICE_UNAVAILABLE = 1
        /**
         * El código ussd falló.
         * */
        const val USSD_CODE_FAILED = 2
        /**
         * Servicio de accesibilidad inactivo.
         * */
        const val ACCESSIBILITY_SERVICE_UNAVAILABLE = 3
        /**
         * Se ha agotado el tiempo de espera.
         * */
        const val CONNECTION_TIMEOUT = 4

        /**
         * Demasiadas peticiones de USSD seguidas (el operador responde "MMI").
         * */
        const val USSD_TOO_MANY_REQUESTS = 5
    }
}