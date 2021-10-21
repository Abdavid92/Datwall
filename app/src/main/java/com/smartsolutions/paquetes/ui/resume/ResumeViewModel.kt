package com.smartsolutions.paquetes.ui.resume

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.*
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.settingsDataStore
import com.smartsolutions.paquetes.exceptions.USSDRequestException
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.contracts.ISynchronizationManager
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import com.smartsolutions.paquetes.uiDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ResumeViewModel @Inject constructor(
    application: Application,
    private val simManager: ISimManager,
    private val userDataBytesRepository: IUserDataBytesRepository,
    private val synchronizationManager: ISynchronizationManager
) : AndroidViewModel(application) {

    private var filter = FilterUserDataBytes.NORMAL
    private var liveUserDataBytes = MutableLiveData<List<UserDataBytes>>()
    private var userDataBytes = emptyList<UserDataBytes>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            getApplication<Application>().uiDataStore.data.collect {
                filter = FilterUserDataBytes.valueOf(
                    it[PreferencesKeys.RESUME_FILTER] ?: FilterUserDataBytes.NORMAL.name
                )
                withContext(Dispatchers.Default) {
                    if (userDataBytes.isNotEmpty()) {
                        liveUserDataBytes.postValue(filter(userDataBytes))
                    }
                }
            }
        }
    }


    fun setFilter(filterUserDataBytes: FilterUserDataBytes) {
        viewModelScope.launch(Dispatchers.IO) {
            getApplication<Application>().uiDataStore.edit {
                it[PreferencesKeys.RESUME_FILTER] = filterUserDataBytes.name
            }
        }
    }


    fun getInstalledSims(): LiveData<List<Sim>> {
        return simManager.flowInstalledSims().asLiveData(Dispatchers.IO)
    }

    fun getUserDataBytes(simId: String): LiveData<List<UserDataBytes>> {
        viewModelScope.launch(Dispatchers.IO) {
            userDataBytesRepository.flowBySimId(simId).collect { userData ->
                userDataBytes = userData
                withContext(Dispatchers.Default) {
                    liveUserDataBytes.postValue(filter(userData))
                }
            }
        }
        return liveUserDataBytes
    }


    fun synchronizeUserDataBytes(callback: SynchronizationResult) {
        viewModelScope.launch {
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
                } else {
                    withContext(Dispatchers.Main) {
                        callback.onFailed(e.cause)
                    }
                }
            }
        }
    }

    fun setDefaultSim(type: SimDelegate.SimType, sim: Sim) {
        viewModelScope.launch {
            simManager.setDefaultSim(type, sim)
        }
    }


    private fun filter(userData: List<UserDataBytes>): List<UserDataBytes> {
        return when (filter){
           FilterUserDataBytes.SIZE_ASC -> {
                userData.sortedBy { it.bytes }
            }
            FilterUserDataBytes.SIZE_DESC -> {
                userData.sortedByDescending { it.bytes }
            }
            FilterUserDataBytes.EXPIRE_ASC -> {
                userData.sortedBy { it.expiredTime }
            }
            FilterUserDataBytes.EXPIRE_DESC -> {
                userData.sortedByDescending { it.expiredTime }
            }
            else -> userData
        }.filter { it.exists() && !it.isExpired() }
    }


    interface SynchronizationResult {
        fun onSuccess()
        fun onCallPermissionsDenied()
        fun onUSSDFail(message: String)
        fun onFailed(throwable: Throwable?)
        fun onAccessibilityServiceDisabled()
    }

    enum class FilterUserDataBytes {
        SIZE_DESC,
        SIZE_ASC,
        EXPIRE_ASC,
        EXPIRE_DESC,
        NORMAL
    }

}