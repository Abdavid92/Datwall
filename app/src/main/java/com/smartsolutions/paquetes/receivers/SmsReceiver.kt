package com.smartsolutions.paquetes.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsMessage
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Este broadcast va a recivir todos los mensajes de Cubacel.
 * Va a llamar a IDataPackageManager para que registre un nuevo paquete
 * si se encuentra.
 * */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * [IDataPackageManager] encargado de registrar los nuevos
     * paquetes comprados.
     * */
    @Inject
    lateinit var dataPackageManager: IDataPackageManager

    override fun onReceive(context: Context, intent: Intent) {
        val sms: Array<SmsMessage>? = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        var body = ""
        var number: String? = null

        val extras = intent.extras
        var simIndex = -1

        extras?.let {
            simIndex = getSimIndex(it)
        }

        sms?.forEach {
            body += it.messageBody

            if (number == null)
                number = it.originatingAddress
        }

        number?.let { phoneNumber ->

            scope.launch {
                if (phoneNumber.equals("cubacel", true))
                    dataPackageManager.registerDataPackage(body, simIndex)
            }
        }
    }

    private fun getSimIndex(bundle: Bundle): Int {
        try {
            var slot = -1
            val keySet = bundle.keySet()
            for (key in keySet) {
                when (key) {
                    "slot" -> slot = bundle.getInt("slot", -1)
                    "simId" -> slot = bundle.getInt("simId", -1)
                    "simSlot" -> slot = bundle.getInt("simSlot", -1)
                    "slot_id" -> slot = bundle.getInt("slot_id", -1)
                    "simnum" -> slot = bundle.getInt("simnum", -1)
                    "slotId" -> slot = bundle.getInt("slotId", -1)
                    "slotIdx" -> slot = bundle.getInt("slotIdx", -1)
                    "android.telephony.extra.SLOT_INDEX" -> slot =
                        bundle.getInt("android.telephony.extra.SLOT_INDEX", -1)
                    else -> if (key.contains("slot", true) || key.contains("sim", true)
                    ) {
                        val value = bundle.getString(key, "-1")
                        if ((value == "0") || (value == "1") || (value == "2")) {
                            slot = bundle.getInt(key, -1)
                        }
                    }
                }
            }
            return slot
        } catch (e: Exception) {

        }
        return -1
    }
}