package com.smartsolutions.paquetes.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import com.google.gson.Gson
import com.smartsolutions.paquetes.data.IAppDao
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.IApp
import javax.inject.Inject

class AppRepository @Inject constructor(
    context: Context,
    gson: Gson,
    private val dao: IAppDao
): BaseAppRepository(context, gson) {

    override val appsCount: Int
        get() = TODO("Not yet implemented")

    override val appsAllowedCount: Int
        get() = TODO("Not yet implemented")

    override val appsBlockedCount: Int
        get() = TODO("Not yet implemented")

    override fun getAll(): LiveData<List<IApp>> {
        TODO("Not yet implemented")
    }

    override suspend fun get(packageName: String): App? {
        TODO("Not yet implemented")
    }

    override suspend fun get(uid: Int): IApp {
        TODO("Not yet implemented")
    }

    override suspend fun create(app: App) {
        TODO("Not yet implemented")
    }

    override suspend fun create(app: App, task: (app: App) -> Unit) {
        TODO("Not yet implemented")
    }

    override suspend fun update(app: App) {
        TODO("Not yet implemented")
    }

    override suspend fun update(app: App, task: (app: App) -> Unit) {
        TODO("Not yet implemented")
    }

    override suspend fun update(apps: List<IApp>) {
        TODO("Not yet implemented")
    }

    override suspend fun update(apps: List<IApp>, task: (apps: List<IApp>) -> Unit) {
        TODO("Not yet implemented")
    }

    override suspend fun delete(app: App) {
        TODO("Not yet implemented")
    }

    override suspend fun delete(app: App, task: (app: App) -> Unit) {
        TODO("Not yet implemented")
    }
}