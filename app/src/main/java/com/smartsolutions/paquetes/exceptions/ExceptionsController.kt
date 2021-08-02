package com.smartsolutions.paquetes.exceptions

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.os.ProcessCompat
import com.smartsolutions.paquetes.BuildConfig
import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.helpers.LocalFileHelper
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.ui.SplashActivity
import com.smartsolutions.paquetes.ui.exceptions.ExceptionsActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.system.exitProcess

class ExceptionsController @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val localFileHelper: LocalFileHelper,
    private val kernel: DatwallKernel,
    private val notificationHelper: NotificationHelper
) : Thread.UncaughtExceptionHandler {

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

        e.printStackTrace()

        val isForeground = kernel.isInForeground()

        if (e is MissingPermissionException) {
            when {
                e.permission.contains(Settings.ACTION_MANAGE_OVERLAY_PERMISSION) -> {
                    kernel.stopFirewall(true)
                    kernel.stopBubbleFloating(true)
                    notify(
                        context.getString(R.string.stoped_missing_overlay_permissions_title_notification),
                        context.getString(R.string.stoped_missing_overlay_permissions_description_notification)
                    )
                }

                e.permission.contains(IPermissionsManager.VPN_PERMISSION_KEY) -> {
                    kernel.stopFirewall(true)
                    notify(
                        context.getString(R.string.stoped_missing_vpn_permissions_title_notification),
                        context.getString(R.string.stoped_missing_vpn_permissions_description_notification)
                    )
                }

                else -> {
                    kernel.stopAllDatwall()
                    notify(
                        context.getString(R.string.missing_permmissions_title_notification),
                        context.getString(R.string.missing_permmissions_description_notification),
                        SplashActivity::class.java
                    )
                }
            }
        } else {
            kernel.stopAllDatwall()
            saveException(e)
            if (isForeground) {
                ContextCompat.startActivity(
                    context,
                    Intent(context, ExceptionsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    null
                )
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
        t.interrupt()
        Process.killProcess(Process.myPid())
        exitProcess(10)
    }


    private fun saveException(exception: Throwable): Boolean {
        var errorReport = ""

        val stacktrace = StringWriter()
        exception.printStackTrace(PrintWriter(stacktrace))

        errorReport += "Version App: ${BuildConfig.VERSION_CODE}\n"
        errorReport += "Fecha: ${SimpleDateFormat("dd/MM/yyyy hh:mm aa", Locale.US).format(Date())}\n\n"
        errorReport += "Causa del Error\n${ stacktrace}\n\n"
        errorReport += "Información Adicional\n"
        errorReport += "Fabricante: ${Build.MANUFACTURER}\n"
        errorReport += "Modelo: ${Build.MODEL}\n"
        errorReport += "Versión SDK: ${Build.VERSION.SDK_INT}\n"

        localFileHelper.saveToFileTemporal(errorReport, EXCEPTION_FILE_NAME, LocalFileHelper.TYPE_DIR_EXCEPTIONS)?.let {
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
                setContentTitle(title)
                setContentText(description)
                clazz?.let {
                    setContentIntent(
                        PendingIntent.getActivity(
                            context,
                            0,
                            Intent(context, clazz)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK),
                            0
                        )
                    )
                }
            }.build()
        )
    }


    companion object{
        const val EXCEPTION_FILE_NAME = "exception.json"
    }

}