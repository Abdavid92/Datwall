package com.smartsolutions.paquetes.ui.usage

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.*
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.helpers.NetworkUsageUtils
import com.smartsolutions.paquetes.helpers.Period
import com.smartsolutions.paquetes.managers.NetworkUsageManager
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.TrafficType
import com.smartsolutions.paquetes.uiDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UsageViewModel @Inject constructor(
    application: Application,
    private val networkUsageManager: NetworkUsageManager,
    private val appRepository: IAppRepository,
    private val networkUsageUtils: NetworkUsageUtils
) : AndroidViewModel(application) {

    init {
        viewModelScope.launch(Dispatchers.IO) {
            getApplication<Application>().uiDataStore.data.collect {
                val period = it[PreferencesKeys.USAGE_PERIOD] ?: 0
                liveData.postValue(getTraffic(period))
            }
        }
    }

    private val liveData = MutableLiveData<Pair<Long, List<App>>>()
    private var type: TrafficType = TrafficType.International

    fun setUsagePeriod(@Period period: Int){
        viewModelScope.launch {
            getApplication<Application>().uiDataStore.edit {
                it[PreferencesKeys.USAGE_PERIOD] = period
            }
        }
    }


    fun getUsage(type: TrafficType): LiveData<Pair<Long, List<App>>>{
        this.type = type
       return liveData
    }


    private suspend fun getTraffic(@Period period: Int): Pair<Long, List<App>>{
        val interval = networkUsageUtils.getTimePeriod(period)
        val traffics = networkUsageManager.getAppsUsage(interval.first, interval.second)

        val uids = mutableListOf<Int>()
        traffics.forEach {
            uids.add(it.uid)
        }

        val apps = appRepository.get(uids.toIntArray()).filter { it.trafficType == type }

        var total = 0L
        apps.forEach { app ->
            app.traffic = traffics.firstOrNull { it.uid == app.uid }
            app.trafficType
            total += app.traffic?.totalBytes?.bytes ?: 0L
        }

        return Pair(total, apps.sortedByDescending { it.traffic!!.totalBytes.bytes })
    }


    suspend fun processTrafficChart(list: List<App>): List<Pair<String, Long>> {
        val data = mutableListOf<Pair<String, Long>>()

        if (list.size > 8) {
            list.subList(0, 7).forEach {
                data.add(Pair(it.name, it.traffic!!.totalBytes.bytes))
            }

            var total = 0L
            list.subList(8, list.size - 1).forEach {
                total += it.traffic!!.totalBytes.bytes
            }

            data.add(Pair("Otras", total))
        } else {
            list.forEach {
                data.add(Pair(it.name.split(" ")[0], it.traffic!!.totalBytes.bytes))
            }
        }
        return data
    }

}