package com.smartsolutions.paquetes.helpers

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.services.UIScannerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.GlobalScope
import javax.inject.Inject

/**
 * Clase ayudante que se usa para ejecutar códigos ussd.
 * */
class USSDHelper @Inject constructor(
    @ApplicationContext
    private val context: Context
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
     * Mensajes de los diferentes errores. Los índices
     * de este arreglo coinciden con los códigos de error emitidos
     * junto con el broadcast lanzado.
     * */
    val errorMessages = arrayOf(
        context.getString(R.string.missing_call_permission), //Permiso de realizar llamadas denegado
        context.getString(R.string.telephony_service_unavailable), //El servicio de telefonía no está disponible
        context.getString(R.string.ussd_return_failure), //El código ussd falló
        context.getString(R.string.accessibility_service_unavailable), //Servicio de accesibilidad inactivo
        context.getString(R.string.response_timeout) //Se ha agotado el tiempo de espera
    )

    /**
     * Ejecuta un código ussd y retorna un callback con el resultado
     * de la ejecución.
     *
     * @param ussd - Código ussd.
     * @param callback - Callback con el resultado.
     * @param activityNewTask - Este parámetro se usa en api 24 para abajo.
     * Indica si se debe poner Intent.FLAG_ACTIVITY_NEW_TASK en el intent
     * que se usa para ejecutar el código ussd. Esto debe ser usado cuando
     * se intenta usar este método en un contexto que no sea una activity o un
     * fragment.
     * */
    fun sendUSSDRequest(ussd: String, callback: Callback?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sendUSSDRequestOreo(ussd, callback)
        } else {
            sendUSSDRequestLegacy(ussd, callback)
        }
    }

    /**
     * Abre las configuraciones del dispositivo para activar los servicios de
     * accesibilidad.
     *
     * @param activityNewTask - True si se usa este método en un contexto que no sea
     * una activity o un fragment.
     * */
    fun openAccessibilityServicesActivity() {

        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendUSSDRequestOreo(ussd: String, callback: Callback?) {
        ContextCompat.getSystemService(context, TelephonyManager::class.java)?.let {

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CALL_PHONE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                callback?.onFail(0, errorMessages[0])
                return
            }

            it.sendUssdRequest(ussd, object : TelephonyManager.UssdResponseCallback() {

                override fun onReceiveUssdResponse(
                    telephonyManager: TelephonyManager?,
                    request: String?,
                    response: CharSequence?
                ) {
                    if (response != null) {
                        callback?.onSuccess(arrayOf(response))
                    } else {
                        callback?.onSuccess(arrayOf())
                    }
                }

                override fun onReceiveUssdResponseFailed(
                    telephonyManager: TelephonyManager?,
                    request: String?,
                    failureCode: Int
                ) {
                    var code = 2

                    when (failureCode) {
                        TelephonyManager.USSD_RETURN_FAILURE -> {
                            code = 2
                        }
                        TelephonyManager.USSD_ERROR_SERVICE_UNAVAIL -> {
                            code = 1
                        }
                    }

                    callback?.onFail(code, errorMessages[code])
                }
            }, Handler(Looper.getMainLooper()))
        }
    }

    /**
     * Ejecuta un código ussd y retorna un callback.
     * */
    fun sendUSSDRequestLegacy(ussd: String, callback: Callback? = null) {
        if (ActivityCompat
                .checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_DENIED) {
            callback?.onFail(0, errorMessages[0])
            return
        }

        callback?.let {
            if (!accessibilityServiceEnabled()) {
                it.onFail(3, errorMessages[3])
                return
            }

            var complete = false

            val intent = Intent(context, UIScannerService::class.java)
                .setAction(UIScannerService.ACTION_WAIT_USSD_CODE)

            context.startService(intent)

            val receiver = object : BroadcastReceiver() {

                override fun onReceive(context: Context, intent: Intent) {
                    LocalBroadcastManager.getInstance(context)
                        .unregisterReceiver(this)
                    complete = true

                    val result = intent.getBooleanExtra(EXTRA_RESULT, false)
                    val response = intent.getCharSequenceArrayExtra(EXTRA_RESPONSE)

                    if (result && response != null) {
                        it.onSuccess(response)
                    } else {
                        it.onFail(2, errorMessages[2])
                    }
                }
            }

            val filter = IntentFilter(ACTION_SEND_USSD_REQUEST)

            LocalBroadcastManager.getInstance(context)
                .registerReceiver(receiver, filter)

            val handler = Handler(Looper.getMainLooper())

            handler.postDelayed({
                if (!complete) {
                    val cancelIntent = Intent(context, UIScannerService::class.java)
                        .setAction(UIScannerService.ACTION_CANCEL_WAIT_USSD_CODE)

                    context.startService(cancelIntent)

                    it.onFail(4, errorMessages[4])

                    LocalBroadcastManager.getInstance(context)
                        .unregisterReceiver(receiver)
                }
            }, timeout)
        }

        val callIntent = Intent(Intent.ACTION_CALL)
            .setData(Uri.parse("tel:" + Uri.encode(ussd)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(callIntent)
    }

    private fun accessibilityServiceEnabled(): Boolean {
        val pref = Settings.Secure
            .getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)

        val serviceName = UIScannerService::class.qualifiedName

        return pref != null &&
                pref.contains(context.packageName + "/" + serviceName)
    }

    companion object {
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
         * Código de error en caso de no tener éxito. Este código coincide
         * con el orden del arreglo errorMessages. Este es un extra de
         * tipo Int
         *
         * @see errorMessages
         * */
        const val EXTRA_ERROR_CODE = "com.smartsolutions.paquetes.extra.ERROR_CODE"

        /**
         * Cuerpo de la respuesta en caso de tener éxito.
         * Este es un extra de tipo Array<CharSequence>.
         * */
        const val EXTRA_RESPONSE = "com.smartsolutions.paquetes.extra.RESPONSE"

        const val MISSING_CALL_PERMISSION = 0
        const val TELEPHONY_SERVICE_UNAVAILABLE = 1
        const val USSD_CODE_FAILED = 2
        const val ACCESSIBILITY_SERVICE_UNAVAILABLE = 3
        const val CONNECTION_TIMEOUT = 4
    }

    interface Callback {

        fun onSuccess(response: Array<CharSequence>)

        fun onFail(errorCode: Int, message: String)
    }
}