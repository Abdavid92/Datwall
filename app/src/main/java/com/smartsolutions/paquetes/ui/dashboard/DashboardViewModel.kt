package com.smartsolutions.paquetes.ui.dashboard

import androidx.lifecycle.*
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val appRepository: IAppRepository
): ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is dashboard Fragment"
    }
    val text: LiveData<String> = _text

    val apps = appRepository.flow().asLiveData(viewModelScope.coroutineContext)

    fun deleteOne() {
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.delete(appRepository.all[0])
        }
    }
}