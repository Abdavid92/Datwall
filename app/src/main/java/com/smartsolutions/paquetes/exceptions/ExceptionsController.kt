package com.smartsolutions.paquetes.exceptions

import android.app.Activity
import android.app.ActivityManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.*
import com.smartsolutions.paquetes.helpers.LocalFileHelper
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.services.BubbleFloatingService
import com.smartsolutions.paquetes.services.DatwallService
import com.smartsolutions.paquetes.ui.SplashActivity
import com.smartsolutions.paquetes.ui.exceptions.ExceptionsActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

class ExceptionsController @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val localFileHelper: LocalFileHelper,
    private val notificationHelper: NotificationHelper
) : Thread.UncaughtExceptionHandler, CoroutineScope {

    var isRegistered = false
        private set

    fun register() {
        if (!isRegistered) {
            Thread.setDefaultUncaughtExceptionHandler(this)
            isRegistered = true
        }
    }

    fun unregister() {
        Thread.setDefaultUncaughtExceptionHandler(null)
        isRegistered = false
    }


    override fun uncaughtException(t: Thread, e: Throwable) {

        launch(Dispatchers.IO) {
            context.internalDataStore.edit {
                it[PreferencesKeys.IS_THROWED] = true
            }
        }

        Toast.makeText(context, "Exception Detected", Toast.LENGTH_SHORT).show()

        e.printStackTrace()

        if (e is MissingPermissionException) {
            notify(
                context.getString(R.string.missing_permmissions_title_notification),
                context.getString(R.string.missing_permmissions_description_notification),
                SplashActivity::class.java
            )
        } else {
            kotlin.runCatching {
                saveException(e)
            }

            if (isInForeground()) {
                context.startActivity(Intent(context, ExceptionsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } else {
                notify(
                    context.getString(R.string.generic_error_title_notification),
                    context.getString(R.string.generic_error_description_notification),
                    ExceptionsActivity::class.java
                )
            }
        }

        closeThread(t)
    }


    private fun closeThread(t: Thread) {
        kotlin.runCatching {
            context.stopService(Intent(context, BubbleFloatingService::class.java))
        }
        kotlin.runCatching {
            context.stopService(Intent(context, DatwallService::class.java))
        }
        notificationHelper.cancelNotification(NotificationHelper.MAIN_NOTIFICATION_ID)
        t.interrupt()
        Process.killProcess(Process.myPid())
        exitProcess(0)
    }


    private fun saveException(exception: Throwable): Boolean {
        var errorReport = ""

        val stacktrace = StringWriter()
        exception.printStackTrace(PrintWriter(stacktrace))

        errorReport += "Version App: ${BuildConfig.VERSION_CODE}\n"
        errorReport += "Fecha: ${
            SimpleDateFormat(
                "dd/MM/yyyy hh:mm aa",
                Locale.US
            ).format(Date())
        }\n\n"
        errorReport += "Causa del Error\n${stacktrace}\n\n"
        errorReport += "Información Adicional\n"
        errorReport += "Fabricante: ${Build.MANUFACTURER}\n"
        errorReport += "Modelo: ${Build.MODEL}\n"
        errorReport += "Versión SDK: ${Build.VERSION.SDK_INT}\n"

        localFileHelper.saveToFileTemporal(
            errorReport,
            EXCEPTION_FILE_NAME,
            LocalFileHelper.TYPE_DIR_EXCEPTIONS
        )?.let {
            return true
        }
        return false
    }


    private fun notify(title: String, description: String, clazz: Class<out Activity>? = null) {
        notificationHelper.notify(
            NotificationHelper.ALERT_NOTIFICATION_ID,
            notificationHelper.buildNotification(
                NotificationHelper.ALERT_CHANNEL_ID
            ).apply {
                setAutoCancel(true)
                setContentTitle(title)
                setContentText(description)
                clazz?.let {
                    setContentIntent(
                        PendingIntent.getActivity(
                            context,
                            0,
                            Intent(context, clazz)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            0
                        )
                    )
                }
            }.build()
        )
    }


    private fun isInForeground(): Boolean {
        return ContextCompat.getSystemService(
            context,
            ActivityManager::class.java
        )?.runningAppProcesses?.any {
            it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    it.processName == context.packageName
        } ?: false
    }

    companion object {
        const val EXCEPTION_FILE_NAME = "exception.json"
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

}