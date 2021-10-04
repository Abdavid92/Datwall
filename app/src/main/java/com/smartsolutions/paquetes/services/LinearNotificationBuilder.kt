package com.smartsolutions.paquetes.services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RemoteViews
import android.widget.TextView
import androidx.annotation.Keep
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import com.smartsolutions.paquetes.ui.SplashActivity
import kotlin.random.Random

@Keep
class LinearNotificationBuilder(
    context: Context,
    channelId: String
) : NotificationBuilder(context, channelId) {

    init {
        setSmallIcon(R.mipmap.ic_launcher_foreground)
        setContentTitle(context.getString(R.string.empty_noti_title))
        setContentText(context.getString(R.string.empty_noti_text))
        setContentIntent(getSplashActivityPendingIntent(context))
        setOngoing(true)
        color = getBackgroundColor()
    }

    @SuppressLint("RestrictedApi")
    override fun setNotificationData(dataBytes: List<UserDataBytes>): NotificationBuilder {
        if (dataBytes.isEmpty()) {
            setCustomContentView(null)
            setContentTitle(mContext.getString(R.string.empty_noti_title))
            setContentText(mContext.getString(R.string.empty_noti_text))
        } else {
            val remoteViews = RemoteViews(
                mContext.packageName,
                R.layout.linear_notification
            )

            var initialTotal = 0L
            var restTotal = 0L

            dataBytes.filter { it.type != DataBytes.DataType.National }
                .forEach {
                    initialTotal += it.initialBytes
                    restTotal += it.bytes
                }

            val percent = (100 * restTotal / initialTotal).toInt()

            remoteViews.setProgressBar(
                R.id.data_progress,
                100,
                percent,
                false
            )

            val restUnitBytes = DataUnitBytes(restTotal)

            remoteViews.setTextViewText(
                R.id.rest_date,
                mContext.getString(R.string.rest_text, restUnitBytes.toString())
            )

            getFirstExpiredDate(dataBytes)?.let {
                remoteViews.setTextViewText(
                    R.id.date_exp,
                    it
                )
            }

            val textColor = if (uiHelper.isUIDarkTheme()) {
                Color.LTGRAY
            } else {
                Color.DKGRAY
            }

            val methodName = "setTextColor"

            remoteViews.setInt(R.id.rest_date, methodName, textColor)
            remoteViews.setInt(R.id.date_exp, methodName, textColor)

            setCustomContentView(remoteViews)
        }

        return this
    }

    @SuppressLint("RestrictedApi", "SetTextI18n")
    override fun getSample(parent: ViewGroup?): View {
        val inflater = LayoutInflater.from(mContext)

        val view = inflater.inflate(R.layout.linear_notification, parent, false)

        view.findViewById<ProgressBar>(R.id.data_progress)
            .progress = Random(System.currentTimeMillis())
            .nextInt(100)

        view.findViewById<TextView>(R.id.rest_date)
            .text = "Restante: 1.5 GB"

        view.findViewById<TextView>(R.id.date_exp)
            .text = "Internacional expira el 29/11"

        return view
    }

    @SuppressLint("RestrictedApi")
    override fun getSummary(): String {
        return mContext.getString(R.string.lineal_notification_summary)
    }
}