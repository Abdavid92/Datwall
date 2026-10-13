package com.smartsolutions.paquetes.helpers

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.TabItemBinding
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.managers.models.DataUnitBytes.Companion.GB
import com.smartsolutions.paquetes.managers.models.DataUnitBytes.Companion.KB
import com.smartsolutions.paquetes.managers.models.DataUnitBytes.Companion.MB
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.Sim

/**
 * Contruye el código ussd para comprar un
 * paquete de datos.
 *
 * @param index - Índice en donde esta el tipo de paquete (si es 3G o 4G).
 * @param dataPackageIndex - Índice en donde está el paquete. Si este parámetro
 * es -1 se considera que se está construyendo el código ussd del Plan Diario
 * y por lo tanto el resultado será diferente.
 * */
fun buildDataPackageUssdCode(index: Int, dataPackageIndex: Int): String {
    return if (dataPackageIndex != -1)
        "*133*1*$index*$dataPackageIndex#"
    else
        "*133*1*$index#"
}

/**
 * Crea un id para un DataPackage.
 * */
fun createDataPackageId(name: String, price: Float): String {
    return name.trim() + price.toString()
}

/**
 * Convierte un DataValue en bytes.
 *
 * @param dataValue - DataValue a convertir.
 * */
fun convertToBytes(dataValue: DataUnitBytes.DataValue): Long {
    return when (dataValue.dataUnit) {
        DataUnitBytes.DataUnit.GB -> (dataValue.value * GB).toLong()
        DataUnitBytes.DataUnit.MB -> (dataValue.value * MB).toLong()
        DataUnitBytes.DataUnit.KB -> (dataValue.value * KB).toLong()
        DataUnitBytes.DataUnit.B -> dataValue.value.toLong()
    }
}

/**
 * Método de extensión que itera por todos los
 * items del arreglo y los concatena en un String.
 * */
fun Array<CharSequence>.string(): String {
    var text = ""

    this.forEach {
        text += it
    }

    return text
}

/**
 * Delegado que busca una vista por el id.
 * Las vistas instanciadas con este delegado se deben usar después de
 * llamar al método super.onCreate() e inflar la vista root de la
 * actividad.
 *
 * @param resId - Id de la vista.
 *
 * @return Instancia de [View]
 * */
fun <T : View> Activity.findView(@IdRes resId: Int) = lazy {
    findViewById<T>(resId)
}

/**
 * Delegado que busca una vista por el id.
 * Las vistas instanciadas con este delegado se deben usar después de
 * llamar al método onCreateView.
 *
 * @param resId - Id de la vista.
 *
 * @return Instancia de [View]
 * */
fun <T : View> Fragment.findView(@IdRes resId: Int) = lazy {
    view?.findViewById<T>(resId)
}

fun setTabLayoutMediatorSims(context: Context, tabLayout: TabLayout, pager2: ViewPager2, sims: List<Sim>) {
    try {
        TabLayoutMediator(tabLayout, pager2) { tab, pos ->
            val tabBind =
                TabItemBinding.inflate(LayoutInflater.from(context), null, false)
            val sim = sims[pos]

            sim.icon?.let {
                tabBind.icon.setImageBitmap(it)
            }

            tabBind.title.text = sim.name()

            tab.customView = tabBind.root

            tab.view.setOnLongClickListener {
                true
            }
        }.attach()
    } catch (e: Exception) {
    }
}

/**
 * Obtiene el nombre legible de los [DataBytes.DataType]
 */
fun getDataTypeName(type: DataBytes.DataType, context: Context): String {
    return when (type) {
        DataBytes.DataType.MessagingBag -> context.getString(R.string.data_type_messaging_bag)
        DataBytes.DataType.International -> context.getString(R.string.data_type_international)
        DataBytes.DataType.InternationalLte -> context.getString(R.string.data_type_international_lte)
        DataBytes.DataType.National -> context.getString(R.string.data_type_national)
        DataBytes.DataType.PromoBonusLte -> context.getString(R.string.data_type_promo_bonus_lte)
        DataBytes.DataType.PromoBonus -> context.getString(R.string.data_type_promo_bonus)
        DataBytes.DataType.DailyBag -> context.getString(R.string.data_type_daily_bag)
    }
}
