package com.smartsolutions.paquetes.ui

import androidx.annotation.StringRes
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.helpers.USSDHelper

/**
 * Textos de los errores del dominio.
 * */
object ErrorTexts {

    /**
     * Texto de un código de error de USSD (constantes de [USSDHelper]).
     * */
    @StringRes
    fun ussd(errorCode: Int): Int = when (errorCode) {
        USSDHelper.DENIED_CALL_PERMISSION -> R.string.missing_call_permission
        USSDHelper.TELEPHONY_SERVICE_UNAVAILABLE -> R.string.telephony_service_unavailable
        USSDHelper.ACCESSIBILITY_SERVICE_UNAVAILABLE -> R.string.accessibility_service_unavailable
        USSDHelper.CONNECTION_TIMEOUT -> R.string.response_timeout
        USSDHelper.USSD_TOO_MANY_REQUESTS -> R.string.ussd_too_many_requests
        else -> R.string.ussd_return_failure
    }
}
