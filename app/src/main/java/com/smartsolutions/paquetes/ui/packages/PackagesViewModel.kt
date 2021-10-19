package com.smartsolutions.paquetes.ui.packages

import android.app.Application
import androidx.lifecycle.*
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import com.smartsolutions.paquetes.exceptions.USSDRequestException
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.repositories.contracts.ISimRepository
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.IDataPackage
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PackagesViewModel @Inject constructor(
    application: Application,
    private val simManager: ISimManager,
    private val dataPackageManager: IDataPackageManager,
    private val simRepository: ISimRepository
) : AndroidViewModel(application) {

    private var liveSimPackageInfo = MutableLiveData<Pair<Sim, List<IDataPackage>>>()

    fun getInstalledSims(): LiveData<List<Sim>> {
        return simManager.flowInstalledSims().asLiveData(Dispatchers.IO)
    }


    fun getSimAndPackages(simID: String): LiveData<Pair<Sim, List<IDataPackage>>>{
        viewModelScope.launch(Dispatchers.IO) {
            simRepository.get(simID, true)?.let {
                liveSimPackageInfo.postValue(it to prepareListPackages(it))
            }
        }
        return liveSimPackageInfo
    }


    fun prepareListPackages(sim: Sim): List<IDataPackage> {
        val list = mutableListOf<IDataPackage>()
        return when (sim.network) {
            Networks.NETWORK_3G -> {
                getNetworksPackages(Networks.NETWORK_3G, sim.packages)
            }
            Networks.NETWORK_3G_4G -> {
                list.addAll(getNetworksPackages(Networks.NETWORK_3G_4G, sim.packages))
                list.addAll(getNetworksPackages(Networks.NETWORK_4G, sim.packages))
                list
            }
            Networks.NETWORK_4G -> {
                getNetworksPackages(sim.network, sim.packages)
            }
            else -> emptyList()
        }
    }


    private fun getNetworksPackages(
        @Networks networks: String,
        list: List<DataPackage>
    ): List<IDataPackage> {
        val ordered = mutableListOf<IDataPackage>()

        when(networks) {
            Networks.NETWORK_3G, Networks.NETWORK_3G_4G -> {
                ordered.add(
                    HeaderPackagesItem(
                        name = getApplication<Application>().getString(R.string.plan_title)
                    )
                )

                ordered.addAll(list.filter { it.network == Networks.NETWORK_3G_4G || it.network == Networks.NETWORK_3G })
            }
            Networks.NETWORK_4G -> {
                ordered.add(
                    HeaderPackagesItem(
                        name = getApplication<Application>().getString(R.string.packages_title)
                    )
                )

                ordered.addAll(list.filter { it.network == Networks.NETWORK_4G })
            }
        }

        return ordered
    }


    fun purchasePackage(sim: Sim, dataPackage: DataPackage, callback: PurchaseResult){
        viewModelScope.launch {
            try {
                dataPackageManager.buyDataPackage(dataPackage, sim)
                withContext(Dispatchers.Main){
                    callback.onSuccess()
                }
            }catch (e: Exception){
                when (e) {
                    is USSDRequestException, is MissingPermissionException -> {
                        withContext(Dispatchers.Main){
                            callback.onMissingPermission()
                        }
                    }
                    else -> {
                        withContext(Dispatchers.Main){
                            callback.onFailed()
                        }
                    }
                }
            }
        }
    }

    class HeaderPackagesItem(
        override val id: DataPackages.PackageId = DataPackages.PackageId.DailyBag,
        override val name: String,
        override val description: String = "",
        override val price: Float = 0f,
        override val bytes: Long = 0,
        override val bytesLte: Long = 0,
        override val nationalBytes: Long = 0,
        override val network: String = Networks.NETWORK_NONE,
        override val index: Int = 0,
        override val duration: Int = 0,
        override val smsKey: String = "",
        override var deprecated: Boolean = false,
        override val minutes: Int = 0,
        override val sms: Int = 0
    ) : IDataPackage

    interface PurchaseResult {
        fun onSuccess()
        fun onFailed()
        fun onMissingPermission()
    }

}