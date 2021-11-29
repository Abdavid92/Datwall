package com.smartsolutions.paquetes.ui.packages

import android.app.Application
import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import com.smartsolutions.paquetes.exceptions.USSDRequestException
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.helpers.SimsHelper
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager2
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
    private val simManager: ISimManager2,
    private val dataPackageManager: IDataPackageManager,
    private val simRepository: ISimRepository,
    private val ussdHelper: USSDHelper,
    private val simsHelper: SimsHelper
) : AndroidViewModel(application) {

    private var liveSimPackageInfo = MutableLiveData<Pair<Sim, List<IDataPackage>>>()

    fun getInstalledSims(): LiveData<List<Sim>> {
        return simManager.flowInstalledSims().asLiveData(Dispatchers.IO)
    }

    fun invokeOnDefaultSim(
        context: Context,
        sim: Sim,
        simType: SimDelegate.SimType,
        fragmentManager: FragmentManager,
        onDefault: () -> Unit
    ){
        viewModelScope.launch {
            simsHelper.invokeOnDefault(context, sim, simType, fragmentManager, onDefault)
        }
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
                list.addAll(getNetworksPackages(Networks.NETWORK_3G, sim.packages))
                list.addAll(getNetworksPackages(Networks.NETWORK_NONE, sim.packages))
                list
            }
            Networks.NETWORK_3G_4G -> {
                list.addAll(getNetworksPackages(Networks.NETWORK_3G_4G, sim.packages))
                list.addAll(getNetworksPackages(Networks.NETWORK_4G, sim.packages))
                list.addAll(getNetworksPackages(Networks.NETWORK_NONE, sim.packages))
                list
            }
            Networks.NETWORK_4G -> {
                list.addAll(getNetworksPackages(sim.network, sim.packages))
                list.addAll(getNetworksPackages(Networks.NETWORK_NONE, sim.packages))
                list
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
            Networks.NETWORK_NONE -> {
                ordered.add(
                    HeaderPackagesItem(
                        name = getApplication<Application>().getString(R.string.others_title)
                    )
                )

                ordered.add(DataPackages.PACKAGES.first { it.id == DataPackages.PackageId.MessagingBag })
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

    fun sendUssdCode(ussd: String) {
        viewModelScope.launch(Dispatchers.IO) {
            ussdHelper.sendUSSDRequestLegacy(ussd, false)
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