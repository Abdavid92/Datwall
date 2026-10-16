package com.smartsolutions.paquetes.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.managers.HistorySummary
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.IPurchasedPackagesManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.repositories.models.IPurchasedPackage
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val simManager: ISimManager,
    private val purchasePackageManager: IPurchasedPackagesManager
) : ViewModel() {


    fun getInstalledSims(): LiveData<List<Sim>> {
        return simManager.flowInstalledSims().asLiveData(Dispatchers.IO)
    }


    fun getPurchasedPackages(simId: String): LiveData<List<IPurchasedPackage>> {
        return purchasePackageManager.getHistory().map { list ->
            HistorySummary.months(list, simId).flatMap { month ->
                listOf<IPurchasedPackage>(getMonthHeader(month)) + month.purchases
            }
        }.asLiveData(Dispatchers.IO)
    }


    private fun getMonthHeader(month: HistorySummary.Month): HeaderPurchasedPackage {
        return HeaderPurchasedPackage(
            month = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(month.date)),
            priceTotal = month.totalPrice,
            cuantity = month.purchases.size,
            date = month.date
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