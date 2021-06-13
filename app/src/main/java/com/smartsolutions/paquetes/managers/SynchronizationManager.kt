package com.smartsolutions.paquetes.managers

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.IMiCubacelManager
import com.smartsolutions.paquetes.managers.contracts.ISynchronizationManager
import com.smartsolutions.paquetes.managers.contracts.IUserDataBytesManager
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.micubacel.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.apache.commons.lang3.time.DateUtils
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.NoSuchElementException

class SynchronizationManager @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val miCubacelManager: IMiCubacelManager,
    private val userDataBytesManager: IUserDataBytesManager,
    private val ussdHelper: USSDHelper
) : ISynchronizationManager {

    private var _synchronizationMode = IDataPackageManager.ConnectionMode.USSD
    override var synchronizationMode: IDataPackageManager.ConnectionMode
        get() = _synchronizationMode
        set(value) {
            if (value == IDataPackageManager.ConnectionMode.Unknown)
                return

            GlobalScope.launch(Dispatchers.IO) {
                context.dataStore.edit {
                    it[PreferencesKeys.SYNCHRONIZATION_MODE] = value.name
                }
            }
            _synchronizationMode = value
        }

    init {
        GlobalScope.launch(Dispatchers.IO) {
            context.dataStore.data.collect {
                _synchronizationMode = IDataPackageManager.ConnectionMode
                    .valueOf(it[PreferencesKeys.SYNCHRONIZATION_MODE] ?: _synchronizationMode.name)
            }
        }
    }

    override suspend fun synchronizeUserDataBytes(sim: Sim) {
        if (_synchronizationMode == IDataPackageManager.ConnectionMode.MiCubacel) {
            if (sim.miCubacelAccount == null)
                throw NoSuchElementException()

            val data = miCubacelManager.synchronizeUserDataBytes(sim.miCubacelAccount)
                .getOrThrow()

            userDataBytesManager.synchronizeUserDataBytes(fillMissingDataBytes(data), sim.id)
        } else if (_synchronizationMode == IDataPackageManager.ConnectionMode.USSD) {
            val data = mutableListOf<DataBytes>()

            val bytesPackages = ussdHelper.sendUSSDRequest("*222*328#")
            val bonusPackages = ussdHelper.sendUSSDRequest("*222*266#")

            data.addAll(obtainDataBytesPackages(bytesPackages))
            data.addAll(obtainDataByteBonus(bonusPackages))

            userDataBytesManager.synchronizeUserDataBytes(fillMissingDataBytes(data), sim.id)
        }
    }

    override fun scheduleUserDataBytesSynchronization(intervalInMinutes: Int) {
        TODO("Not yet implemented")
    }

    private fun obtainDataBytesPackages(bytesPackages: Array<CharSequence>): Collection<DataBytes> {
        val response = joinString(bytesPackages)

        val data = mutableListOf<DataBytes>()

        if (response.contains(PAQUETES))
            data.addAll(getInternationals(response))

        val value = getValueFromText(DIARIA, response)
        if (value > 0){
            data.add(DataBytes(DataBytes.DataType.DailyBag, value, getExpireDatePackages(response, true)))
        }

        return data
    }

    private fun obtainDataByteBonus(bonusPackages: Array<CharSequence>): Collection<DataBytes> {
        val response = joinString(bonusPackages)

        val data = mutableListOf<DataBytes>()

        var value = getValueFromText(PROMO_BONO, response)
        if (value > 0){
            data.add(DataBytes(DataBytes.DataType.PromoBonus, value, getExpireDateBonus(PROMO_BONO, response)))
        }

        value = getValueFromText(BONO, response)
        if (value > 0){
            data.add(DataBytes(DataBytes.DataType.Bonus, value, getExpireDateBonus(BONO, response)))
        }

        value = getValueFromText(NATIONAL, response)
        if (value > 0){
            data.add(DataBytes(DataBytes.DataType.National, value, getExpireDateBonus(NATIONAL, response)))
        }

        return data
    }


    private fun fillMissingDataBytes(data: List<DataBytes>): List<DataBytes> {
        val list = data.toMutableList()

        DataBytes.DataType.values().forEach { type ->
            if (list.firstOrNull { it.type == type } == null) {
                list.add(DataBytes(type, 0, 0))
            }
        }
        return list
    }

    private fun getInternationals(text: String): List<DataBytes> {
        var international = getValueFromText(PAQUETES, text)
        var internationalLte = 0L

        if (text.contains("solo LTE)")){
            val start = text.indexOf(PAQUETES)
            internationalLte = getValueFromText(text.substring(start, text.indexOf("(", start) + 1), text)
            international -= internationalLte

        }else if (text.contains("solo LTE")){
            internationalLte = international
            international = 0L
        }

        return listOf(
            DataBytes(DataBytes.DataType.International, international, getExpireDatePackages(text, false)),
            DataBytes(DataBytes.DataType.InternationalLte, internationalLte, getExpireDatePackages(text, false))
        )
    }

    private fun getValueFromText(find: String, text: String): Long {
        if (!text.contains(find))
            return -1

        val start = text.indexOf(find) + find.length
        var unit: DataUnitBytes.DataUnit = DataUnitBytes.DataUnit.B

        var index = start

        while (index < text.length){
            val letter = text[index]
            when (letter.toString()){
                "B" -> {
                    unit = DataUnitBytes.DataUnit.B
                    break
                }
                "K" -> {
                    unit = DataUnitBytes.DataUnit.KB
                    break
                }
                "M" -> {
                    unit = DataUnitBytes.DataUnit.MB
                    break
                }
                "G" -> {
                    unit = DataUnitBytes.DataUnit.GB
                    break
                }
            }
            index++
        }

        return try {
            val value = text.substring(start, index).trimStart().trimEnd().toFloat()
            when (unit) {
                DataUnitBytes.DataUnit.KB -> (value * DataUnitBytes.KB).toLong()
                DataUnitBytes.DataUnit.MB -> (value * DataUnitBytes.MB).toLong()
                DataUnitBytes.DataUnit.GB -> (value * DataUnitBytes.GB).toLong()
                else -> value.toLong()
            }
        }catch (e: Exception){
            -1
        }
    }

    private fun getExpireDatePackages(text: String, isBolsa: Boolean): Long {
        if (isBolsa && !text.contains(DIARIA) || !isBolsa && !text.contains(PAQUETES)){
            return 0L
        }

        val start = if (isBolsa){
            text.indexOf("validos", text.indexOf(DIARIA)) + 7
        }else {
            text.indexOf("validos", text.indexOf(PAQUETES)) + 7
        }

        val finish = if (isBolsa){
            text.indexOf("horas", start)
        }else {
            text.indexOf("dias", start)
        }

        return try {
            val value = text.substring(start, finish).trimStart().trimEnd().toInt()
            if (isBolsa) {
                DateUtils.addHours(Date(), value).time
            }else {
                DateUtils.addDays(Date(), value).time
            }
        }catch (e: Exception){
            0L
        }
    }

    private fun getExpireDateBonus(find: String, text: String): Long {
        if (!text.contains(find)){
            return 0L
        }
        val start = text.indexOf("->", text.indexOf(find)) + 2
        val finish = text.indexOf(".", start)

        return try {
            val dateString = text.substring(start, finish).trimStart().trimEnd()
            var date = SimpleDateFormat("dd-MM-yy", Locale.US).parse(dateString)
            date = DateUtils.setHours(date, 23)
            date = DateUtils.setMinutes(date, 59)
            date = DateUtils.setSeconds(date, 59)
            date.time
        }catch (e: Exception){
            0L
        }
    }

    private fun joinString(charSequence: Array<CharSequence>): String {
        var string = ""
        charSequence.forEach {
            string += it.toString()
        }
        return string
    }

    companion object {
        const val DIARIA = "Diaria:"
        const val PAQUETES = "Paquetes:"
        const val BONO = "LTE"
        const val PROMO_BONO = "Datos"
        const val NATIONAL = "Datos.cu"
    }
}