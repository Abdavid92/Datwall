package com.smartsolutions.paquetes.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.managers.contracts.IIconManager2
import com.smartsolutions.paquetes.repositories.contracts.IPurchasedPackageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val purchasedPackageRepository: IPurchasedPackageRepository,
    private val iconManager: IIconManager2,
    private val activationManager: IActivationManager
) : ViewModel() {

    fun clearHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO){
                purchasedPackageRepository.getAll()
            }.firstOrNull()
                ?.filter { !it.pending }
                ?.let { packages ->
                    withContext(Dispatchers.IO){
                        purchasedPackageRepository.delete(packages)
                    }
                }
        }
    }

    fun clearIconCache() {
        viewModelScope.launch {
            iconManager.deleteAll()
        }
    }

    fun getIdentifierDevice(callback: (identifier: String?) -> Unit) {
        viewModelScope.launch {
            callback(activationManager.getLocalLicense()?.deviceId)
        }
    }
}