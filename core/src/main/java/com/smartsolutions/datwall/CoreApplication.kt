package com.smartsolutions.datwall

import android.app.Application
import com.smartsolutions.datwall.watcher.Watcher
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Clase principal del Core
 * */
class CoreApplication : Application() {

    @Inject
    lateinit var watcher: Watcher

    override fun onCreate() {
        super.onCreate()

        watcher.start()
    }


    override fun onTerminate() {
        super.onTerminate()

        watcher.stop()
    }
}