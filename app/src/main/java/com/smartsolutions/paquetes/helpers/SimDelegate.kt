package com.smartsolutions.paquetes.helpers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.ExecutorCompat
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import com.smartsolutions.paquetes.internalDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.Throws

/**
 * Ayudante para obtener datos de las lineas instaladas en el dispositivo.
 * */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
class SimDelegate @Inject constructor(
    @ApplicationContext
    private val context: Context
): CoroutineScope {

    private lateinit var subscriptionManager: SubscriptionManager

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
            subscriptionManager = ContextCompat.getSystemService(context, SubscriptionManager::class.java)
                ?: throw NullPointerException()
    }

    /**
     * Obtiene el [SubscriptionInfo] de la linea por el tipo.
     *
     * @param type - Tipo de linea.
     *
     * @return [SubscriptionInfo]
     * */
    @Throws(MissingPermissionException::class)
    @RequiresApi(Build.VERSION_CODES.N)
    fun getActiveSim(type: SimType): SubscriptionInfo? {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
        ) {
            throw MissingPermissionException(Manifest.permission.READ_PHONE_STATE)
        }

        return try {
            when (type) {
                SimType.DATA -> subscriptionManager.getActiveSubscriptionInfo(
                    SubscriptionManager.getDefaultDataSubscriptionId()
                )
                SimType.VOICE -> subscriptionManager.getActiveSubscriptionInfo(
                    SubscriptionManager.getDefaultVoiceSubscriptionId()
                )
            }
        }catch (e: Exception){
            launch {
                context.internalDataStore.edit {
                    it[PreferencesKeys.IS_DUAL_SIM_BROKEN] = true
                }
            }
            null
        }
    }

    /**
     * Obtiene todas las lineas activas.
     * */
    @Throws(MissingPermissionException::class)
    fun getActiveSimsInfo(): List<SubscriptionInfo> {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
        ) {
            throw MissingPermissionException(Manifest.permission.READ_PHONE_STATE)
        }

        return subscriptionManager.activeSubscriptionInfoList
    }

    /**
     * Obtiene el id de la linea instalada en el slot dado.
     *
     * @param simIndex - Slot de la linea.
     * */
    @Throws(MissingPermissionException::class)
    fun getSimBySlotIndex(simIndex: Int): SubscriptionInfo? {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw MissingPermissionException(Manifest.permission.READ_PHONE_STATE)
        }
        return subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(simIndex)
    }

    fun getSimId(subscriptionInfo: SubscriptionInfo): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            subscriptionInfo.subscriptionId.toString()
        else
            subscriptionInfo.subscriptionId.toString()

    @MainThread
    fun addOnSubscriptionsChangedListener(
        listener: SubscriptionManager.OnSubscriptionsChangedListener
    ) {
        subscriptionManager.addOnSubscriptionsChangedListener(listener)
    }

    @MainThread
    fun removeOnSubscriptionsChangedListener(
        listener: SubscriptionManager.OnSubscriptionsChangedListener
    ) {
        subscriptionManager.removeOnSubscriptionsChangedListener(listener)
    }

    fun getSubcriptionInfo(subscriptionId: Int): SubscriptionInfo? {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        return subscriptionManager.getActiveSubscriptionInfo(subscriptionId)
    }

    enum class SimType {
        VOICE,
        DATA
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default
}