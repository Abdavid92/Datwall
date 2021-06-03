package com.smartsolutions.paquetes.helpers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.jvm.Throws

/**
 * Ayudante para obtener datos de las lineas instaladas en el dispositivo.
 * */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
class SimsHelper @Inject constructor(
    @ApplicationContext
    private val context: Context
) {

    private val subscriptionManager = ContextCompat.getSystemService(context, SubscriptionManager::class.java)
        ?: throw NullPointerException()

    /**
     * Obtiene el índice de la tarjeta sim activa para las llamadas.
     *
     * @return 1 para la tarjeta sim del slot 1.
     * 2 para la tarjeta sim del slot 2.
     * -1 si no se pudo obtener el slot de la sim.
     * */
    @Throws(MissingPermissionException::class)
    @RequiresApi(Build.VERSION_CODES.N)
    fun getActiveVoiceSimIndex() = getActiveSim(2).simSlotIndex

    @Throws(MissingPermissionException::class)
    @RequiresApi(Build.VERSION_CODES.N)
    fun getActiveVoiceSimId(): String {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return getActiveSim(2).cardId.toString()
        }

        return getActiveSim(2).iccId
    }

    /**
     * Obtiene el índice de la tarjeta sim activa para los datos móviles.
     *
     * @return 1 para la tarjeta sim del slot 1.
     * 2 para la tarjeta sim del slot 2.
     * -1 si no se pudo obtener el slot de la sim.
     * */
    @Throws(MissingPermissionException::class)
    @RequiresApi(Build.VERSION_CODES.N)
    fun getActiveDataSimIndex() = getActiveSim(1).simSlotIndex

    @Throws(MissingPermissionException::class)
    @RequiresApi(Build.VERSION_CODES.N)
    fun getActiveDataSimId(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return getActiveSim(2).cardId.toString()
        }

        return getActiveSim(2).iccId
    }


    @Throws(MissingPermissionException::class)
    @RequiresApi(Build.VERSION_CODES.N)
    private fun getActiveSim(type: Int): SubscriptionInfo {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
        ) {
            throw MissingPermissionException(Manifest.permission.READ_PHONE_STATE)
        }

        return when (type) {
            1 -> subscriptionManager.getActiveSubscriptionInfo(
                SubscriptionManager.getDefaultDataSubscriptionId()
            )
            2 -> subscriptionManager.getActiveSubscriptionInfo(
                SubscriptionManager.getDefaultVoiceSubscriptionId()
            )
            else -> subscriptionManager.getActiveSubscriptionInfo(
                SubscriptionManager.getDefaultVoiceSubscriptionId()
            )
        }
    }


    private fun getActiveSimsInfo(): List<SubscriptionInfo>? {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        return subscriptionManager.activeSubscriptionInfoList
    }

    fun isDualSim(): Boolean {
        return (getActiveSimsInfo()?.size ?: 0) > 1
    }

    @Throws(MissingPermissionException::class)
    fun getSimIDByIndex(simIndex: Int): String {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw MissingPermissionException(Manifest.permission.READ_PHONE_STATE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(simIndex).cardId.toString()
        }
        return subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(simIndex).iccId
    }
}