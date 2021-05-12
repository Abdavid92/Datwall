package com.smartsolutions.paquetes.managers

import android.content.Context
import android.provider.ContactsContract
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.data.DataPackagesContract
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.helpers.convertToBytes
import com.smartsolutions.paquetes.managers.models.DataBytes
import com.smartsolutions.paquetes.repositories.AppRepository
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

class StatisticsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDataBytesRepository: IUserDataBytesRepository,
    private val networkUsageManager: NetworkUsageManager,
    private val appRepository: AppRepository
) {

    private var enabledLTE : Boolean = false

    init {
        GlobalScope.launch(Dispatchers.Main) {
            context.dataStore.data.collect {
                enabledLTE = it[PreferencesKeys.ENABLED_NETWORK_LTE] == true
            }
        }
    }


    suspend fun remainingBagDaily(simIndex: Int): DataBytes {
        val bagDaily = userDataBytesRepository.byType(UserDataBytes.DataType.BagDaily, simIndex)

        if (!bagDaily.isExpired() && bagDaily.exists()) {
            if (!enabledLTE)
                return DataBytes(bagDaily.bytesLte)

            var consumed = networkUsageManager.getUsageTotal(
                bagDaily.startTime,
                finishTime
            ).totalBytes.bytes

            val remaining = bagDaily.bytesLte - consumed
            if (remaining > 0)
                return DataBytes(remaining)
        }

        return DataBytes(0L)
    }

    suspend fun remainingNational(simIndex: Int): DataBytes {
        val national = userDataBytesRepository.byType(UserDataBytes.DataType.National, simIndex)

        if (!national.isExpired() && national.exists()) {
            val appsNational = mutableListOf<App>() //TODO aun no esta el metodo de pedir del repositorio por nacionales nada mas
            networkUsageManager.fillAppsUsage(appsNational, national.startTime, finishTime)
            var consumed = 0L
            appsNational.forEach{
                consumed += it.traffic!!.totalBytes.bytes
            }
            val remaining = national.bytes - consumed
            if (remaining > 0){
                return DataBytes(remaining)
            }
        }

        return DataBytes(0L)
    }


    suspend fun remainingBonus(simIndex: Int): DataBytes {
        val bonus = userDataBytesRepository.byType(UserDataBytes.DataType.Bonus, simIndex)

        if (!bonus.isExpired() && bonus.exists()){
            if (!enabledLTE)
                return DataBytes(bonus.bytesLte)

            val bagDaily = userDataBytesRepository.byType(UserDataBytes.DataType.BagDaily, simIndex)

            var consumed = networkUsageManager.getUsageTotal(bonus.startTime, finishTime).totalBytes.bytes
            var consumedBagDaily = 0L

            if (bagDaily.exists()){
                if (bagDaily.startTime >= bonus.startTime){
                    consumedBagDaily = networkUsageManager.getUsageTotal(bagDaily.startTime, finishTime).totalBytes.bytes
                    if (consumedBagDaily > convertToBytes(DataPackagesContract.DailyBag.bytesLte)){
                        consumed -= convertToBytes(DataPackagesContract.DailyBag.bytesLte)
                    }else {
                        consumed -= consumedBagDaily
                    }
                }else {
                    consumedBagDaily = networkUsageManager.getUsageTotal(bagDaily.startTime, bonus.startTime).totalBytes.bytes
                    val remainingBagDaily = convertToBytes(DataPackagesContract.DailyBag.bytesLte) - consumedBagDaily
                    if (remainingBagDaily > 0 && consumed > remainingBagDaily){
                        consumed -= remainingBagDaily
                    }
                }
            }

            val remaining = bonus.bytesLte - consumed
            if (remaining > 0){
                return DataBytes(remaining)
            }
        }
        return DataBytes(0L)
    }


    suspend fun remainingPromoBonus(simIndex: Int): DataBytes {
       val promoBonus = userDataBytesRepository.byType(UserDataBytes.DataType.PromoBonus, simIndex)

        if (!promoBonus.isExpired() && promoBonus.exists()){
            var consumed = networkUsageManager.getUsageTotal(promoBonus.startTime, finishTime).totalBytes.bytes

            if (enabledLTE) {

                val bagDaily =
                    userDataBytesRepository.byType(UserDataBytes.DataType.BagDaily, simIndex)

                if (bagDaily.exists()) {
                    if (bagDaily.startTime >= promoBonus.startTime) {
                        val consumedBagDaily = networkUsageManager.getUsageTotal(bagDaily.startTime, finishTime).totalBytes.bytes

                        if (consumedBagDaily > convertToBytes(DataPackagesContract.DailyBag.bytesLte)){
                            consumed -= convertToBytes(DataPackagesContract.DailyBag.bytesLte)
                        }else {
                            consumed -= consumedBagDaily
                        }
                    }else {
                        val consumedBagDailyBefore = networkUsageManager.getUsageTotal(bagDaily.startTime, promoBonus.startTime).totalBytes.bytes
                        //...... Aqui quede morido.....
                    }
                }



            }


        }

        return DataBytes(0L)
    }



    suspend fun remainingInternational(simIndex: Int): DataBytes {
        val international = userDataBytesRepository.byType(UserDataBytes.DataType.International, simIndex)

        if (!international.isExpired() && !international.exists()) {
            var consumed = networkUsageManager.getUsageTotal(international.startTime, finishTime)

            val bagDaily = userDataBytesRepository.byType(UserDataBytes.DataType.BagDaily, simIndex)

            if (bagDaily.exists()) {
                if (bagDaily.startTime >= international.startTime) {

                }
            }
        }

        return DataBytes(0L)
    }


    companion object {
        var finishTime = System.currentTimeMillis()
    }
}