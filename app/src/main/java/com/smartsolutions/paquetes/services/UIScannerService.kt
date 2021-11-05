
package com.smartsolutions.paquetes.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.smartsolutions.paquetes.DatwallApplication
import com.smartsolutions.paquetes.helpers.USSDHelper
import java.util.*

/**
 * Servicio de accesibilidad encargado de obtener el resultado de los
 * códigos ussd.
 * */
class UIScannerService : AccessibilityService() {

    /**
     * Indica si se está esperando el resultado de un código ussd
     * */
    private var waitingUssdCodeResult = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {
            ACTION_WAIT_USSD_CODE -> waitingUssdCodeResult = true
            ACTION_CANCEL_WAIT_USSD_CODE -> waitingUssdCodeResult = false
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        setServiceStatus(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {

        val response = event.text.toTypedArray()

        /*
        * Si se está esperando el resultado de un código ussd y
        * es el código esperado.
        * */
        if (waitingUssdCodeResult && isExpectedUSSDCode(response)) {
            //Cancelo la espera.
            waitingUssdCodeResult = false

            //Preparo el intent.
            val intent = Intent(USSDHelper.ACTION_SEND_USSD_REQUEST)
                .putExtra(USSDHelper.EXTRA_RESULT, true)
                .putExtra(USSDHelper.EXTRA_RESPONSE, response)

            //Lanzo el broadcast.
            sendBroadcast(intent)

            tryCloseDialog(event)
        }
    }

    private fun tryCloseDialog(event: AccessibilityEvent) {
        var nodes = event.source
            ?.findAccessibilityNodeInfosByText(
                getString(android.R.string.cancel)
                    .uppercase(Locale.ROOT)
            )

        if (nodes == null || nodes.isEmpty())
            nodes = event.source
                ?.findAccessibilityNodeInfosByText(getString(android.R.string.cancel))

        if (nodes?.isNotEmpty() == true) {
            nodes.forEach {
                it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        } else {
            //Presiono el botón atras para cerrar el diálogo.
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    /**
     * Indica si es o no el resultado esperado.
     * */
    private fun isExpectedUSSDCode(textList: Array<CharSequence>): Boolean {
        val phoneServiceName = packageManager
            .getApplicationLabel(packageManager.getApplicationInfo("com.android.phone", 0))

        var string = ""

        textList.forEach {
            string += it.toString()
        }

        return !string.contains(phoneServiceName, true) &&
                !string.contains("ussd", true)
    }

    override fun onInterrupt() {
        setServiceStatus(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        setServiceStatus(false)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        setServiceStatus(false)
        return super.onUnbind(intent)
    }

    private fun setServiceStatus(ready: Boolean) {
        (application as DatwallApplication).uiScannerServiceReady = ready
    }

    companion object {

        /**
         * Activa la espera del resultado de la ejecución de un código ussd.
         * */
        const val ACTION_WAIT_USSD_CODE = "com.smartsolutions.paquetes.action.WAIT_USSD_CODE"

        /**
         * Cancela la espera del resultado de la ejecución de un código ussd.
         * */
        const val ACTION_CANCEL_WAIT_USSD_CODE = "com.smarsolutions.paquetes.action.CANCEL_WAIT_USSD_CODE"
    }
}