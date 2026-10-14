package com.smartsolutions.paquetes.ui.permissions

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.managers.contracts.IConfigurationManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.settingsDataStore
import com.smartsolutions.paquetes.ui.next
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    application: Application,
    val permissionsManager: IPermissionsManager,
    val configurationManager: IConfigurationManager
) : AndroidViewModel(application) {

    /**
     * Permisos que muestra la pantalla: en el primer uso solo los que se piden al inicio;
     * desde Ajustes, todos.
     * */
    fun permissions(atStartup: Boolean) =
        if (atStartup)
            permissionsManager.permissions.filter { it.atStartup }
        else
            permissionsManager.permissions

    /**
     * Cierra el primer uso (el usuario continuó o eligió "Ahora no") y sigue con el arranque.
     * */
    fun finishFirstUse() {
        viewModelScope.launch {
            getApplication<Application>().settingsDataStore.edit {
                it[PreferencesKeys.DATA_DISCLOSURE_ANSWERED] = true
            }
            next()
        }
    }
}
