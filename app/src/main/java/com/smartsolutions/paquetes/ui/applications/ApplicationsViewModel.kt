package com.smartsolutions.paquetes.ui.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.managers.contracts.IAppsManager
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.managers.models.AppListSection
import com.smartsolutions.paquetes.managers.models.AppsFilter
import com.smartsolutions.paquetes.repositories.contracts.IPreferencesRepository
import com.smartsolutions.paquetes.repositories.models.IApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ApplicationsUiState(
    val loading: Boolean = true,
    val filter: AppsFilter = AppsFilter.InternetAccess,
    val query: String? = null,
    /**
     * Secciones de la pestaña de apps del usuario. Con una búsqueda, una sola sección sin
     * cabecera con los resultados.
     * */
    val userSections: List<AppListSection> = emptyList(),
    val systemSections: List<AppListSection> = emptyList(),
    /**
     * Cambia con cada actualización: `App.equals` ignora el acceso, y sin esto un cambio de
     * acceso parecería "el mismo estado" y no llegaría a la pantalla.
     * */
    val revision: Long = 0
)

/**
 * Lo comparten la pantalla de aplicaciones y sus pestañas (apps del usuario y del sistema).
 * */
@OptIn(FlowPreview::class)
@HiltViewModel
class ApplicationsViewModel @Inject constructor(
    private val appsManager: IAppsManager,
    private val preferencesRepository: IPreferencesRepository,
    val iconManager: IIconManager
) : ViewModel() {

    private val query = MutableStateFlow<String?>(null)
    private var revision = 0L

    private val filter = preferencesRepository.preferences
        .map { prefs ->
            prefs.appsFilter
                ?.let { name -> AppsFilter.entries.firstOrNull { it.name == name } }
                ?: AppsFilter.InternetAccess
        }
        .distinctUntilChanged()

    val state: StateFlow<ApplicationsUiState> = combine(
        appsManager.flowApps(),
        filter,
        query.debounce { if (it.isNullOrBlank()) 0L else SEARCH_DELAY }
    ) { apps, filter, query ->
        ApplicationsUiState(
            loading = false,
            filter = filter,
            query = query,
            userSections = sections(apps.filter { !it.system }, filter, query),
            systemSections = sections(apps.filter { it.system }, filter, query),
            revision = ++revision
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ApplicationsUiState())

    /**
     * Apps con cambios de acceso sin guardar.
     * */
    private val appsToUpdate = mutableListOf<IApp>()

    fun onFilterChanged(filter: AppsFilter) {
        viewModelScope.launch {
            preferencesRepository.setAppsFilter(filter.name)
        }
    }

    fun onQueryChanged(query: String?) {
        this.query.value = query
    }

    /**
     * Se cambió el acceso de [app] (en la lista o en su pantalla de control).
     * */
    fun onAccessChanged(app: IApp) {
        val index = appsToUpdate.indexOf(app)

        if (index != -1) {
            appsToUpdate[index] = app
        } else {
            appsToUpdate.add(app)
        }
    }

    /**
     * Guarda los cambios pendientes. Devuelve false si no había ninguno.
     * */
    fun onCommitChanges(): Boolean {
        if (appsToUpdate.isEmpty())
            return false

        val apps = appsToUpdate.toList()
        appsToUpdate.clear()

        viewModelScope.launch {
            appsManager.commit(apps)
        }
        return true
    }

    private fun sections(apps: List<IApp>, filter: AppsFilter, query: String?): List<AppListSection> =
        if (query.isNullOrBlank())
            appsManager.sections(apps, filter)
        else
            listOf(AppListSection(null, appsManager.search(apps, query)))

    private companion object {
        const val SEARCH_DELAY = 200L
    }
}
