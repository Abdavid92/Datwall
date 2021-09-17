package com.smartsolutions.paquetes.ui.resume

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private val liveDataSims = MutableLiveData<List<Sim>>()
    private val liveDataUserData = MutableLiveData<List<UserDataBytes>>()

    fun getInstalledSims(): LiveData<List<Sim>> {
        viewModelScope.launch(Dispatchers.IO) {
            simManager.flowInstalledSims().collect {
                liveDataSims.postValue(it)
            }
        }
        return liveDataSims
    }


    fun getUserDataBytes(simId: String): LiveData<List<UserDataBytes>> {
        viewModelScope.launch(Dispatchers.IO) {
            userDataBytesRepository.flowBySimId(simId).collect {
                liveDataUserData.postValue(it.filter { it.exists() && !it.isExpired() })
            }
        }
        return liveDataUserData
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


    interface SynchronizationResult {
        fun onSuccess()
        fun onCallPermissionsDenied()
        fun onUSSDFail(message: String)
        fun onFailed(throwable: Throwable?)
        fun onAccessibilityServiceDisabled()
    }

}