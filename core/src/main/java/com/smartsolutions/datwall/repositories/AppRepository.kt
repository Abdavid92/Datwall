package com.smartsolutions.datwall.repositories

import android.content.Context
import com.google.gson.Gson
import com.smartsolutions.datwall.data.IAppDao
import com.smartsolutions.datwall.repositories.models.App
import com.smartsolutions.datwall.repositories.models.AppGroup
import com.smartsolutions.datwall.repositories.models.IApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Implementación de la interfaz IAppRepository
 * */
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
    private val listObserver = mutableListOf<Observer>()

    override val appsCount: Int
        get() = dao.appsCount

    override val appsAllowedCount: Int
        get() = dao.appsAllowedCount

    override val appsBlockedCount: Int
        get() = dao.appsBlockedCount

    override val all: List<App>
        get() = dao.apps

    override fun registerObserver(observer: Observer) {
        this.listObserver.add(observer)

        launch {
            val list = convertToListIApp(dao.apps)

            withContext(Dispatchers.Main) {
                observer.change(list)
            }
        }
    }

    override fun unregisterObserver(observer: Observer) {
        this.listObserver.remove(observer)
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
        refreshObservers()
    }

    override suspend fun create(app: App, task: (app: App) -> Unit) {
        dao.create(app)
        task(app)
        refreshObservers()
    }

    override suspend fun update(app: App) {
        dao.update(app)
        refreshObservers()
    }

    override suspend fun update(app: App, task: (app: App) -> Unit) {
        dao.update(app)
        task(app)
        refreshObservers()
    }

    override suspend fun update(apps: List<IApp>) {
        dao.update(convertToListApp(apps))
        refreshObservers()
    }

    override suspend fun update(apps: List<IApp>, task: (apps: List<IApp>) -> Unit) {
        dao.update(convertToListApp(apps))
        task(apps)
        refreshObservers()
    }

    override suspend fun delete(app: App) {
        dao.delete(app)
        refreshObservers()
    }

    override suspend fun delete(app: App, task: (app: App) -> Unit) {
        dao.delete(app)
        task(app)
        refreshObservers()
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
     * Lanza los observadores con datos frescos
     * */
    private fun refreshObservers() {
        launch {
            val list = convertToListIApp(dao.apps)

            withContext(Dispatchers.Main) {
                listObserver.forEach {
                    it.change(list)
                }
            }
        }
    }
}