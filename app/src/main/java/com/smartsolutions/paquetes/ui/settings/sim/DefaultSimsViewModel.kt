package com.smartsolutions.paquetes.ui.settings.sim

import androidx.lifecycle.*
import com.smartsolutions.paquetes.managers.SimManager
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DefaultSimsViewModel @Inject constructor(
    private val simManager: SimManager
): ViewModel() {


    fun getInstalledSims(): LiveData<List<Sim>>{
        return simManager.flowInstalledSims().asLiveData(Dispatchers.IO)
    }

}