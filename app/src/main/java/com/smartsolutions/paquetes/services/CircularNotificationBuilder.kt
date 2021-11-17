package com.smartsolutions.paquetes.services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.RemoteViews
import androidx.annotation.Keep
import androidx.core.app.NotificationCompat
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import kotlin.random.Random

@Keep
class CircularNotificationBuilder(
    context: Context,
    channelId: String
) : NotificationBuilder(context, channelId) {

    init {
        setSmallIcon(R.drawable.ic_main_notification)
        setContentTitle(context.getString(R.string.empty_noti_title))
        setContentText(context.getString(R.string.empty_noti_text))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            setStyle(NotificationCompat.DecoratedCustomViewStyle())

        setContentIntent(getSplashActivityPendingIntent(context))
        setOngoing(true)
        setColorized(true)
        color = getBackgroundColor()
    }

    @SuppressLint("RestrictedApi")
    override fun setNotificationData(dataBytes: List<UserDataBytes>): NotificationBuilder {
        if (dataBytes.isEmpty()) {
            setCustomContentView(null)
            setCustomBigContentView(null)
            setContentTitle(mContext.getString(R.string.empty_noti_title))
            setContentText(mContext.getString(R.string.empty_noti_text))
        } else {
            val remoteViews = RemoteViews(
                mContext.packageName,
                R.layout.circular_notification
            )

            val expandedRemoteViews = RemoteViews(
                mContext.packageName,
                R.layout.circular_notification_expanded
            )

            for (i in dataBytes.indices) {

                val title = getDataTitle(dataBytes[i].type)

                addRemoteViewsContent(
                    remoteViews,
                    dataBytes[i],
                    title,
                    i != 0
                )

                addExpandedRemoteViewContent(
                    expandedRemoteViews,
                    dataBytes[i],
                    title,
                    i != 0
                )
            }

            setFirstExpiredDate(expandedRemoteViews, dataBytes)

            val backgroundColor = getBackgroundColor()

            remoteViews.setInt(
                R.id.content_view,
                "setBackgroundColor",
                backgroundColor
            )

            expandedRemoteViews.setInt(
                R.id.root_layout,
                "setBackgroundColor",
                backgroundColor
            )

            setCustomContentView(remoteViews)
            setCustomBigContentView(expandedRemoteViews)

            color = backgroundColor
        }

        return this
    }

    @SuppressLint("RestrictedApi")
    override fun getSample(parent: ViewGroup?): View {
        val remoteViews = RemoteViews(mContext.packageName, R.layout.circular_notification)

        for (i in DataBytes.DataType.values().indices) {
            val dataBytes = UserDataBytes(
                "",
                DataBytes.DataType.values()[i],
                100,
                Random(System.currentTimeMillis()).nextLong(100),
                System.currentTimeMillis(),
                System.currentTimeMillis() + 1000000
            )

            addRemoteViewsContent(
                remoteViews,
                dataBytes,
                getDataTitle(dataBytes.type),
                i != 0
            )
        }

        return remoteViews.apply(mContext, parent)
    }

    @SuppressLint("RestrictedApi")
    override fun getSummary(): Array<String> {
        return arrayOf(
            mContext.getString(R.string.circular_notification),
            mContext.getString(R.string.circular_notification_summary)
        )
    }

    /**
     * Agrega el nuevo contenido a la notificación colapsada.
     *
     * @param userDataBytes - [UserDataBytes] con los valores a usar.
     * @param title - Título del contenido.
     * @param addSeparator - Indica si se debe agregar un separador antes del contenido.
     * */
    @SuppressLint("RestrictedApi")
    private fun addRemoteViewsContent(
        remoteViews: RemoteViews,
        userDataBytes: UserDataBytes,
        title: String,
        addSeparator: Boolean
    ) {

        val percent = if (userDataBytes.initialBytes != 0L)
            (100 * userDataBytes.bytes / userDataBytes.initialBytes)
                .toInt()
        else
            0

        val color = if (isUIDarkTheme())
            Color.LTGRAY
        else
            Color.DKGRAY

        if (addSeparator) {
            val separator = RemoteViews(mContext.packageName, R.layout.item_datwall_service_separator)
                .apply {

                    if (isUIDarkTheme())
                        setInt(
                            R.id.separator,
                            "setBackgroundColor",
                            Color.LTGRAY
                        )
                }

            remoteViews.addView(R.id.content_view, separator)
        }

        val childRemotes = RemoteViews(mContext.packageName, R.layout.item_datwall_service).apply {
            setTextViewText(R.id.data_title, title)
            setInt(R.id.data_title, "setTextColor", color)

            setProgressBar(
                R.id.data_progress,
                100,
                percent,
                false
            )

            setTextViewText(
                R.id.data_percent,
                if (userDataBytes.isExpired()) "exp" else "$percent%"
            )
            setInt(R.id.data_percent, "setTextColor", color)
        }

        remoteViews.addView(R.id.content_view, childRemotes)
    }

    /**
     * Agrega el nuevo contenido a la notificación expandida.
     *
     * @param userDataBytes - [UserDataBytes] con los valores a usar.
     * @param title - Título del contenido.
     * @param addSeparator - Indica si se debe agregar un separador antes del contenido.
     * */
    @SuppressLint("RestrictedApi")
    private fun addExpandedRemoteViewContent(
        expandedRemoteViews: RemoteViews,
        userDataBytes: UserDataBytes,
        title: String,
        addSeparator: Boolean
    ) {

        val percent = if (userDataBytes.initialBytes != 0L)
            (100 * userDataBytes.bytes / userDataBytes.initialBytes)
                .toInt()
        else
            0

        val color = if (isUIDarkTheme())
            Color.LTGRAY
        else
            Color.DKGRAY

        if (addSeparator) {
            val separator = RemoteViews(mContext.packageName, R.layout.item_datwall_service_separator)
                .apply {

                    if (isUIDarkTheme())
                        setInt(
                            R.id.separator,
                            "setBackgroundColor",
                            Color.LTGRAY
                        )
                }

            expandedRemoteViews.addView(R.id.content_view, separator)
        }

        val childRemotes = RemoteViews(mContext.packageName, R.layout.item_datwall_service_expanded).apply {
            setTextViewText(R.id.data_title, title)
            setInt(R.id.data_title, "setTextColor", color)


            setProgressBar(
                R.id.data_progress,
                100,
                percent,
                false
            )

            setTextViewText(
                R.id.data_percent,
                if (userDataBytes.isExpired()) "exp" else "$percent%"
            )
            setInt(R.id.data_percent, "setTextColor", color)

            val dataBytes = DataUnitBytes(userDataBytes.bytes)

            setTextViewText(
                R.id.data_bytes,
                dataBytes.toString()
            )
            setInt(R.id.data_bytes, "setTextColor", color)
        }

        expandedRemoteViews.addView(R.id.content_view, childRemotes)
    }

    /**
     * Establece la fecha de expiración del paquete más próximo a vencer
     * en la notificación expandida.
     *
     * @param userData
     * */
    @SuppressLint("RestrictedApi")
    private fun setFirstExpiredDate(
        expandedRemoteViews: RemoteViews,
        userData: List<UserDataBytes>
    ) {
        getFirstExpiredDate(userData)?.let {
            expandedRemoteViews.setTextViewText(
                R.id.date_exp,
                it
            )

            expandedRemoteViews.setInt(
                R.id.date_exp,
                "setTextColor",
                if (isUIDarkTheme())
                    Color.LTGRAY
                else
                    Color.DKGRAY
            )
        }
    }
}