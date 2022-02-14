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
import com.smartsolutions.paquetes.helpers.DateCalendarUtils
import com.smartsolutions.paquetes.managers.NetworkUsageManager
import com.smartsolutions.paquetes.managers.contracts.IStatisticsManager
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import com.smartsolutions.paquetes.ui.SplashActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang.time.DateUtils
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@Keep
class LinearNotificationBuilder(
    context: Context,
    channelId: String
) : NotificationBuilder(context, channelId) {

    private val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        LinearNotificationEntryPoint::class.java
    )

    private val statisticsManager by lazy {
        return@lazy entryPoint.getStatisticsManager()
    }

    private val dateCalendarUtils by lazy {
        return@lazy entryPoint.getDateCalendarUtils()
    }

    private val networkUsageManager by lazy {
        return@lazy entryPoint.getNetworkUsageManager()
    }

    init {
        setSmallIcon(R.drawable.ic_main_notification)
        setContentTitle(context.getString(R.string.empty_noti_title))
        setContentText(context.getString(R.string.empty_noti_text))
        setContentIntent(getSplashActivityPendingIntent(context))
        setOngoing(true)
        setColorized(true)
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

            dataBytes.filter {
                it.type != DataBytes.DataType.National &&
                        it.type != DataBytes.DataType.MessagingBag
            }.forEach {
                    initialTotal += it.initialBytes
                    restTotal += it.bytes
                }

            val percent = if (initialTotal != 0L)
                (100 * restTotal / initialTotal).toInt()
            else
                0

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

            val usageUnitBytes = DataUnitBytes(initialTotal - restTotal)

            remoteViews.setTextViewText(
                R.id.usage_date,
                mContext.getString(R.string.usage_text, usageUnitBytes.toString())
            )

            getFirstExpiredDate(dataBytes)?.let {
                remoteViews.setTextViewText(
                    R.id.date_exp,
                    it.first
                )

                remoteViews.setTextViewText(
                    R.id.date_rest_days,
                    it.second
                )
            }

            val remainder = statisticsManager.getRemainder(TimeUnit.DAYS, dataBytes)

            val usageToday = runBlocking(Dispatchers.Default) {

                val period = dateCalendarUtils.getTimePeriod(DateCalendarUtils.PERIOD_TODAY)

                val traffics = networkUsageManager.getAppsUsage(period.first, period.second)

                var total = 0L

                traffics.forEach {
                    total += it.totalBytes.bytes
                }

                return@runBlocking DataUnitBytes(total)
            }

            remoteViews.setTextViewText(
                R.id.usage_today,
                mContext.getString(R.string.usage_today, usageToday.toString())
            )

            remoteViews.setTextViewText(
                R.id.rest_today,
                mContext.getString(R.string.rest_today, remainder.toString())
            )

            applyTheme(remoteViews)

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

        view.findViewById<TextView>(R.id.usage_today)
            .text = "Hoy: 125 mb"

        view.findViewById<TextView>(R.id.rest_today)
            .text = "Rest: 345 mb"

        return view
    }

    @SuppressLint("RestrictedApi")
    override fun getSummary(): Array<String> {
        return arrayOf(
            mContext.getString(R.string.lineal_notification),
            mContext.getString(R.string.lineal_notification_summary)
        )
    }

    private fun applyTheme(remoteViews: RemoteViews) {

        val textColor = if (isUIDarkTheme()) {
            Color.LTGRAY
        } else {
            Color.DKGRAY
        }

        val methodName = "setTextColor"

        remoteViews.setInt(R.id.rest_date, methodName, textColor)
        remoteViews.setInt(R.id.date_exp, methodName, textColor)
        remoteViews.setInt(R.id.usage_date, methodName, textColor)
        remoteViews.setInt(R.id.date_rest_days, methodName, textColor)
        remoteViews.setInt(R.id.usage_today, methodName, textColor)
        remoteViews.setInt(R.id.rest_today, methodName, textColor)

        remoteViews.setInt(
            R.id.root_view,
            "setBackgroundColor",
            getBackgroundColor()
        )

        if (isUIDarkTheme()) {
            remoteViews.setInt(
                R.id.divider,
                "setBackgroundColor",
                Color.LTGRAY
            )
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface LinearNotificationEntryPoint {

        fun getStatisticsManager(): IStatisticsManager

        fun getDateCalendarUtils(): DateCalendarUtils

        fun getNetworkUsageManager(): NetworkUsageManager
    }
}