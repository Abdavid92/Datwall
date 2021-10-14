package com.smartsolutions.paquetes.ui.setup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.managers.contracts.IConfigurationManager
import com.smartsolutions.paquetes.managers.models.Configuration
import com.smartsolutions.paquetes.ui.NextViewModelDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val configurationManager: IConfigurationManager,
    kernel: DatwallKernel
) : ViewModel() {

    private val nextViewModelDelegate = NextViewModelDelegate(kernel, viewModelScope)

    private var index = 0

    private val _configurations = MutableLiveData<Array<Configuration>>()
    val configurations: LiveData<Array<Configuration>>
        get() {
            if (_configurations.value == null) {
                viewModelScope.launch(Dispatchers.Default) {
                    _configurations.postValue(configurationManager.getUncompletedConfigurations())
                }
            }
            return _configurations
        }

    fun nextConfiguration(): Configuration? {
        if (hasNextConfiguration())
            return _configurations.value?.get(index++)
        return null
    }

    fun hasNextConfiguration(): Boolean {
        return if (_configurations.value != null)
                _configurations.value!!.size > index
            else
                false
    }

    fun nextActivity() = nextViewModelDelegate.nextActivity()

    fun next() = nextViewModelDelegate.next()
}