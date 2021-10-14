package com.smartsolutions.paquetes.helpers

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import com.smartsolutions.paquetes.managers.ActivationManager2
import com.smartsolutions.paquetes.managers.PermissionsManager
import com.smartsolutions.paquetes.managers.contracts.IActivationManager2
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.services.BubbleFloatingService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.Throws

class BubbleServiceHelper @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val activationManager: IActivationManager2,
    private val permissionsManager: IPermissionsManager
) {

    @Throws(MissingPermissionException::class)
    suspend fun startBubble(turnOn: Boolean) {
        if (turnOn)
            writeChangesDataStore(true)

        if (DatwallKernel.DATA_MOBILE_ON && activationManager.canWork().first) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val permission =
                    permissionsManager.findPermission(IPermissionsManager.DRAW_OVERLAYS_CODE)
                if (permission?.checkPermission?.invoke(permission, context) != true) {

                }
            }

            kotlin.runCatching {
                context.startService(Intent(context, BubbleFloatingService::class.java))
            }
        }
    }


    suspend fun stopBubble(turnOf: Boolean) {
        if (turnOf)
            writeChangesDataStore(false)

        kotlin.runCatching {
            context.stopService(Intent(context, BubbleFloatingService::class.java))
        }
    }


    private suspend fun writeChangesDataStore(
        enabled: Boolean
    ) {
        withContext(Dispatchers.IO) {
            context.dataStore.edit {
                it[PreferencesKeys.ENABLED_BUBBLE_FLOATING] = enabled
            }
        }
    }

}