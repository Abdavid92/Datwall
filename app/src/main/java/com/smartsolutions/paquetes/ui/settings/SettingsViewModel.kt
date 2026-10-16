package com.smartsolutions.paquetes.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.managers.contracts.IPurchasedPackagesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val purchasedPackagesManager: IPurchasedPackagesManager,
    private val iconManager: IIconManager
) : ViewModel() {

    fun clearHistory() {
        viewModelScope.launch {
            purchasedPackagesManager.clearHistory()
        }
    }

    fun clearIconCache() {
        viewModelScope.launch {
            iconManager.deleteAll()
        }
    }
}
