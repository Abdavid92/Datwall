package com.smartsolutions.paquetes.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.smartsolutions.paquetes.managers.IDataPackageManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Este broadcast va a recivir todos los mensajes de Cubacel.
 * Va a llamar a IDataPackageManager para que registre un nuevo paquete
 * si se encuentra.
 * */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    /**
     * IDataPackageManager encargado de registrar los nuevos
     * paquetes comprados.
     * */
    @Inject
    lateinit var dataPackageManager: IDataPackageManager

    override fun onReceive(context: Context, intent: Intent) {
        val sms: Array<SmsMessage>? = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        var body = ""
        var number: String? = null

        sms?.forEach {
            body += it.messageBody

            if (number == null)
                number = it.originatingAddress
        }

        number?.let {

            if (it.equals("cubacel", true)) {
                dataPackageManager.registerDataPackage(body)
            }
        }
    }
}