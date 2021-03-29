package com.smartsolutions.datwall.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.smartsolutions.datwall.data.IAppDao
import com.smartsolutions.datwall.repositories.models.App
import com.smartsolutions.datwall.repositories.models.AppGroup
import com.smartsolutions.datwall.repositories.models.IApp
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.ref.WeakReference
import javax.inject.Inject

class AppRepository @Inject constructor(
    @ApplicationContext
    context: Context,
    gson: Gson,
    private val dao: IAppDao
): BaseAppRepository(context, gson) {

    private val convertedData = WeakReference<MutableLiveData<List<IApp>>>(MutableLiveData())

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
        observer.change(convertToListIApp(dao.apps))
        this.listObserver.add(observer)
    }

    override fun unregisterObserver(observer: Observer) {
        this.listObserver.remove(observer)
    }

    override fun liveData(): LiveData<List<IApp>> = convertedData.get() ?: MutableLiveData()

    override suspend fun get(packageName: String): App? = dao.get(packageName)

    override suspend fun get(uid: Int): IApp {
        val apps = dao.get(uid)

        return if (apps.size == 1)
            apps[0]
        else {
            val appGroup = AppGroup(
                uid,
                "",
            apps.toMutableList(),
            null,
            null)
            appGroup.name = createGroupName(appGroup)
            appGroup
        }
    }

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

    private fun convertToListApp(apps: List<IApp>): List<App> {
        val list = mutableListOf<App>()

        apps.forEach {
            if (it is App)
                list.add(it)
            else if (it is AppGroup) {
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

                fillAppGroup(appGroup)

                listGroup.add(appGroup)
            }
        }

        return MutableList(listGroup.size) {
            return@MutableList if (listGroup[it].size == 1)
                listGroup[it][0]
            else
                listGroup[it]
        }
    }

    private fun createGroupName(appGroup: AppGroup): String {
        return "new group"
    }

    private fun refreshObservers() {
        listObserver.forEach {
            it.change(convertToListIApp(dao.apps))
        }
    }
}