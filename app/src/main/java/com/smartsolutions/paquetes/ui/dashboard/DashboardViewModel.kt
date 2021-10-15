package com.smartsolutions.paquetes.ui.dashboard

import android.app.Application
import android.os.Build
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.appcompat.widget.SwitchCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import com.smartsolutions.paquetes.*
import com.smartsolutions.paquetes.exceptions.USSDRequestException
import com.smartsolutions.paquetes.helpers.FirewallHelper
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.services.BubbleFloatingService
import com.smartsolutions.paquetes.ui.permissions.SinglePermissionFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val appRepository: IAppRepository,
    private val permissionManager: IPermissionsManager,
    private val firewallHelper: FirewallHelper,
    private val ussdHelper: USSDHelper
) : AndroidViewModel(application) {

    private val dataStore = getApplication<DatwallApplication>().internalDataStore

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

            firewallHelper.observeFirewallState().collect {
                withContext(Dispatchers.Main) {
                    switch.setOnCheckedChangeListener(null)

                    switch.isChecked = it

                    switch.setOnCheckedChangeListener { buttonView,_ ->
                        onFirewallChangeListener(buttonView, fm)
                    }
                }
            }
        }
    }

    private fun onFirewallChangeListener(
        buttonView: CompoundButton,
        fm: FragmentManager
    ) {
        if (buttonView.isChecked) {

            viewModelScope.launch(Dispatchers.IO) {
                if (firewallHelper.allAccess(appRepository.all())) {

                    withContext(Dispatchers.Main) {
                        buttonView.isChecked = false

                        Toast.makeText(
                            getApplication(),
                            getApplication<DatwallApplication>()
                                .getString(R.string.error_starting_firewall_for_all_access),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {

                    val dynamic = dataStore.data.firstOrNull()
                        ?.get(PreferencesKeys.ENABLED_DYNAMIC_FIREWALL) ?: true

                    if (dynamic) {
                        requestDrawOverPermission(fm,
                            onGranted = {
                                startFirewall(buttonView, fm)
                            },
                            onDenied = {
                                buttonView.isChecked = false
                            }
                        )
                    }else {
                        startFirewall(buttonView, fm)
                    }
                }
            }
        } else {
            viewModelScope.launch {
                firewallHelper.stopFirewall(true)
            }
        }
    }

    private fun startFirewall(buttonView: CompoundButton, fm: FragmentManager){
        if (!firewallHelper.checkFirewallPermission()) {
            SinglePermissionFragment.newInstance(
                IPermissionsManager.VPN_CODE,
                object : SinglePermissionFragment.SinglePermissionCallback {

                    override fun onGranted() {
                        viewModelScope.launch {
                            firewallHelper.startFirewall(true)
                        }
                    }

                    override fun onDenied() {
                        buttonView.isChecked = false
                    }
                }
            ).show(fm, null)

        } else {
            viewModelScope.launch {
                firewallHelper.startFirewall(true)
            }
        }
    }

    fun setFirewallDynamicModeListener(
        dynamicRadio: RadioButton,
        staticRadio: RadioButton,
        fm: FragmentManager
    ) {
        viewModelScope.launch {

            val dynamic = getApplication<DatwallApplication>().settingsDataStore.data
                .firstOrNull()?.get(PreferencesKeys.ENABLED_DYNAMIC_FIREWALL) ?: true

            if (dynamic)
                dynamicRadio.isChecked = true
            else
                staticRadio.isChecked = true

            dynamicRadio.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    requestDrawOverPermission(fm,
                        onGranted = {
                            writeChangesDataStore(PreferencesKeys.ENABLED_DYNAMIC_FIREWALL, true)
                        },
                        onDenied = {
                            writeChangesDataStore(PreferencesKeys.ENABLED_DYNAMIC_FIREWALL, false)
                            staticRadio.isChecked
                        })
                } else {
                    writeChangesDataStore(PreferencesKeys.ENABLED_DYNAMIC_FIREWALL, false)
                }
            }
        }
    }

    fun setBubbleSwitchListener(bubble: SwitchCompat, childFragmentManager: FragmentManager) {
        viewModelScope.launch(Dispatchers.IO) {

            val dataStore = getApplication<DatwallApplication>().settingsDataStore

            dataStore.data.collect {

                withContext(Dispatchers.Main) {
                    bubble.setOnCheckedChangeListener(null)

                    bubble.isChecked = it[PreferencesKeys.ENABLED_BUBBLE_FLOATING] == true

                    bubble.setOnCheckedChangeListener { buttonView, isChecked ->
                        onBubbleChangeListener(
                            buttonView,
                            isChecked,
                            childFragmentManager
                        )
                    }
                }
            }
        }
    }

    private fun onBubbleChangeListener(
        buttonView: CompoundButton,
        isChecked: Boolean,
        fm: FragmentManager
    ) {
        if (isChecked) {
           requestDrawOverPermission(fm,
           onGranted = {
               writeChangesDataStore(PreferencesKeys.ENABLED_BUBBLE_FLOATING, true)
           },
           onDenied = {
               buttonView.isChecked = false
           })
        } else {
            writeChangesDataStore(PreferencesKeys.ENABLED_BUBBLE_FLOATING, false)
        }
    }

    fun setTransparencyListener(bubbleTransparency: AppCompatSeekBar, bubbleExample: View) {
        viewModelScope.launch {
            val dataStore = getApplication<DatwallApplication>().settingsDataStore

            val transparency = dataStore.data
                .firstOrNull()?.get(PreferencesKeys.BUBBLE_TRANSPARENCY)
                ?: BubbleFloatingService.TRANSPARENCY

            bubbleTransparency.max = 10

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                bubbleTransparency.setProgress((transparency * 10).toInt(), true)
            else
                bubbleTransparency.progress = (transparency * 10).toInt()

            bubbleExample.alpha = transparency

            bubbleTransparency.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val newTransparency = progress.toFloat() / 10f
                    bubbleExample.alpha = newTransparency
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    val newTransparency = seekBar.progress.toFloat() / 10f

                    viewModelScope.launch {
                        dataStore.edit {
                            it[PreferencesKeys.BUBBLE_TRANSPARENCY] = newTransparency
                        }
                    }
                }

            })
        }
    }

    fun setSizeListener(bubbleSize: AppCompatSeekBar, bubbleExample: View) {
        viewModelScope.launch {
            val dataStore = getApplication<DatwallApplication>().settingsDataStore

            val size = BubbleFloatingService.BubbleSize.valueOf(
                dataStore.data.firstOrNull()?.get(PreferencesKeys.BUBBLE_SIZE)
                    ?: BubbleFloatingService.SIZE.name
            )

            BubbleFloatingService.setSizeBubble(
                getApplication(),
                bubbleExample,
                size
            )

            bubbleSize.max = 2

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                bubbleSize.setProgress(size.ordinal, true)
            else
                bubbleSize.progress = size.ordinal

            bubbleSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val newSize = BubbleFloatingService.BubbleSize.values()[progress]

                    BubbleFloatingService.setSizeBubble(
                        getApplication(),
                        bubbleExample,
                        newSize
                    )
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    val newSize = BubbleFloatingService.BubbleSize.values()[seekBar.progress]

                    viewModelScope.launch {
                        dataStore.edit {
                            it[PreferencesKeys.BUBBLE_SIZE] = newSize.name
                        }
                    }
                }

            })
        }
    }

    fun setBubbleAllWayListener(allWay: AppCompatRadioButton, onlyConsume: AppCompatRadioButton) {
        viewModelScope.launch {
            val dataStore = getApplication<DatwallApplication>().settingsDataStore

            val allWayStore = dataStore.data
                .firstOrNull()?.get(PreferencesKeys.BUBBLE_ALWAYS_SHOW)
                ?: BubbleFloatingService.ALWAYS_SHOW

            if (allWayStore)
                allWay.isChecked = true
            else
                onlyConsume.isChecked = true

            allWay.setOnCheckedChangeListener { _, isChecked ->
                viewModelScope.launch {
                    dataStore.edit {
                        it[PreferencesKeys.BUBBLE_ALWAYS_SHOW] = isChecked
                    }
                }
            }
        }
    }


    private fun writeChangesDataStore(preferences: Preferences.Key<Boolean>, value: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            getApplication<Application>().settingsDataStore.edit {
                it[preferences] = value
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun findDrawOverPermission() = permissionManager
        .findPermission(IPermissionsManager.DRAW_OVERLAYS_CODE)
        ?: throw NullPointerException("Incorrect permission code")

    private fun requestDrawOverPermission(
        fm: FragmentManager,
        onGranted: () -> Unit,
        onDenied: (() -> Unit)? = null
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permission = findDrawOverPermission()

            if (permission.checkPermission(permission, getApplication())) {
                onGranted()
            } else {
                SinglePermissionFragment.newInstance(
                    IPermissionsManager.DRAW_OVERLAYS_CODE,
                    object : SinglePermissionFragment.SinglePermissionCallback {
                        override fun onGranted() {
                            onGranted()
                        }

                        override fun onDenied() {
                            onDenied?.invoke()
                        }
                    }
                ).show(fm, null)

                return false
            }
        } else
            onGranted()

        return true
    }
}