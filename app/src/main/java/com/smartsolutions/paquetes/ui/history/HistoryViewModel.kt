package com.smartsolutions.paquetes.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.helpers.DateCalendarUtils.Companion.isSameMonth
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.IPurchasedPackagesManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager2
import com.smartsolutions.paquetes.repositories.models.IPurchasedPackage
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val simManager: ISimManager2,
    private val purchasePackageManager: IPurchasedPackagesManager
) : ViewModel() {


    fun getInstalledSims(): LiveData<List<Sim>> {
        return simManager.flowInstalledSims().asLiveData(Dispatchers.IO)
    }


    fun getPurchasedPackages(simId: String): LiveData<List<IPurchasedPackage>> {
        return purchasePackageManager.getHistory().map { list ->
            return@map orderListPurchasedPackages(list.filter { it.simId == simId && !it.pending }
                .sortedByDescending { it.date })
        }.asLiveData(Dispatchers.IO)
    }


    fun seedOldPurchasedPackages(){
        viewModelScope.launch {
            purchasePackageManager.seedOldPurchasedPackages()
        }
    }


    private fun orderListPurchasedPackages(list: List<PurchasedPackage>): List<IPurchasedPackage> {
        val ordered = mutableListOf<IPurchasedPackage>()

        var lastDate = 0L

        for (pos in list.indices) {
            val purchasedPackage = list[pos]

            if (!isSameMonth(purchasedPackage.date, lastDate)) {
                ordered.add(getMonthHeader(list.filter {
                    isSameMonth(
                        it.date,
                        purchasedPackage.date
                    )
                }))
            }

            ordered.add(purchasedPackage)
            lastDate = purchasedPackage.date
        }

        return ordered
    }


    private fun getMonthHeader(purchasedPackages: List<PurchasedPackage>): HeaderPurchasedPackage {
        val month = SimpleDateFormat(
            "MMMM yyyy",
            Locale.getDefault()
        ).format(Date(purchasedPackages[0].date))

        var price = 0
        purchasedPackages.forEach {
            price += it.dataPackage.price.toInt()
        }

        return HeaderPurchasedPackage(
            month = month,
            priceTotal = price,
            cuantity = purchasedPackages.size,
            date = purchasedPackages[0].date
        )
    }


    class HeaderPurchasedPackage(
        override val id: Long = 0,
        override val date: Long = 0,
        override val origin: IDataPackageManager.ConnectionMode = IDataPackageManager.ConnectionMode.USSD,
        override var simId: String = "",
        override var pending: Boolean = false,
        override val dataPackageId: DataPackages.PackageId = DataPackages.PackageId.DailyBag,
        val month: String,
        val priceTotal: Int,
        val cuantity: Int
    ) : IPurchasedPackage

}