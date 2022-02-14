package com.smartsolutions.paquetes.services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.helpers.UIHelper
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import com.smartsolutions.paquetes.settingsDataStore
import com.smartsolutions.paquetes.ui.SplashActivity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang.time.DateUtils
import java.text.SimpleDateFormat
import java.util.*

abstract class NotificationBuilder(
    context: Context,
    channelId: String
) : NotificationCompat.Builder(context, channelId) {

    private val uiHelper = UIHelper(context)

    /**
     * Construye una notificación personalizada usando los [UserDataBytes]
     * dados.
     *
     * @param dataBytes
     * */
    abstract fun setNotificationData(dataBytes: List<UserDataBytes>): NotificationBuilder

    /**
     * Obtiene una muestra de cómo se vería la notificación.
     *
     * @return [View]
     * */
    abstract fun getSample(parent: ViewGroup?): View

    /**
     * Obtiene el nombre y una descripción de las características de la notificación
     *
     * @return [Array]
     * */
    abstract fun getSummary(): Array<String>

    /**
     * Procesa y busca el dataByte con la fecha de expiración más grande.
     *
     * @return [Pair] El primer valor es un texto con el nombre del userDataBytes y
     * su fecha de expiración. El segundo valor es la cantidad de días restantes.
     * */
    @SuppressLint("RestrictedApi")
    protected fun getFirstExpiredDate(dataBytes: List<UserDataBytes>): Pair<String, String>? {

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

        var days = (date.time - System.currentTimeMillis()) / DateUtils.MILLIS_PER_DAY

        if (days < 0) {
            days = 0
        }

        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

        val dataTitle = getDataTitle(data.type)

        return mContext.getString(
            R.string.date_exp,
            dataTitle,
            dateFormat.format(date)
        ) to if(days > 0) mContext.getString(
            R.string.date_remainder,
            days
        ) else "Sincronice"
    }

    @SuppressLint("RestrictedApi")
    protected fun getBackgroundColor(): Int {

        return if (isUIDarkTheme())
            ContextCompat.getColor(mContext, R.color.background_dark)
        else
            ContextCompat.getColor(mContext, R.color.white)
    }

    /**
     * Indica si el modo oscuro está activado.
     * */
    @SuppressLint("RestrictedApi")
    protected fun isUIDarkTheme(): Boolean {

        return runBlocking {
            val notiThemeApp = mContext.settingsDataStore.data
                .firstOrNull()
                ?.get(PreferencesKeys.NOTIFICATION_STYLE_THEME_MODE_APP) == true

            return@runBlocking if (notiThemeApp)
                uiHelper.isAppUIDarkTheme()
            else
                uiHelper.isUIDarkTheme()
        }
    }

    companion object {

        /**
         * Implementación predeterminada del estilo de notificación.
         * */
        val DEFAULT_NOTIFICATION_IMPL: String = CircularNotificationBuilder::class.java.name

        fun newInstance(
            className: String,
            context: Context,
            channelId: String): NotificationBuilder {

            return Class.forName(className)
                .getConstructor(Context::class.java, String::class.java)
                .newInstance(context, channelId) as NotificationBuilder
        }

        /**
         * Obtiene un título legible a establecer en la notificación
         * usando el dataType dado.
         *
         * @param dataType - [DataBytes.DataType]
         *
         * @return [String] el título legible.
         * */
        fun getDataTitle(dataType: DataBytes.DataType): String {
            return when (dataType) {
                DataBytes.DataType.MessagingBag -> "Mensaj."
                DataBytes.DataType.International -> "3G/LTE"
                DataBytes.DataType.InternationalLte -> "LTE"
                DataBytes.DataType.PromoBonus -> "Promo"
                DataBytes.DataType.PromoBonusLte -> "Promo LTE"
                DataBytes.DataType.National -> ".CU"
                DataBytes.DataType.DailyBag -> "Bolsa"
            }
        }

        /**
         * Retorna un [PendingIntent] para ejecutar el [SplashActivity].
         * */
        fun getSplashActivityPendingIntent(context: Context): PendingIntent {
            return PendingIntent
                .getActivity(
                    context,
                    0,
                    Intent(context, SplashActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }
                )
        }

        /**
         * Retorna un array con los estilos de notificaciones disponibles.
         * */
        fun getNotificationStyles() = arrayOf(
            CircularNotificationBuilder::class.java,
            LinearNotificationBuilder::class.java,
            VerticalNotificationBuilder::class.java
        )
    }
}