package com.smartsolutions.paquetes.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.sims.SimType
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class PackagesConfigurationViewModel @Inject constructor(
    private val simManager: ISimManager,
    private val dataPackageManager: IDataPackageManager
) : ViewModel() {


    fun geInstalledSims(): LiveData<List<Sim>> {
        return simManager.flowInstalledSims().asLiveData()
    }

    fun configureManualPackages(@Networks network: String, sim: Sim) {
        viewModelScope.launch {
            dataPackageManager.setDataPackagesManualConfiguration(network, sim)
        }
    }

    fun isDefaultSim(sim: Sim, type: SimType): Boolean? {
        return runBlocking {
            return@runBlocking simManager.isSimDefaultBoth(type, sim)
        }
    }

}
