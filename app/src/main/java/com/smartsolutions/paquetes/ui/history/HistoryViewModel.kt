package com.smartsolutions.paquetes.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.helpers.NetworkUsageUtils.Companion.isSameMonth
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.IPurchasedPackagesManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.repositories.models.IPurchasedPackage
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import org.apache.commons.lang.time.DateUtils
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
            return@map orderListPurchasedPackages(list.filter { it.simId == simId && !it.pending }
                .sortedByDescending { it.date })
        }.asLiveData(Dispatchers.IO)
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

        //TODO REMOVE FAKE DATA
        return addFakeData()
    }



    private fun addFakeData(): List<IPurchasedPackage>{
        val list = mutableListOf<IPurchasedPackage>()

        list.add(HeaderPurchasedPackage(
            month = "Junio 2021",
            date =   DateUtils.addMonths(Date(), 0).time,
            cuantity = 3,
            priceTotal = 5463
        ))

        list.add(PurchasedPackage(
            0,
            System.currentTimeMillis(),
            IDataPackageManager.ConnectionMode.USSD,
            "",
            false,
            DataPackages.PACKAGES[0].id
        ).apply {
            dataPackage =DataPackages.PACKAGES[3]
        })

        list.add(PurchasedPackage(
            0,
            System.currentTimeMillis() - 745635L,
            IDataPackageManager.ConnectionMode.USSD,
            "",
            false,
            DataPackages.PACKAGES[0].id
        ).apply {
            dataPackage =DataPackages.PACKAGES[2]
        })

        list.add(PurchasedPackage(
            0,
            System.currentTimeMillis() - 86975L,
            IDataPackageManager.ConnectionMode.USSD,
            "",
            false,
            DataPackages.PACKAGES[0].id
        ).apply {
            dataPackage =DataPackages.PACKAGES[5]
        })

        list.add(HeaderPurchasedPackage(
            month = "julio 2021",
            date =  DateUtils.addMonths(Date(), -1).time,
            cuantity = 2,
            priceTotal = 5463
        ))

        list.add(PurchasedPackage(
            0,
            System.currentTimeMillis(),
            IDataPackageManager.ConnectionMode.USSD,
            "",
            false,
            DataPackages.PACKAGES[0].id
        ).apply {
            dataPackage =DataPackages.PACKAGES[3]
        })

        list.add(PurchasedPackage(
            0,
            System.currentTimeMillis() - 745635L,
            IDataPackageManager.ConnectionMode.USSD,
            "",
            false,
            DataPackages.PACKAGES[0].id
        ).apply {
            dataPackage =DataPackages.PACKAGES[2]
        })

        list.add(PurchasedPackage(
            0,
            System.currentTimeMillis() - 86975L,
            IDataPackageManager.ConnectionMode.USSD,
            "",
            false,
            DataPackages.PACKAGES[0].id
        ).apply {
            dataPackage =DataPackages.PACKAGES[5]
        })

        list.add(HeaderPurchasedPackage(
            month = "Junio 2021",
            date =  DateUtils.addMonths(Date(), -2).time,
            cuantity = 5,
            priceTotal = 5463
        ))

        list.add(PurchasedPackage(
            0,
            System.currentTimeMillis(),
            IDataPackageManager.ConnectionMode.USSD,
            "",
            false,
            DataPackages.PACKAGES[0].id
        ).apply {
            dataPackage =DataPackages.PACKAGES[3]
        })

        list.add(PurchasedPackage(
            0,
            System.currentTimeMillis() - 745635L,
            IDataPackageManager.ConnectionMode.USSD,
            "",
            false,
            DataPackages.PACKAGES[0].id
        ).apply {
            dataPackage =DataPackages.PACKAGES[2]
        })

        list.add(PurchasedPackage(
            0,
            System.currentTimeMillis() - 86975L,
            IDataPackageManager.ConnectionMode.USSD,
            "",
            false,
            DataPackages.PACKAGES[0].id
        ).apply {
            dataPackage =DataPackages.PACKAGES[5]
        })

        list.add(HeaderPurchasedPackage(
            month = "julio 2021",
            date =  DateUtils.addMonths(Date(), -3).time,
            cuantity = 4,
            priceTotal = 5463
        ))

        list.add(PurchasedPackage(
            0,
            System.currentTimeMillis(),
            IDataPackageManager.ConnectionMode.USSD,
            "",
            false,
            DataPackages.PACKAGES[0].id
        ).apply {
            dataPackage =DataPackages.PACKAGES[3]
        })

        list.add(PurchasedPackage(
            0,
            System.currentTimeMillis() - 745635L,
            IDataPackageManager.ConnectionMode.USSD,
            "",
            false,
            DataPackages.PACKAGES[0].id
        ).apply {
            dataPackage =DataPackages.PACKAGES[2]
        })

        list.add(PurchasedPackage(
            0,
            System.currentTimeMillis() - 86975L,
            IDataPackageManager.ConnectionMode.USSD,
            "",
            false,
            DataPackages.PACKAGES[0].id
        ).apply {
            dataPackage =DataPackages.PACKAGES[5]
        })

        return list
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