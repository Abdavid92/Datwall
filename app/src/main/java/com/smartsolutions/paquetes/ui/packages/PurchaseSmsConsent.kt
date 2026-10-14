package com.smartsolutions.paquetes.ui.packages

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

/**
 * Espera el SMS de confirmación de una compra con la API SMS User Consent: durante 5 minutos,
 * el sistema pregunta al usuario si quiere compartir con la app el siguiente SMS que llegue de
 * un remitente que no está en sus contactos. La app solo recibe el texto si el usuario acepta,
 * y no necesita permisos de SMS.
 *
 * Sin Google Play Services no hace nada: la compra queda sin confirmar.
 *
 * Se crea al construir el fragmento, para registrar el lanzador de resultados a tiempo.
 * */
class PurchaseSmsConsent(
    private val fragment: Fragment,
    private val onSms: (smsBody: String) -> Unit
) : DefaultLifecycleObserver {

    /**
     * Contexto con el que se registró el receptor; null si no está registrado.
     * */
    private var registeredContext: Context? = null

    private val consentLauncher =
        fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)?.let(onSms)
            }
        }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != SmsRetriever.SMS_RETRIEVED_ACTION)
                return

            val status = IntentCompat.getParcelableExtra(intent, SmsRetriever.EXTRA_STATUS, Status::class.java)

            if (status?.statusCode == CommonStatusCodes.SUCCESS) {
                IntentCompat.getParcelableExtra(intent, SmsRetriever.EXTRA_CONSENT_INTENT, Intent::class.java)
                    ?.let { consentLauncher.launch(it) }
            }
            // Con TIMEOUT no se hace nada: la compra queda sin confirmar.
        }
    }

    init {
        fragment.lifecycle.addObserver(this)
    }

    /**
     * Empieza a esperar el SMS de la compra que se acaba de hacer.
     * */
    fun start() {
        val context = fragment.context ?: return

        try {
            SmsRetriever.getClient(context)
                .startSmsUserConsent(null)
                .addOnSuccessListener { register(context) }
        } catch (e: Exception) {
            // Sin Google Play Services
        }
    }

    private fun register(context: Context) {
        if (registeredContext != null)
            return

        val appContext = context.applicationContext
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
            SmsRetriever.SEND_PERMISSION,
            null,
            ContextCompat.RECEIVER_EXPORTED
        )
        registeredContext = appContext
    }

    override fun onDestroy(owner: LifecycleOwner) {
        registeredContext?.unregisterReceiver(receiver)
        registeredContext = null
    }
}
