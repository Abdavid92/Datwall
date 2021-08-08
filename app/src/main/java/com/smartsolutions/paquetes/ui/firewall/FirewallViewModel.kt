package com.smartsolutions.paquetes.ui.firewall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.managers.IconManager
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
    private val appRepository: IAppRepository,
    private val iconManager: IconManager
) : ViewModel() {

    private val appsToUpdate = mutableListOf<IApp>()

    fun getApps() = appRepository.flowByGroup()
        .asLiveData(Dispatchers.IO)

    fun updateApp(app: IApp) {
        if (!appsToUpdate.contains(app)) {
            appsToUpdate.add(app)
        } else {
            val index = appsToUpdate.indexOf(app)

            if (index != -1) {
                appsToUpdate[index] = app
            }
        }
    }

    fun confirmUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.update(appsToUpdate)
        }
    }
}