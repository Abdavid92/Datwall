package com.smartsolutions.paquetes.ui.firewall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppGroup
import com.smartsolutions.paquetes.repositories.models.IApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FirewallViewModel @Inject constructor(
    private val appRepository: IAppRepository
) : ViewModel() {

    fun getApps() = appRepository.flowByGroup()
        .asLiveData(viewModelScope.coroutineContext)

    fun updateApp(app: IApp) {
        viewModelScope.launch(Dispatchers.IO) {
            if (app is App)
                appRepository.update(app)
            else if (app is AppGroup)
                appRepository.update(app)
        }
    }
}