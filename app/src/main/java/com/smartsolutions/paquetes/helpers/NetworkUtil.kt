package com.smartsolutions.paquetes.helpers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.jvm.Throws

class NetworkUtil @Inject constructor(
    @ApplicationContext
    private val context: Context
) {

    private val connectivityManager = ContextCompat
        .getSystemService(context, ConnectivityManager::class.java) ?: throw NullPointerException()

    private val telephonyManager = ContextCompat
        .getSystemService(context, TelephonyManager::class.java) ?: throw NullPointerException()

    @Throws(MissingPermissionException::class)
    fun getNetworkGeneration(): NetworkType {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                throw MissingPermissionException(Manifest.permission.READ_PHONE_STATE)
            }
            getNetworkGeneration(telephonyManager.dataNetworkType)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            getNetworkGeneration(networkInfo?.subtype ?: -1)
        }
    }

    private fun getNetworkGeneration(networkType: Int): NetworkType {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_1xRTT,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_IDEN,
            TelephonyManager.NETWORK_TYPE_GSM -> NetworkType.NETWORK_2G
            TelephonyManager.NETWORK_TYPE_EHRPD,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_TD_SCDMA -> NetworkType.NETWORK_3G
            TelephonyManager.NETWORK_TYPE_LTE,
            TelephonyManager.NETWORK_TYPE_IWLAN -> NetworkType.NETWORK_4G
            TelephonyManager.NETWORK_TYPE_NR -> NetworkType.NETWORK_5G
            else -> NetworkType.NETWORK_UNKNOWN
        }
    }

    /**
     * Tipo de red movil.
     * */
    enum class NetworkType {
        NETWORK_5G,
        NETWORK_4G,
        NETWORK_3G,
        NETWORK_2G,
        NETWORK_UNKNOWN
    }
}