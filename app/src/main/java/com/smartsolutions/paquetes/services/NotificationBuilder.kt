package com.smartsolutions.paquetes.services

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.helpers.UIHelper
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import java.text.SimpleDateFormat
import java.util.*

abstract class NotificationBuilder(
    context: Context,
    channelId: String
) : NotificationCompat.Builder(context, channelId) {

    protected val uiHelper = UIHelper(context)

    /**
     * Construye una notificación personalizada usando los [UserDataBytes]
     * dados.
     *
     * @param dataBytes
     * */
    abstract fun setNotificationData(dataBytes: List<UserDataBytes>): NotificationBuilder

    @SuppressLint("RestrictedApi")
    protected fun getFirstExpiredDate(dataBytes: List<UserDataBytes>): String? {
        if (dataBytes.isEmpty())
            return null

        var data = dataBytes[0]

        dataBytes.forEach {
            if (data.expiredTime > it.expiredTime)
                data = it
            else if (data.expiredTime == it.expiredTime && data.priority < it.priority)
                data = it
        }

        val date = Date(data.expiredTime)

        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

        val dataTitle = getDataTitle(data.type)

        return mContext.getString(
            R.string.date_exp,
            dataTitle,
            dateFormat.format(date)
        )
    }

    /**
     * Obtiene un título legible a establecer en la notificación
     * usando el dataType dado.
     *
     * @param dataType - [DataBytes.DataType]
     *
     * @return [String] el título legible.
     * */
    protected fun getDataTitle(dataType: DataBytes.DataType): String {
        return when (dataType) {
            DataBytes.DataType.International -> "Internacional"
            DataBytes.DataType.InternationalLte -> "Lte"
            DataBytes.DataType.PromoBonus -> "Promoción"
            DataBytes.DataType.National -> "Nacional"
            DataBytes.DataType.DailyBag -> "Bolsa diaria"
        }
    }

    @SuppressLint("RestrictedApi")
    protected fun getBackgroundColor(): Int {
        return if (uiHelper.isUIDarkTheme())
            ContextCompat.getColor(mContext, R.color.background_dark)
        else
            ContextCompat.getColor(mContext, R.color.white)
    }

    companion object {

        fun newInstance(
            className: String,
            context: Context,
            channelId: String): NotificationBuilder {

            return Class.forName(className)
                .getConstructor(Context::class.java, String::class.java)
                .newInstance(context, channelId) as NotificationBuilder
        }
    }
}