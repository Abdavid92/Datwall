package com.smartsolutions.paquetes.ui.settings.sim

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.managers.contracts.ISimManager2
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.serverApis.models.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SimsDefaultDialogViewModel @Inject constructor(
    private val simManager: ISimManager2
) : ViewModel() {

    var onDefault: ((sim: Sim) -> Unit)? = null
    var sim: Sim? = null

    private var liveData = MutableLiveData<Result<Sim>>()


    fun getDefaultSim(type: SimDelegate.SimType): LiveData<Result<Sim>>{
        viewModelScope.launch {
            liveData.postValue(simManager.getDefaultSimSystem(type))
        }
        return liveData
    }



}