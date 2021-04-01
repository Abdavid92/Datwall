package com.smartsolutions.datwall.repositories

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.gson.Gson
import com.smartsolutions.datwall.data.IAppDao
import com.smartsolutions.datwall.repositories.models.App
import com.smartsolutions.datwall.repositories.models.AppGroup
import com.smartsolutions.datwall.repositories.models.IApp
import com.smartsolutions.datwall.watcher.ChangeType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Implementación de la interfaz IAppRepository
 * */
@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext
    context: Context,
    gson: Gson,
    private val dao: IAppDao
): BaseAppRepository(context, gson), CoroutineScope {

    //Contexto de las co-rutinas
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    //Lista de observadores
    private val listObserver = mutableListOf<Pair<LifecycleOwner?, Observer>>()

    override val appsCount: Int
        get() = dao.appsCount

    override val appsAllowedCount: Int
        get() = dao.appsAllowedCount

    override val appsBlockedCount: Int
        get() = dao.appsBlockedCount

    override val all: List<App>
        get() = dao.apps

    override fun registerObserver(observer: Observer) {
        this.listObserver.add(Pair(null, observer))

        launch {
            val list = convertToListIApp(dao.apps)

            withContext(Dispatchers.Main) {
                observer.onChange(list)
            }
        }
    }

    override fun registerObserver(lifecycleOwner: LifecycleOwner, observer: Observer) {
        this.listObserver.add(Pair(lifecycleOwner, observer))

        launch {
            val list = convertToListIApp(dao.apps)

            withContext(Dispatchers.Main) {
                observer.onChange(list)
            }
        }
    }

    override fun unregisterObserver(observer: Observer) {
        this.listObserver.firstOrNull { it.second == observer }?.let {
            this.listObserver.remove(it)
        }
    }

    override suspend fun get(packageName: String): App? = dao.get(packageName)

    override suspend fun get(uid: Int): IApp {
        //Obtengo los datos
        val apps = dao.get(uid)

        return if (apps.size == 1)
            //Si la lista de aplicaciones es de uno retorno una instancia de App
            apps[0]
        else {
            //Sino creo un grupo y lo retorno
            val appGroup = AppGroup(
                uid,
                "",
            apps.toMutableList(),
            null,
            null)

            fillAppGroup(appGroup)
        }
    }

    override suspend fun getAllByGroup(): List<IApp> = convertToListIApp(dao.apps)

    override suspend fun create(app: App) {
        dao.create(app)
        observersOnChangeType(listOf(app), ChangeType.Created)
    }

    override suspend fun create(apps: List<IApp>) {
        val list = convertToListApp(apps)

        dao.create(list)
        observersOnChangeType(list, ChangeType.Created)
    }

    override suspend fun update(app: App) {
        if (dao.update(app) > 0)
            observersOnChangeType(listOf(app), ChangeType.Updated)
    }

    override suspend fun update(apps: List<IApp>) {
        val list = convertToListApp(apps)

        if (dao.update(list) > 0)
            observersOnChangeType(list, ChangeType.Updated)
    }

    override suspend fun delete(app: App) {
        if (dao.delete(app) > 0)
            observersOnChangeType(listOf(app), ChangeType.Deleted)
    }

    override suspend fun delete(apps: List<IApp>) {
        val list = convertToListApp(apps)

        if (dao.delete(list) > 0)
            observersOnChangeType(list, ChangeType.Deleted)
    }

    /**
     * Convierte una lista de IApp en una lista de App
     * */
    private fun convertToListApp(apps: List<IApp>): List<App> {
        //Lista final de App
        val list = mutableListOf<App>()

        apps.forEach {
            //Si el elemento es una App la añado a la lista
            if (it is App)
                list.add(it)
            else if (it is AppGroup) {
                //Si es un grupo añado las apps
                it.forEach { app ->
                    list.add(app)
                }
            }
        }
        return list
    }

    private fun convertToListIApp(apps: List<App>): List<IApp> {
        val listGroup = mutableListOf<AppGroup>()

        apps.forEach { app ->
            var appGroup = listGroup.firstOrNull { it.uid == app.uid }

            if (appGroup != null) {
                appGroup.add(app)
            } else {
                appGroup = AppGroup(
                    app.uid,
                    "",
                    mutableListOf(app),
                    null,
                    null
                )
                listGroup.add(appGroup)
            }
        }

        return MutableList(listGroup.size) {
            return@MutableList if (listGroup[it].size == 1)
                listGroup[it][0]
            else {
                fillAppGroup(listGroup[it])
            }
        }
    }

    /**
     * Lanza los eventos de los observadores
     * */
    private suspend fun observersOnChangeType(apps: List<App>, type: ChangeType) {

        //Lista de todas las aplcaciones que se usará para lanzar el evento onChange
        val all = convertToListIApp(dao.apps)

        /*Cambio de contexto hacia el hilo principal para poder
          tocar vistas dentro de
          estos eventos*/
        withContext(Dispatchers.Main) {
            listObserver.forEach {

                //Si el ciclo de vida está destruido
                if (it.first?.lifecycle?.currentState == Lifecycle.State.DESTROYED)
                    //Elimino el observador
                    unregisterObserver(it.second)
                else {
                    //Lanzo el evento correspondiente al cambio
                    when (type) {
                        ChangeType.Created -> {
                            it.second.onCreate(apps)
                        }
                        ChangeType.Updated -> {
                            it.second.onUpdate(apps)
                        }
                        ChangeType.Deleted -> {
                            it.second.onDelete(apps)
                        }
                        ChangeType.None -> {
                            //None
                        }
                    }
                    //Lanzo el evento onChange
                    it.second.onChange(all)
                }
            }
        }
    }
}