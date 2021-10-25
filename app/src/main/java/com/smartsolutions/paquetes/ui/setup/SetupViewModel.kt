package com.smartsolutions.paquetes.ui.setup

import android.app.Application
import androidx.lifecycle.*
import com.smartsolutions.paquetes.managers.contracts.IConfigurationManager
import com.smartsolutions.paquetes.managers.models.Configuration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    application: Application,
    private val configurationManager: IConfigurationManager,
) : AndroidViewModel(application) {

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
}