package com.smartsolutions.paquetes.helpers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.jvm.Throws

class SimsHelper @Inject constructor(
    @ApplicationContext
    private val context: Context
) {


    private var subscriptionManager: SubscriptionManager? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
            subscriptionManager = ContextCompat
                .getSystemService(context, SubscriptionManager::class.java)
    }

    fun getActiveVoiceSimIndex(): Int = getActiveSimIndex(2)

    /**
     * Obtiene el índice de la tarjeta sim activa.
     *
     * @return 1 para la tarjeta sim del slot 1.
     * 2 para la tarjeta sim del slot 2.
     * -1 si no se pudo obtener el slot de la sim.
     * */
    fun getActiveDataSimIndex(): Int = getActiveSimIndex(1)


    @Throws(MissingPermissionException::class)
    private fun getActiveSimIndex(type: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
            ) {
                throw MissingPermissionException(Manifest.permission.READ_PHONE_STATE)
            }

            val info = when (type) {
                1 -> subscriptionManager?.getActiveSubscriptionInfo(
                    SubscriptionManager.getDefaultDataSubscriptionId()
                )
                2 -> subscriptionManager?.getActiveSubscriptionInfo(
                    SubscriptionManager.getDefaultVoiceSubscriptionId()
                )
                else -> subscriptionManager?.getActiveSubscriptionInfo(
                    SubscriptionManager.getDefaultVoiceSubscriptionId()
                )
            }

            return info?.simSlotIndex ?: 1
        }
        return 1
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun getActiveSimsInfo(): List<SubscriptionInfo>? {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        return subscriptionManager?.activeSubscriptionInfoList
    }

    fun isDualSim(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
            return (getActiveSimsInfo()?.size ?: 0) > 1
        return false
    }
}