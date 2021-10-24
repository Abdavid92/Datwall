package com.smartsolutions.paquetes.ui.dashboard

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.appcompat.widget.SwitchCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import com.smartsolutions.paquetes.*
import com.smartsolutions.paquetes.exceptions.USSDRequestException
import com.smartsolutions.paquetes.helpers.BubbleServiceHelper
import com.smartsolutions.paquetes.helpers.FirewallHelper
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.kernel.DatwallApplication
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.services.BubbleFloatingService
import com.smartsolutions.paquetes.ui.permissions.SinglePermissionFragment
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
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
    private val bubbleServiceHelper: BubbleServiceHelper,
    private val ussdHelper: USSDHelper
) : AndroidViewModel(application) {

    /**
     * DataStore interno.
     * */
    private val dataStore = getApplication<DatwallApplication>().internalDataStore

    private val _appsData = MutableLiveData<IntArray>()

    @SuppressLint("StaticFieldLeak")
    private var bubbleView: View? = null

    /**
     * LiveData que contiene un Array de tres items.
     * El primero es el total de aplicaciones permitidas,
     * el segundo es el total de aplicaciones bloqueadas,
     * y el tercero es el total de aplicaciones guardadas
     * en la base de datos.
     * */
    val appsData: LiveData<IntArray>
        get() {
            viewModelScope.launch {
                val apps = withContext(Dispatchers.IO){
                    appRepository.all()
                }

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

                        withContext(Dispatchers.Main) {
                            SinglePermissionFragment.newInstance(
                                IPermissionsManager.CALL_CODE
                            ).show(fm, null)
                        }
                    }
                }
            }
        }
    }

    fun setFirewallSwitchListener(switch: SwitchCompat, fm: FragmentManager) {
        viewModelScope.launch {

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

            viewModelScope.launch {
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

                    startFirewall(buttonView, fm)
                }
            }
        } else {
            viewModelScope.launch {
                firewallHelper.stopFirewall(true)
            }
        }
    }

    private fun startFirewall(buttonView: CompoundButton, fm: FragmentManager) {
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
        staticRadio: RadioButton
    ) {
        viewModelScope.launch {

            val dynamic = withContext(Dispatchers.IO){
                dataStore.data
                    .firstOrNull()?.get(PreferencesKeys.ENABLED_DYNAMIC_FIREWALL)
            } ?: true

            if (dynamic)
                dynamicRadio.isChecked = true
            else
                staticRadio.isChecked = true

            dynamicRadio.setOnCheckedChangeListener { _, isChecked ->
                writeChangesDataStore(PreferencesKeys.ENABLED_DYNAMIC_FIREWALL, isChecked)
            }
        }
    }

    fun setBubbleSwitchListener(bubble: SwitchCompat, childFragmentManager: FragmentManager) {
        viewModelScope.launch {

            bubbleServiceHelper.observeBubbleChanges().collect {

                withContext(Dispatchers.Main) {
                    bubble.setOnCheckedChangeListener(null)

                    bubble.isChecked = it

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
               viewModelScope.launch {
                   bubbleServiceHelper.startBubble(true)
               }
           },
           onDenied = {
               buttonView.isChecked = false
           })
        } else {
            viewModelScope.launch {
                bubbleServiceHelper.stopBubble(true)
            }
        }
    }

    fun setTransparencyListener(bubbleTransparency: IndicatorSeekBar) {
        viewModelScope.launch {
            val dataStore = getApplication<DatwallApplication>().uiDataStore

            val transparency = dataStore.data.firstOrNull()
                ?.get(PreferencesKeys.BUBBLE_TRANSPARENCY) ?: BubbleFloatingService.TRANSPARENCY

            bubbleTransparency.setProgress((transparency * 10))

            initBubbleView(bubbleTransparency.context)

            bubbleTransparency.indicator.contentView = bubbleView

            bubbleTransparency.onSeekChangeListener = object : OnSeekChangeListener {
                override fun onSeeking(seekParams: SeekParams) {

                    if (seekParams.fromUser) {
                        val newTransparency = seekParams.progress.toFloat() / 10f

                        bubbleView?.findViewById<LinearLayout>(R.id.lin_background_bubble)
                            ?.alpha = newTransparency
                    }
                }

                override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) { }

                override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {
                    val newTransparency = seekBar.progress.toFloat() / 10f

                    viewModelScope.launch {
                        dataStore.edit {
                            it[PreferencesKeys.BUBBLE_TRANSPARENCY] = newTransparency
                        }
                    }
                }
            }
        }
    }

    fun setSizeListener(bubbleSize: IndicatorSeekBar) {
        viewModelScope.launch {
            val dataStore = getApplication<DatwallApplication>().settingsDataStore

            val size = BubbleFloatingService.BubbleSize.valueOf(
                dataStore.data.firstOrNull()
                    ?.get(PreferencesKeys.BUBBLE_SIZE) ?: BubbleFloatingService.SIZE.name
            )

            initBubbleView(bubbleSize.context)

            BubbleFloatingService.setSizeBubble(
                getApplication(),
                bubbleView!!,
                size
            )

            bubbleSize.indicator.contentView = bubbleView

            bubbleSize.setProgress(size.ordinal.toFloat())

            bubbleSize.onSeekChangeListener = object : OnSeekChangeListener {

                override fun onSeeking(seekParams: SeekParams) {
                    val newSize = BubbleFloatingService.BubbleSize.values()[seekParams.progress]

                    BubbleFloatingService.setSizeBubble(
                        getApplication(),
                        bubbleView!!,
                        newSize
                    )
                }

                override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) { }

                override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {
                    val newSize = BubbleFloatingService.BubbleSize.values()[seekBar.progress]

                    viewModelScope.launch(Dispatchers.IO) {
                        dataStore.edit {
                            it[PreferencesKeys.BUBBLE_SIZE] = newSize.name
                        }
                    }
                }
            }
        }
    }

    fun setBubbleAllWayListener(allWay: AppCompatRadioButton, onlyConsume: AppCompatRadioButton) {
        viewModelScope.launch {
            val dataStore = getApplication<DatwallApplication>().settingsDataStore

            val allWayStore = withContext(Dispatchers.IO){
                dataStore.data
                    .firstOrNull()?.get(PreferencesKeys.BUBBLE_ALWAYS_SHOW)
            } ?: BubbleFloatingService.ALWAYS_SHOW

            if (allWayStore)
                allWay.isChecked = true
            else
                onlyConsume.isChecked = true

            allWay.setOnCheckedChangeListener { _, isChecked ->
                viewModelScope.launch(Dispatchers.IO) {
                    dataStore.edit {
                        it[PreferencesKeys.BUBBLE_ALWAYS_SHOW] = isChecked
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        bubbleView = null
    }

    private fun initBubbleView(context: Context) {
        if (bubbleView == null) {
            bubbleView = LayoutInflater.from(context)
                .inflate(
                    R.layout.bubble_floating_layout,
                    null,
                    false).apply {
                        findViewById<LinearLayout>(R.id.lin_background_bubble)
                            .visibility = View.VISIBLE
                }
        }
    }

    /**
     * Guarda los cambios en el dataStore interno.
     * */
    private fun writeChangesDataStore(preferences: Preferences.Key<Boolean>, value: Boolean) {
        viewModelScope.launch {
            dataStore.edit {
                it[preferences] = value
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun findDrawOverPermission() = permissionManager
        .findPermission(IPermissionsManager.DRAW_OVERLAYS_CODE)
        ?: throw NullPointerException("Incorrect permission code")

    /**
     * Pide el permiso de sobreposición de pantalla en caso de estar en android 6 en adelante
     * */
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