package com.smartsolutions.paquetes.ui.resume

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class ResumeViewModel @Inject constructor(
    private val simManager: ISimManager
) : ViewModel(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    private val liveDataSims = MutableLiveData<List<Sim>>()

    init {
        launch {
            simManager.flowInstalledSims().collect {
                liveDataSims.postValue(it)
            }
        }
    }

    fun getInstalledSims(): LiveData<List<Sim>> {
        return liveDataSims
    }



}