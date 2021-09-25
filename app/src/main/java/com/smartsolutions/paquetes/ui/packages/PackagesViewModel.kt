package com.smartsolutions.paquetes.ui.packages

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.managers.DataPackageManager
import com.smartsolutions.paquetes.managers.SimManager
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.repositories.SimRepository
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class PackagesViewModel @Inject constructor(
    private val simManager: ISimManager,
    private val dataPackageManager: IDataPackageManager
) : ViewModel() {

    fun getPackagesAndSims(): LiveData<Pair<List<Sim>, List<DataPackage>>> {
        return simManager.flowInstalledSims(true).map {
            return@map Pair(it, it.firstOrNull { it.defaultVoice }?.packages ?: emptyList())
        }.asLiveData(Dispatchers.IO)
    }

}