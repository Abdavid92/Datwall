package com.smartsolutions.paquetes.ui.usage

import android.app.Application
import android.graphics.Color
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.*
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.helpers.NetworkUsageUtils
import com.smartsolutions.paquetes.helpers.Period
import com.smartsolutions.paquetes.managers.NetworkUsageManager
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.TrafficType
import com.smartsolutions.paquetes.uiDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

@HiltViewModel
class UsageViewModel @Inject constructor(
    application: Application,
    private val networkUsageManager: NetworkUsageManager,
    private val appRepository: IAppRepository,
    private val networkUsageUtils: NetworkUsageUtils,
    val iconManager: IIconManager
) : AndroidViewModel(application) {

    private var period = 0
    var apps = mutableListOf<UsageApp>()
    var others = mutableListOf<UsageApp>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            getApplication<Application>().uiDataStore.data.collect {
                period = it[PreferencesKeys.USAGE_PERIOD] ?: 0
                liveData.postValue(getTraffic(period))
            }
        }
    }

    private val liveData = MutableLiveData<Pair<Long, List<UsageApp>>>()
    private var type: TrafficType = TrafficType.International

    fun setUsagePeriod(@Period period: Int){
        viewModelScope.launch(Dispatchers.IO) {
            getApplication<Application>().uiDataStore.edit {
                it[PreferencesKeys.USAGE_PERIOD] = period
            }
        }
    }

    fun refreshData(){
        viewModelScope.launch(Dispatchers.IO) {
            liveData.postValue(getTraffic(period))
        }
    }


    fun getUsage(type: TrafficType): LiveData<Pair<Long, List<UsageApp>>>{
        this.type = type
       return liveData
    }


    private suspend fun getTraffic(@Period period: Int): Pair<Long, List<UsageApp>> {
        val interval = networkUsageUtils.getTimePeriod(period)
        val traffics = networkUsageManager.getAppsUsage(interval.first, interval.second)
        var total = 0L

        val uids = mutableListOf<Int>()
        traffics.forEach {
            uids.add(it.uid)
        }

        val apps =
            appRepository.get(uids.toIntArray()).filter { it.trafficType == type }.toMutableList()

        traffics.forEach { traffic ->
            val toRemove = apps.filter { it.uid ==  traffic.uid}
            if (toRemove.size > 1) {
                apps.removeAll(toRemove.subList(1, toRemove.size - 1))
            }
        }

        apps.forEach { app ->
            app.traffic = traffics.firstOrNull { it.uid == app.uid }
            total += app.traffic?.totalBytes?.bytes ?: 0
        }

        return Pair(
            total,
            transformAppsToUsageApps(
                apps.sortedByDescending { it.traffic!!.totalBytes.bytes },
                getRandomsColors(apps.size)
            )
        )
    }



    private fun filterByTrafficApps(total: Long, list: List<UsageApp>): Pair<List<UsageApp>, List<UsageApp>> {
        val onePercent = total/100

        apps.clear()
        others.clear()

        list.forEach {
            if (it.app.traffic!!.totalBytes.bytes > onePercent){
                apps.add(it)
            }else {
                others.add(it)
            }
        }

        return Pair(apps, others)
    }


    fun processAndFillPieCharData(total: Long, apps: List<UsageApp>, pieChart: PieChart) {
        viewModelScope.launch(Dispatchers.IO) {
            val entries = mutableListOf<PieEntry>()

            val filtered = filterByTrafficApps(total, apps)
            val colours = mutableListOf<Int>()

            filtered.first.forEach {
                entries.add(
                    PieEntry(
                        it.app.traffic!!.totalBytes.bytes.toFloat(),
                        it.app.name
                    )
                )
                colours.add(it.colour)
            }

            if (filtered.second.isNotEmpty()) {
                entries.add(
                    PieEntry(
                        filtered.second[0].app.traffic!!.totalBytes.bytes.toFloat(),
                        UsageHolderFragment.OTHERS_LABEL
                    )
                )
                val colour = Color.GRAY
                filtered.second.forEach {
                    it.colour = colour
                }
                colours.add(colour)
            }

            val pieData = PieData(PieDataSet(entries, "").apply {
                valueTextSize = 11f
                colors = colours
            })

            pieData.setDrawValues(false)

            withContext(Dispatchers.Main) {
                pieChart.apply {
                    data = pieData
                    animateY(1000, Easing.EaseInOutCubic)
                }.postInvalidate()
            }
        }
    }


    fun getAppDetails(app: App) {

    }


    private fun getRandomsColors(size: Int): MutableList<Int> {
        val colors = mutableListOf<Int>()
        val rnd = Random()
        var items = 0
        while (items < size) {
            colors.add(Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)))
            items++
        }

        //TODO: Puedes cambiar el while por un for
        /*for (i in 0..size) {
            colors.add(Color.argb(
                    255,
                rnd.nextInt(256),
                rnd.nextInt(256),
                rnd.nextInt(256)
                )
            )
        }*/
        return colors
    }


    private fun transformAppsToUsageApps(apps: List<App>, colours: List<Int>): List<UsageApp> {
        if (apps.size != colours.size)
            throw IllegalArgumentException()

        val list = mutableListOf<UsageApp>()
        for (pos in apps.indices) {
            list.add(UsageApp(apps[pos], colours[pos]))
        }
        return list
    }


}