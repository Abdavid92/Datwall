package com.smartsolutions.paquetes.helpers

import android.content.Context
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.Sim

/**
 * Textos de presentación de los modelos. Los usan la UI y las notificaciones; los modelos no
 * producen textos.
 * */
object DisplayTexts {

    /**
     * Nombre legible de un [DataBytes.DataType].
     * */
    fun dataTypeName(type: DataBytes.DataType, context: Context): String = when (type) {
        DataBytes.DataType.MessagingBag -> context.getString(R.string.data_type_messaging_bag)
        DataBytes.DataType.International -> context.getString(R.string.data_type_international)
        DataBytes.DataType.InternationalLte -> context.getString(R.string.data_type_international_lte)
        DataBytes.DataType.National -> context.getString(R.string.data_type_national)
        DataBytes.DataType.PromoBonusLte -> context.getString(R.string.data_type_promo_bonus_lte)
        DataBytes.DataType.PromoBonus -> context.getString(R.string.data_type_promo_bonus)
        DataBytes.DataType.DailyBag -> context.getString(R.string.data_type_daily_bag)
    }

    /**
     * "Sim 1", "Sim 2"… según la ranura de la línea.
     * */
    fun simName(sim: Sim, context: Context): String = context.getString(R.string.sim_name, sim.slotIndex + 1)
}
