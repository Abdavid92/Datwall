package com.smartsolutions.paquetes.ui.dashboard

import android.app.Application
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SwitchCompat
import androidx.datastore.preferences.core.edit
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import com.smartsolutions.paquetes.DatwallApplication
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.exceptions.USSDRequestException
import com.smartsolutions.paquetes.helpers.FirewallHelper
import com.smartsolutions.paquetes.helpers.NetworkUsageUtils
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.managers.NetworkUsageManager
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.contracts.IUserDataBytesManager
import com.smartsolutions.paquetes.managers.models.Permission
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import com.smartsolutions.paquetes.ui.permissions.SinglePermissionFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val appRepository: IAppRepository,
    private val networkUsageManager: NetworkUsageManager,
    private val permissionManager: IPermissionsManager,
    private val iconManager: IIconManager,
    private val firewallHelper: FirewallHelper,
    private val networkUsageUtils: NetworkUsageUtils,
    private val ussdHelper: USSDHelper
): AndroidViewModel(application) {

    private val _appsData = MutableLiveData<IntArray>()
    /**
     * LiveData que contiene un Array de tres items.
     * El primero es el total de aplicaciones permitidas,
     * el segundo es el total de aplicaciones bloqueadas,
     * y el tercero es el total de aplicaciones guardadas
     * en la base de datos.
     * */
    val appsData: LiveData<IntArray>
        get() {
            viewModelScope.launch(Dispatchers.IO) {
                val apps = appRepository.all()

                val allowedAppsCount = apps.count { it.access }
                val blockedAppsCount = apps.count { !it.access }

                _appsData.postValue(intArrayOf(allowedAppsCount, blockedAppsCount, apps.size))
            }

            return _appsData
        }

    private val _appMoreConsume = MutableLiveData<App?>()
    /**
     * Contiene la aplicación que más ha consumido desde que se
     * obtuvo el primer userDataByte o null.
     * */
    val appMoreConsume: LiveData<App?>
        get() {
            viewModelScope.launch(Dispatchers.IO) {

                val interval = networkUsageUtils.getTimePeriod(NetworkUsageUtils.PERIOD_PACKAGE)

                val apps = appRepository.all()

                networkUsageManager.fillAppsUsage(
                    apps,
                    interval.first,
                    interval.second
                )

                var finalApp: App? = null

                apps.forEach { app ->
                    val finalTraffic = finalApp?.traffic?.totalBytes?.bytes

                    app.traffic?.totalBytes?.bytes?.let { bytes ->
                        if (finalTraffic == null || finalTraffic < bytes) {
                            finalApp = app
                        }
                    }
                }

                if (finalApp?.traffic?.totalBytes?.bytes == 0L)
                    _appMoreConsume.postValue(null)
                else
                    _appMoreConsume.postValue(finalApp)
            }

            return _appMoreConsume
        }

    /**
     * Establece el ícono de la app en el imageView de manera asincrónica y
     * le da visibilidad al imageView si está invisible.
     *
     * @param app
     * @param imageView
     * */
    fun setAppIcon(app: App, imageView: ImageView) {
        iconManager.getAsync(
            packageName = app.packageName,
            versionCode = app.version,
            callback = {
                imageView.setImageBitmap(it)

                if (imageView.visibility != View.VISIBLE)
                    imageView.visibility = View.VISIBLE
            }
        )
    }

    fun launchUssdCode(ussd: String, fm: FragmentManager) {
        viewModelScope.launch {
            try {
                ussdHelper.sendUSSDRequestLegacy(ussd, false)
            } catch (e: USSDRequestException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    if (e.errorCode == USSDHelper.DENIED_CALL_PERMISSION) {

                        SinglePermissionFragment.newInstance(
                            IPermissionsManager.CALL_CODE
                        ).show(fm, null)
                    }
                }
            }
        }
    }

    fun setFirewallSwitchListener(switch: SwitchCompat, fm: FragmentManager) {
        viewModelScope.launch(Dispatchers.IO) {

            val firewallEnabled = firewallHelper.firewallEnabled()

            withContext(Dispatchers.Main) {
                switch.isChecked = firewallEnabled
            }

            switch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {

                    viewModelScope.launch(Dispatchers.IO) {
                        if (firewallHelper.allAccess(appRepository.all())) {

                            withContext(Dispatchers.Main) {
                                buttonView.isChecked = false

                                Toast.makeText(
                                    getApplication(),
                                    getApplication<DatwallApplication>()
                                        .getString(R.string.error_starting_firewall_for_all_access),
                                    Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (firewallHelper.startFirewall() != null) {
                                val fragment = SinglePermissionFragment.newInstance(
                                    IPermissionsManager.VPN_CODE,
                                    object : SinglePermissionFragment.SinglePermissionCallback {

                                        override fun onGranted() {
                                            firewallHelper.startFirewall()
                                        }

                                        override fun onDenied() {
                                            buttonView.isChecked = false

                                            Toast.makeText(
                                                getApplication(),
                                                getApplication<DatwallApplication>()
                                                    .getString(R.string.stoped_missing_vpn_permissions_title_notification),
                                                Toast.LENGTH_SHORT)
                                                .show()
                                        }
                                    }
                                )

                                withContext(Dispatchers.Main) {
                                    fragment.show(fm, null)
                                }
                            }
                        }
                    }
                } else {
                    firewallHelper.stopFirewall()
                }
            }
        }
    }

    fun setFirewallDynamicModeListener(dynamicRadio: RadioButton, staticRadio: RadioButton) {
        viewModelScope.launch {
            val dataStore = getApplication<DatwallApplication>().dataStore

            val defaultDynamic = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

            val dynamic = dataStore.data
                .firstOrNull()?.get(PreferencesKeys.ENABLED_DYNAMIC_FIREWALL) ?: defaultDynamic

            if (dynamic)
                dynamicRadio.isChecked = true
            else
                staticRadio.isChecked = true

            dynamicRadio.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val permission = findDrawOverPermission()

                        if (permission.checkPermission(permission, getApplication())) {
                            viewModelScope.launch {
                                dataStore.edit {
                                    it[PreferencesKeys.ENABLED_DYNAMIC_FIREWALL] = true
                                }
                            }
                        } else {
                            SinglePermissionFragment.newInstance(
                                IPermissionsManager.DRAW_OVERLAYS_CODE,
                                object : SinglePermissionFragment.SinglePermissionCallback {
                                    override fun onGranted() {
                                        viewModelScope.launch {
                                            dataStore.edit {
                                                it[PreferencesKeys.ENABLED_DYNAMIC_FIREWALL] = true
                                            }
                                        }
                                    }

                                    override fun onDenied() {
                                        buttonView.isChecked = false
                                    }

                                }
                            )
                        }
                    } else {
                        viewModelScope.launch {
                            dataStore.edit {
                                it[PreferencesKeys.ENABLED_DYNAMIC_FIREWALL] = true
                            }
                        }
                    }
                } else {
                    viewModelScope.launch {
                        dataStore.edit {
                            it[PreferencesKeys.ENABLED_DYNAMIC_FIREWALL] = false
                        }
                    }
                }
            }
        }
    }

    fun setBubbleSwitchListener(bubble: SwitchCompat, childFragmentManager: FragmentManager) {
        viewModelScope.launch(Dispatchers.IO) {
            val dataStore = getApplication<DatwallApplication>().dataStore

            val bubbleEnabled = dataStore.data
                .firstOrNull()?.get(PreferencesKeys.ENABLED_BUBBLE_FLOATING) == true

            withContext(Dispatchers.Main) {
                bubble.isChecked = bubbleEnabled
            }

            bubble.setOnCheckedChangeListener { buttonView, isChecked ->

                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val permission = findDrawOverPermission()

                        if (permission.checkPermission(permission, getApplication())) {

                            viewModelScope.launch {
                                dataStore.edit {
                                    it[PreferencesKeys.ENABLED_BUBBLE_FLOATING] = true
                                }
                            }
                        } else {
                            SinglePermissionFragment.newInstance(
                                IPermissionsManager.DRAW_OVERLAYS_CODE,
                                object : SinglePermissionFragment.SinglePermissionCallback {
                                    override fun onGranted() {
                                        viewModelScope.launch {
                                            dataStore.edit {
                                                it[PreferencesKeys.ENABLED_BUBBLE_FLOATING] = true
                                            }
                                        }
                                    }

                                    override fun onDenied() {
                                        buttonView.isChecked = false
                                    }
                                }
                            ).show(childFragmentManager, null)
                        }
                    } else {
                        viewModelScope.launch {
                            dataStore.edit {
                                it[PreferencesKeys.ENABLED_BUBBLE_FLOATING] = true
                            }
                        }
                    }
                } else {
                    viewModelScope.launch {
                        dataStore.edit {
                            it[PreferencesKeys.ENABLED_BUBBLE_FLOATING] = false
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun findDrawOverPermission() = permissionManager
        .findPermission(IPermissionsManager.DRAW_OVERLAYS_CODE) ?:
        throw NullPointerException("Incorrect permission code")
}