package com.smartsolutions.paquetes.helpers

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import com.smartsolutions.paquetes.internalDataStore
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.services.BubbleFloatingService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BubbleServiceHelper @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val activationManager: IActivationManager,
    private val permissionsManager: IPermissionsManager,
    private val notificationHelper: NotificationHelper
) {

    @Throws(MissingPermissionException::class)
    suspend fun startBubble(turnOn: Boolean = false) {
        if (turnOn)
            writeChangesDataStore(true)

        if (DatwallKernel.DATA_MOBILE_ON && activationManager.canWork().first) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val permission =
                    permissionsManager.findPermission(IPermissionsManager.DRAW_OVERLAYS_CODE)
                if (permission?.checkPermission?.invoke(permission, context) != true) {
                    stopBubble(true)
                    notifyStop()
                    return
                }
            }

            kotlin.runCatching {
                context.startService(Intent(context, BubbleFloatingService::class.java))
            }
        }
    }

    suspend fun stopBubble(turnOf: Boolean = false) {
        if (turnOf)
            writeChangesDataStore(false)

        kotlin.runCatching {
            context.stopService(Intent(context, BubbleFloatingService::class.java))
        }
    }

    fun notifyStop(){
        notificationHelper.notify(
            NotificationHelper.ALERT_NOTIFICATION_ID,
            notificationHelper.buildNotification(
                NotificationHelper.ALERT_CHANNEL_ID
            ).apply {
                setContentTitle(context.getString(R.string.bubble_stoped_permission_title))
                setContentText(context.getString(R.string.bubble_stoped_permission_description))
            }.build()
        )
    }

    private suspend fun writeChangesDataStore(
        enabled: Boolean
    ) {
        withContext(Dispatchers.IO) {
            context.internalDataStore.edit {
                it[PreferencesKeys.ENABLED_BUBBLE_FLOATING] = enabled
            }
        }
    }
}