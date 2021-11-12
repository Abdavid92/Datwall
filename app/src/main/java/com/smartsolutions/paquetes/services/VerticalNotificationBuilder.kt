package com.smartsolutions.paquetes.services

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViews
import androidx.annotation.Keep
import androidx.core.app.NotificationCompat
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

@Keep
class VerticalNotificationBuilder constructor(
    context: Context,
    channelID: String
) : NotificationBuilder(context, channelID) {

    init {
        setSmallIcon(R.drawable.ic_main_notification)
        setContentTitle(context.getString(R.string.empty_noti_title))
        setContentText(context.getString(R.string.empty_noti_text))
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

            for (i in dataBytes.indices) {

                val title = getDataTitle(dataBytes[i].type)

                addRemoteViewsContent(
                    remoteViews,
                    dataBytes[i],
                    title,
                    i != 0
                )
            }

            setCustomContentView(remoteViews)
            color = getBackgroundColor()
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
            mContext.getString(R.string.vertical_notification),
            mContext.getString(R.string.vertical_notification_summary)
        )
    }


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

        val color = if (uiHelper.isUIDarkTheme())
            Color.LTGRAY
        else
            Color.DKGRAY

        if (addSeparator) {
            val separator =
                RemoteViews(mContext.packageName, R.layout.item_datwall_service_separator)
                    .apply {

                        if (uiHelper.isUIDarkTheme())
                            setInt(
                                R.id.separator,
                                "setBackgroundColor",
                                Color.LTGRAY
                            )
                    }

            remoteViews.addView(R.id.content_view, separator)
        }

        val childRemotes = RemoteViews(mContext.packageName, R.layout.item_datwall_service_vertical).apply {
            setTextViewText(R.id.data_title, title)

            setInt(R.id.data_title, "setTextColor", color)
            setInt(R.id.data_percent, "setTextColor", Color.WHITE)
            setInt(R.id.data_bytes, "setTextColor", color)
            setInt(R.id.data_expire, "setTextColor", color)

            setProgressBar(
                R.id.data_progress,
                100,
                percent,
                false
            )

            val dataBytes = DataUnitBytes(userDataBytes.bytes)

            setTextViewText(
                R.id.data_bytes,
                dataBytes.toString()
            )

            setTextViewText(
                R.id.data_percent,
                if (userDataBytes.isExpired()) "exp" else "$percent%"
            )

            setTextViewText(
                R.id.data_expire,
                SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(userDataBytes.expiredTime))
            )
        }

        remoteViews.addView(R.id.content_view, childRemotes)
    }
}