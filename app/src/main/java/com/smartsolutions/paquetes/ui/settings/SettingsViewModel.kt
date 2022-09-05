package com.smartsolutions.paquetes.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.repositories.contracts.IPurchasedPackageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val purchasedPackageRepository: IPurchasedPackageRepository,
    private val iconManager: IIconManager
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
}