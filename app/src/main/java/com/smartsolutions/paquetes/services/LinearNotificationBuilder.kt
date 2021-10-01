package com.smartsolutions.paquetes.services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.Keep
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import com.smartsolutions.paquetes.ui.SplashActivity

@Keep
class LinearNotificationBuilder(
    context: Context,
    channelId: String
) : NotificationBuilder(context, channelId) {

    init {
        setSmallIcon(R.mipmap.ic_launcher_foreground)
        setContentTitle(context.getString(R.string.empty_noti_title))
        setContentText(context.getString(R.string.empty_noti_text))
        setContentIntent(
            PendingIntent
                .getActivity(
                    context,
                    0,
                    Intent(context, SplashActivity::class.java)
                        .setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        ),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }
                )
        )
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

            setCustomContentView(remoteViews)
        }

        return this
    }
}