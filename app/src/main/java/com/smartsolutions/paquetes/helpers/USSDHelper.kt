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
     * manera predeterminada tiene un valor de 3000
     * milisegundos.
     * */
    var timeout = 3000L

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
     * Ejecuta un código ussd y lanza un broadcast con el resultado
     * de la ejecución. El broadcast contendrá el cuerpo de la respuesta
     * o un código de error si la ejecución no tuvo éxito.
     *
     * @param ussd - Código ussd.
     * @param activityNewTask - Este parámetro se usa en api 24 para abajo.
     * Indica si se debe poner Intent.FLAG_ACTIVITY_NEW_TASK en el intent
     * que se usa para ejecutar el código ussd. Esto debe ser usado cuando
     * se intenta usar este método en un contexto que no sea una activity o un
     * fragment.
     * */
    /*fun sendUSSDRequest(ussd: String, activityNewTask: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sendUSSDRequestOreo(ussd)
        } else {
            sendUSSDRequestLegacy(ussd, activityNewTask)
        }
    }*/

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
    fun sendUSSDRequest(ussd: String, callback: Callback?, activityNewTask: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sendUSSDRequestOreo(ussd, callback)
        } else {
            sendUSSDRequestLegacy(ussd, activityNewTask, callback)
        }
    }

    /**
     * Abre las configuraciones del dispositivo para activar los servicios de
     * accesibilidad.
     *
     * @param activityNewTask - True si se usa este método en un contexto que no sea
     * una activity o un fragment.
     * */
    fun openAccessibilityServicesActivity(activityNewTask: Boolean = false) {

        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

        if (activityNewTask)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

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
                    callback?.onSuccess(response.toString())
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

    /*@RequiresApi(Build.VERSION_CODES.O)
    private fun sendUSSDRequestOreo(ussd: String) {
        ContextCompat.getSystemService(context, TelephonyManager::class.java)?.let {

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CALL_PHONE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                val intent = Intent(ACTION_SEND_USSD_REQUEST)
                    .putExtra(EXTRA_RESULT, false)
                    .putExtra(EXTRA_ERROR_CODE, 0)

                LocalBroadcastManager.getInstance(context)
                    .sendBroadcast(intent)

                return
            }

            it.sendUssdRequest(ussd, object : TelephonyManager.UssdResponseCallback() {

                override fun onReceiveUssdResponse(
                    telephonyManager: TelephonyManager?,
                    request: String?,
                    response: CharSequence?
                ) {
                    val intent = Intent(ACTION_SEND_USSD_REQUEST)
                        .putExtra(EXTRA_RESULT, true)
                        .putExtra(EXTRA_RESPONSE, response.toString())

                    LocalBroadcastManager.getInstance(context)
                        .sendBroadcast(intent)
                }

                override fun onReceiveUssdResponseFailed(
                    telephonyManager: TelephonyManager?,
                    request: String?,
                    failureCode: Int
                ) {

                    val intent = Intent(ACTION_SEND_USSD_REQUEST)
                        .putExtra(EXTRA_RESULT, false)

                    when (failureCode) {
                        TelephonyManager.USSD_RETURN_FAILURE -> {
                            intent.putExtra(EXTRA_ERROR_CODE, 2)
                        }
                        TelephonyManager.USSD_ERROR_SERVICE_UNAVAIL -> {
                            intent.putExtra(EXTRA_ERROR_CODE, 1)
                        }
                        else -> intent.putExtra(EXTRA_ERROR_CODE, 2)
                    }

                    LocalBroadcastManager.getInstance(context)
                        .sendBroadcast(intent)
                }
           }, Handler(Looper.getMainLooper()))
        }
    }*/

    private fun sendUSSDRequestLegacy(ussd: String, activityNewTask: Boolean, callback: Callback?) {
        if (!accessibilityServiceEnabled()) {
            callback?.onFail(3, errorMessages[3])
            return
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_DENIED) {
            callback?.onFail(0, errorMessages[0])
            return
        }

        var complete = false

        val intent = Intent(context, UIScannerService::class.java)
            .setAction(UIScannerService.ACTION_WAIT_USSD_CODE)

        context.startService(intent)

        val callIntent = Intent(Intent.ACTION_CALL)
            .setData(Uri.parse("tel:" + Uri.encode(ussd)))

        if (activityNewTask)
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(callIntent)

        val receiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                LocalBroadcastManager.getInstance(context)
                    .unregisterReceiver(this)
                complete = true

                val result = intent.getBooleanExtra(EXTRA_RESULT, false)
                val response = intent.getStringExtra(EXTRA_RESPONSE)

                if (result && response != null) {
                    callback?.onSuccess(response)
                } else {
                    callback?.onFail(2, errorMessages[2])
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

                callback?.onFail(4, errorMessages[4])

                LocalBroadcastManager.getInstance(context)
                    .unregisterReceiver(receiver)
            }
        }, timeout)
    }

    /*private fun sendUSSDRequestLegacy(ussd: String, activityNewTask: Boolean) {
        if (!accessibilityServiceEnabled()) {

            val intent = Intent(ACTION_SEND_USSD_REQUEST)
                .putExtra(EXTRA_RESULT, false)
                .putExtra(EXTRA_ERROR_CODE, 3)

            LocalBroadcastManager.getInstance(context)
                .sendBroadcast(intent)
            return
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_DENIED) {
            val intent = Intent(ACTION_SEND_USSD_REQUEST)
                .putExtra(EXTRA_RESULT, false)
                .putExtra(EXTRA_ERROR_CODE, 0)

            LocalBroadcastManager.getInstance(context)
                .sendBroadcast(intent)
            return
        }

        val intent = Intent(context, UIScannerService::class.java)
            .setAction(UIScannerService.ACTION_WAIT_USSD_CODE)

        context.startService(intent)

        val callIntent = Intent(Intent.ACTION_CALL)
            .setData(Uri.parse("tel:" + Uri.encode(ussd)))

        if (activityNewTask)
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(callIntent)

        val handler = Handler(Looper.getMainLooper())

        handler.postDelayed({
            val cancelIntent = Intent(context, UIScannerService::class.java)
                .setAction(UIScannerService.ACTION_CANCEL_WAIT_USSD_CODE)

            context.startService(cancelIntent)

            val broadcastErrorIntent = Intent(ACTION_SEND_USSD_REQUEST)
                .putExtra(EXTRA_RESULT, false)
                .putExtra(EXTRA_ERROR_CODE, 4)

            LocalBroadcastManager.getInstance(context)
                .sendBroadcast(broadcastErrorIntent)
        }, timeout)
    }*/

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
         * Este es un extra de tipo String.
         * */
        const val EXTRA_RESPONSE = "com.smartsolutions.paquetes.extra.RESPONSE"
    }

    interface Callback {

        fun onSuccess(response: String)

        fun onFail(errorCode: Int, message: String)
    }
}