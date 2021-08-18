package com.smartsolutions.paquetes.ui.applications

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.*
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppGroup
import com.smartsolutions.paquetes.repositories.models.IApp
import com.smartsolutions.paquetes.repositories.models.TrafficType
import com.smartsolutions.paquetes.ui.firewall.AppsListAdapter
import com.smartsolutions.paquetes.uiDataStore
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ApplicationsViewModel @Inject constructor(
    application: Application,
    private val appRepository: IAppRepository,
    val iconManager: IIconManager
) : AndroidViewModel(application) {

    /**
     * Lista de aplicaciones pendientes a actualizar.
     * */
    private val appsToUpdate = mutableListOf<IApp>()

    fun addAppToUpdate(app: IApp) {
        val index = appsToUpdate.indexOf(app)

        if (index != -1) {
            appsToUpdate[index] = app
        } else {
            appsToUpdate.add(app)
        }
    }

    /**
     * Confirma la actualización de las aplicaciones pendientes.
     * */
    fun commitUpdateApps(): Boolean {
        if (appsToUpdate.isNotEmpty()) {
            GlobalScope.launch(Dispatchers.IO) {
                appRepository.update(appsToUpdate)
                appsToUpdate.clear()
            }
            return true
        }
        return false
    }

    fun setFilter(filter: AppsFilter) {
        viewModelScope.launch {
            getApplication<Application>()
                .uiDataStore.edit {
                    it[PreferencesKeys.APPS_FILTER] = filter.name
                }
        }
    }

    fun getApps(key: String): LiveData<Pair<AppsFilter, List<IApp>>> {
        return appRepository.flowByGroup()
            .combine(getApplication<Application>().uiDataStore.data) { apps, preferences ->

                delay(500)

                val filter = AppsFilter.valueOf(
                    preferences[PreferencesKeys.APPS_FILTER] ?:
                    AppsFilter.InternetAccess.name)

                return@combine when (key) {
                    SectionsPagerAdapter.USER_APPS -> {
                        Pair(filter, orderAppsByFilter(
                            apps.filter { !it.system },
                            filter
                        ))
                    }
                    SectionsPagerAdapter.SYSTEM_APPS -> {
                        Pair(filter, orderAppsByFilter(
                            apps.filter { it.system },
                            filter
                        ))
                    }
                    else -> throw IllegalArgumentException("Incorrect key")
                }
            }.asLiveData(Dispatchers.IO)
    }

    private fun orderAppsByFilter(apps: List<IApp>, filter: AppsFilter): List<IApp> {
        return when (filter) {
            AppsFilter.Alphabetic -> {
                //Organizo la lista por el nombre de las aplicaciones
                apps.sortedBy { it.name }
            }
            AppsFilter.InternetAccess -> {
                //Resultado final
                val finalList = mutableListOf<IApp>()

                //Aplicaciones permitidas
                val allowedApps = apps.filter { it.access }
                //Aplicaciones bloqueadas
                val blockedApps = apps.filter { !it.access }

                if (allowedApps.isNotEmpty()) {
                    //Encabezado de aplicaciones permitidas.
                    finalList.add(HeaderApp.newInstance(
                        getApplication<Application>()
                            .getString(R.string.allowed_apps, allowedApps.size))
                    )
                }

                finalList.addAll(allowedApps.sortedBy { it.name })

                if (blockedApps.isNotEmpty()) {
                    //Encabezado de aplicaciones bloqueadas
                    finalList.add(
                        HeaderApp.newInstance(
                            getApplication<Application>()
                                .getString(R.string.blocked_apps, blockedApps.size))
                    )
                }

                finalList.addAll(blockedApps.sortedBy { it.name })

                finalList
            }
            AppsFilter.TrafficType -> {
                val finalList = mutableListOf<IApp>()

                val freeApps = getAppsByTrafficType(apps, TrafficType.Free)
                val nationalApps = getAppsByTrafficType(apps, TrafficType.National)
                val internationalApps = getAppsByTrafficType(apps, TrafficType.International)

                if (freeApps.isNotEmpty()) {
                    finalList.add(HeaderApp.newInstance(
                        getApplication<Application>()
                            .getString(R.string.free_apps, freeApps.size))
                    )
                }

                finalList.addAll(freeApps.sortedBy { it.name })

                if (nationalApps.isNotEmpty()) {
                    finalList.add(HeaderApp.newInstance(
                        getApplication<Application>()
                            .getString(R.string.national_apps, nationalApps.size)
                    ))
                }

                finalList.addAll(nationalApps.sortedBy { it.name })

                if (internationalApps.isNotEmpty()) {
                    finalList.add(HeaderApp.newInstance(
                        getApplication<Application>()
                            .getString(R.string.international_apps, internationalApps.size)
                    ))
                }

                finalList.addAll(internationalApps.sortedBy { it.name })

                finalList
            }
        }
    }

    private fun getAppsByTrafficType(apps: List<IApp>, trafficType: TrafficType): List<IApp> {
        val finalList = mutableListOf<IApp>()

        apps.forEach {
            if (it is App) {
                if (it.trafficType == trafficType)
                    finalList.add(it)
            } else if (it is AppGroup) {
                if (it.getMasterApp().trafficType == trafficType)
                    finalList.add(it)
            }
        }

        return finalList
    }
}

/**
 * Modelo que se usa para dibujar un encabezado en la lista. Solo
 * contiene el nombre del encabezado en la propiedad name. Las demas
 * propiedades están vacias o nulas. El método [accessHashCode] no está
 * soportado.
 * */
@Parcelize
private class HeaderApp private constructor(
    override var packageName: String,
    override var uid: Int,
    override var name: String,
    override var access: Boolean,
    override var system: Boolean,
    override var allowAnnotations: String?,
    override var blockedAnnotations: String?
) : IApp {

    override fun accessHashCode(): Long {
        throw UnsupportedOperationException("Not supported")
    }

    companion object {
        fun newInstance(headerText: String): HeaderApp {
            return HeaderApp(
                "",
                -1,
                headerText,
                access = false,
                system = false,
                null,
                null
            )
        }
    }
}

enum class AppsFilter {
    Alphabetic,
    InternetAccess,
    TrafficType
}