package com.smartsolutions.paquetes.annotations

import androidx.annotation.StringDef
import com.smartsolutions.paquetes.annotations.Networks.Companion.NETWORK_3G
import com.smartsolutions.paquetes.annotations.Networks.Companion.NETWORK_3G_4G
import com.smartsolutions.paquetes.annotations.Networks.Companion.NETWORK_4G

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@StringDef(NETWORK_3G_4G, NETWORK_4G, NETWORK_3G)
annotation class Networks {
    companion object {
        const val NETWORK_3G_4G = "3G_4G"
        const val NETWORK_4G = "4G"
        const val NETWORK_3G = "3G"
    }
}