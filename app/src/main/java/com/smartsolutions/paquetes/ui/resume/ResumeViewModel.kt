package com.smartsolutions.paquetes.ui.resume

import androidx.lifecycle.*
import com.smartsolutions.paquetes.exceptions.USSDRequestException
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.contracts.ISynchronizationManager
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class ResumeViewModel @Inject constructor(
    private val simManager: ISimManager,
    private val userDataBytesRepository: IUserDataBytesRepository,
    private val synchronizationManager: ISynchronizationManager
) : ViewModel() {

    fun getInstalledSims(): LiveData<List<Sim>> {
        return simManager.flowInstalledSims().asLiveData(Dispatchers.IO)
    }

    fun getUserDataBytes(simId: String): LiveData<List<UserDataBytes>> {
        return userDataBytesRepository.flowBySimId(simId).map {
            return@map it.filter { it.exists() && !it.isExpired() }
        }.asLiveData(Dispatchers.IO)
    }


    fun synchronizeUserDataBytes(callback: SynchronizationResult) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                synchronizationManager.synchronizeUserDataBytes(simManager.getDefaultSim(SimDelegate.SimType.VOICE))
                withContext(Dispatchers.Main) {
                    callback.onSuccess()
                }
            } catch (e: Exception) {
                if (e is USSDRequestException) {
                    when (e.errorCode) {
                        USSDHelper.ACCESSIBILITY_SERVICE_UNAVAILABLE -> {
                            withContext(Dispatchers.Main) {
                                callback.onAccessibilityServiceDisabled()
                            }
                        }
                        USSDHelper.DENIED_CALL_PERMISSION -> {
                            withContext(Dispatchers.Main) {
                                callback.onCallPermissionsDenied()
                            }
                        }
                        else -> {
                            withContext(Dispatchers.Main) {
                                callback.onUSSDFail(
                                    (e.message ?: e.errorCode).toString()
                                )
                            }
                        }
                    }
                }else {
                    withContext(Dispatchers.Main) {
                        callback.onFailed(e.cause)
                    }
                }
            }
        }
    }


    fun setDefaultSim(type: SimDelegate.SimType, sim: Sim){
        viewModelScope.launch(Dispatchers.IO) {
            simManager.setDefaultSim(type, sim)
        }
    }

    interface SynchronizationResult {
        fun onSuccess()
        fun onCallPermissionsDenied()
        fun onUSSDFail(message: String)
        fun onFailed(throwable: Throwable?)
        fun onAccessibilityServiceDisabled()
    }

}