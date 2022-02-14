package com.smartsolutions.paquetes.ui.applications

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.datastore.preferences.core.edit
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import androidx.recyclerview.widget.DiffUtil
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.settingsDataStore
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppGroup
import com.smartsolutions.paquetes.repositories.models.IApp
import com.smartsolutions.paquetes.repositories.models.TrafficType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ApplicationsViewModel @Inject constructor(
    application: Application,
    private val appRepository: IAppRepository,
    val iconManager: IIconManager
) : AndroidViewModel(application) {

    private var currentFilter: AppsFilter = AppsFilter.InternetAccess

    /**
     * Se invoca cuando se establece un nuevo filtro.
     * */
    var filterChangeListener: ((filter: AppsFilter) -> Unit)? = null

    /**
     * Lista de aplicaciones pendientes a actualizar.
     * */
    private val appsToUpdate = mutableListOf<IApp>()

    private var searchJob: Job? = null

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
            viewModelScope.launch {
                withContext(Dispatchers.IO){
                    appRepository.update(appsToUpdate)
                }

                appsToUpdate.clear()
            }
            return true
        }
        return false
    }

    fun setFilter(filter: AppsFilter) {
        if (currentFilter != filter)
            filterChangeListener?.invoke(filter)

        viewModelScope.launch(Dispatchers.IO) {
            getApplication<Application>()
                .settingsDataStore.edit {
                    it[PreferencesKeys.APPS_FILTER] = filter.name
                }
        }
    }

    fun getApps(key: String): LiveData<Pair<AppsFilter, List<IApp>>> {
        return appRepository.flowByGroup()
            .combine(getApplication<Application>().settingsDataStore.data) { apps, preferences ->

                delay(500)

                val filter = AppsFilter.valueOf(
                    preferences[PreferencesKeys.APPS_FILTER] ?:
                    AppsFilter.InternetAccess.name)

                currentFilter = filter

                return@combine withContext(Dispatchers.Default){
                    when (key) {
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
                }
            }.asLiveData(Dispatchers.IO)
    }

    fun scheduleSearchQuery(query: String?, adapter: AppsListAdapter) {

        searchJob?.cancel()
        searchJob = null

        searchJob = viewModelScope.launch(Dispatchers.Default) {

            delay(200)

            if (this.isActive) {

                val newList = if (query != null && query.isNotBlank()) {
                    adapter.list.where { it.name.contains(query, true) }
                } else {
                    adapter.list
                }.toMutableList()

                val result = DiffUtil.calculateDiff(DiffCallback(
                    adapter.list,
                    newList,
                    false)
                )

                withContext(Dispatchers.Main) {
                    adapter.updateList(result, newList)
                }
            }
        }
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
                            .getString(R.string.allowed_applications, allowedApps.size))
                    )
                }

                finalList.addAll(allowedApps.sortedBy { it.name })

                if (blockedApps.isNotEmpty()) {
                    //Encabezado de aplicaciones bloqueadas
                    finalList.add(
                        HeaderApp.newInstance(
                            getApplication<Application>()
                                .getString(R.string.blocked_applications, blockedApps.size))
                    )
                }

                finalList.addAll(blockedApps.sortedBy { it.name })

                finalList
            }
            AppsFilter.TrafficType -> {
                val finalList = mutableListOf<IApp>()

                TrafficType.values().forEach {
                    val list = getAppsByTrafficType(apps, it)

                    if (list.isNotEmpty()){
                        finalList.add(HeaderApp.newInstance(
                            getApplication<Application>()
                                .getString(
                                    when (it){
                                        TrafficType.Free -> R.string.free_apps
                                        TrafficType.National -> R.string.national_apps
                                        TrafficType.International -> R.string.international_apps
                                        TrafficType.Messaging -> R.string.messaging_apps
                                    },
                                    list.size
                                ))
                        )

                        finalList.addAll(list.sortedBy { it.name })
                    }
                }

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

    private class DiffCallback(
        private val oldList: List<IApp>,
        private val newList: List<IApp>,
        private val changeFilter: Boolean
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition] && !changeFilter
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].accessHashToken() ==
                    newList[newItemPosition].accessHashToken()
        }

    }
}

inline fun List<IApp>.where(predicate: (IApp) -> Boolean): List<IApp> {
    val result = mutableListOf<IApp>()

    forEach {
        if (it is AppGroup) {
            result.addAll(it.filter(predicate))
        } else {
            if (predicate(it))
                result.add(it)
        }
    }
    return result
}

/**
 * Modelo que se usa para dibujar un encabezado en la lista. Solo
 * contiene el nombre del encabezado en la propiedad name. Las demas
 * propiedades están vacias o nulas. El método [accessHashToken] no está
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

    override fun accessHashToken(): String {
        return "0"
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