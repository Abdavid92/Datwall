package com.smartsolutions.paquetes.repositories

import android.content.Context
import com.google.gson.Gson
import com.smartsolutions.paquetes.data.IAppDao
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppGroup
import com.smartsolutions.paquetes.repositories.models.IApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de la interfaz IAppRepository
 * */
@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext
    context: Context,
    gson: Gson,
    private val dao: IAppDao
): BaseAppRepository(context, gson) {

    override suspend fun appsCount(): Int = dao.appsCount()

    override suspend fun appsAllowedCount(): Int = dao.appsAllowedCount()

    override suspend fun appsBlockedCount(): Int = dao.appsBlockedCount()

    override suspend fun all(): List<App> = dao.apps()

    override fun flow(): Flow<List<App>> = dao.flow()

    override fun flowByGroup(): Flow<List<IApp>> =
        dao.flow().map {
            return@map convertToListIApp(it)
        }

    override suspend fun get(packageName: String): App? = dao.get(packageName)

    override suspend fun get(uid: Int): IApp? {
        //Obtengo los datos
        val apps = dao.get(uid)

        if (apps.isEmpty())
            return null

        return if (apps.size == 1)
            //Si la lista de aplicaciones es de uno retorno una instancia de App
            apps[0]
        else {
            //Sino creo un grupo y lo retorno
            val appGroup = AppGroup(
                "",
                uid,
                "",
            apps.toMutableList(),
            null,
            null)

            fillAppGroup(appGroup)
        }
    }

    override suspend fun getAllByGroup(): List<IApp> = convertToListIApp(dao.apps())

    override suspend fun create(app: App) = dao.create(app)

    override suspend fun create(apps: List<IApp>) = dao.create(convertToListApp(apps))

    override suspend fun update(app: App) = dao.update(app)

    override suspend fun update(apps: List<IApp>) = dao.update(convertToListApp(apps))

    override suspend fun delete(app: App) = dao.delete(app)

    override suspend fun delete(apps: List<IApp>) = dao.delete(convertToListApp(apps))

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
                    app.packageName,
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
}