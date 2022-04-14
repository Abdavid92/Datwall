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
 * es -1 se considera que se está construyendo el código ussd de la bolsa diaria
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
 * Lee la cantidad de bytes de un texto en el que se exprese de
 * la siguiente manera: '1.24 MB' o '1.5 GB'.
 *
 * @param key - Texto que se encuentra justo antes de la cantidad de bytes.
 * @param text - Texto completo donde buscar.
 *
 * @return La cantidad encontrada expresada en bytes o -1 si no es posible
 * obtener la cantidad.
 * */
fun getBytesFromText(key: String, text: String): Long {
    if (!text.contains(key))
        return -1

    val start = text.indexOf(key) + key.length
    var unit: DataUnitBytes.DataUnit = DataUnitBytes.DataUnit.B

    var index = start

    while (index < text.length) {

        when (text[index].uppercaseChar()){
            'B' -> {
                unit = DataUnitBytes.DataUnit.B
                break
            }
            'K' -> {
                unit = DataUnitBytes.DataUnit.KB
                break
            }
            'M' -> {
                unit = DataUnitBytes.DataUnit.MB
                break
            }
            'G' -> {
                unit = DataUnitBytes.DataUnit.GB
                break
            }
        }
        index++
    }

    when (text[index].uppercaseChar()) {
        'K', 'M', 'G' -> {
            if (text.length < index + 1 || !text[index + 1].uppercaseChar().equals('B', true))
                return -1
        }
        'B' -> {
            if (text.length > index + 1 && text[index + 1].isLetter())
                return -1
        }
    }

    return try {
        val value = text.substring(start, index).trimStart().trimEnd().toFloat()
        when (unit) {
            DataUnitBytes.DataUnit.KB -> (value * KB).toLong()
            DataUnitBytes.DataUnit.MB -> (value * MB).toLong()
            DataUnitBytes.DataUnit.GB -> (value * GB).toLong()
            else -> value.toLong()
        }
    } catch (e: Exception) {
        -1
    }
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

fun setTabLayoutMediatorSims(context: Context, tabLayout: TabLayout, pager2: ViewPager2, sims: List<Sim>, fragmentManager: FragmentManager) {
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


fun getDataTypeName(type: DataBytes.DataType, context: Context): String {
    return when (type){
        DataBytes.DataType.MessagingBag -> context.getString(R.string.data_type_messaging_bag)
        DataBytes.DataType.International -> context.getString(R.string.data_type_international)
        DataBytes.DataType.InternationalLte -> context.getString(R.string.data_type_international_lte)
        DataBytes.DataType.National -> context.getString(R.string.data_type_national)
        DataBytes.DataType.PromoBonusLte -> context.getString(R.string.data_type_promo_bonus_lte)
        DataBytes.DataType.PromoBonus -> context.getString(R.string.data_type_promo_bonus)
        DataBytes.DataType.DailyBag -> context.getString(R.string.data_type_daily_bag)
    }
}
