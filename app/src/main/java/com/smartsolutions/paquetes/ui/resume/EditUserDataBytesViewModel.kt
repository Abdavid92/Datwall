package com.smartsolutions.paquetes.ui.resume

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.repositories.UserDataBytesRepository
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditUserDataBytesViewModel @Inject constructor(
    private val userDataBytesRepository: IUserDataBytesRepository
): ViewModel() {

    fun getUserDataBytes(simId: String): LiveData<List<UserDataBytes>>{
        return userDataBytesRepository.flowBySimId(simId).asLiveData()
    }

    fun updateUserDataBytes(userDataBytes: UserDataBytes){
        viewModelScope.launch {
            userDataBytesRepository.update(userDataBytes)
        }
    }

}