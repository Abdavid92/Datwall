package com.smartsolutions.paquetes.ui.usage

import android.app.Application
import android.graphics.Color
import android.view.View
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.*
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.helpers.DateCalendarUtils
import com.smartsolutions.paquetes.helpers.Period
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.managers.NetworkUsageManager
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.managers.sims.SimType
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.TrafficType
import com.smartsolutions.paquetes.uiDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

@HiltViewModel
class UsageViewModel @Inject constructor(
    application: Application,
    private val networkUsageManager: NetworkUsageManager,
    private val simManager: ISimManager,
    private val appRepository: IAppRepository,
    private val dateCalendarUtils: DateCalendarUtils,
    val iconManager: IIconManager
) : AndroidViewModel(application) {

    private val liveData = MutableLiveData<Pair<Long, List<UsageApp>>>()
    private var type: TrafficType = TrafficType.International
    private var period = 0
    private var filter = UsageFilters.MAX_USAGE

    var apps = mutableListOf<UsageApp>()
    var others = mutableListOf<UsageApp>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            getApplication<Application>().uiDataStore.data.collect {
                withContext(Dispatchers.Default){
                    delay(300)
                    period = it[PreferencesKeys.USAGE_PERIOD] ?: 0
                    filter = UsageFilters.valueOf(it[PreferencesKeys.USAGE_FILTER] ?: UsageFilters.MAX_USAGE.name)
                    liveData.postValue(getTraffic(period, filter))
                }
            }
        }
    }

    fun setUsagePeriod(@Period period: Int){
        viewModelScope.launch(Dispatchers.IO) {
            getApplication<Application>().uiDataStore.edit {
                it[PreferencesKeys.USAGE_PERIOD] = period
            }
        }
    }

    fun setUsageFilter(filter: UsageFilters) {
        viewModelScope.launch(Dispatchers.IO) {
            getApplication<Application>().uiDataStore.edit {
                it[PreferencesKeys.USAGE_FILTER] = filter.name
            }
        }
    }

    fun refreshData(){
        viewModelScope.launch {
            liveData.postValue(getTraffic(period, filter))
        }
    }


    fun getUsage(type: TrafficType): LiveData<Pair<Long, List<UsageApp>>>{
        this.type = type
       return liveData
    }

    fun processAndFillPieCharData(total: Long, apps: List<UsageApp>, pieChart: PieChart) {
        viewModelScope.launch {
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
                pieChart.visibility = View.VISIBLE
            }
        }
    }


    private suspend fun getTraffic(@Period period: Int, filter: UsageFilters): Pair<Long, List<UsageApp>> {
        val interval = dateCalendarUtils.getTimePeriod(period)
        val traffics = networkUsageManager.getAppsUsage(interval.first, interval.second)
        var total = 0L

        val uids = mutableListOf<Int>()

        traffics.forEach {
            uids.add(it.uid)
        }

        val apps = withContext(Dispatchers.IO) {
            appRepository.get(uids.toIntArray())
        }.filter { it.trafficType == type }.toMutableList()

        traffics.forEach { traffic ->
            val toRemove = apps.filter { it.uid == traffic.uid }
            if (toRemove.size > 1) {
                apps.removeAll(toRemove.subList(1, toRemove.size - 1))
            }
        }

        val simId = simManager.getDefaultSimBoth(SimType.DATA)
            ?.id ?: "unknown"

        apps.forEach { app ->
            app.traffic = traffics.firstOrNull { it.uid == app.uid }
            total += app.traffic?.totalBytes?.bytes ?: 0

            if (app.traffic == null) {
                app.traffic = Traffic(app.uid, 0, 0, simId)
            }
        }

        val appsShorted = when (filter) {
            UsageFilters.ALPHABETICAL -> {
                apps.sortedBy { it.name }
            }
            UsageFilters.MAX_USAGE -> {
                apps.sortedByDescending { it.traffic!!.totalBytes.bytes }
            }
            UsageFilters.MIN_USAGE -> {
                apps.sortedBy { it.traffic!!.totalBytes.bytes }
            }
        }

        return Pair(
            total,
            transformAppsToUsageApps(
                appsShorted,
                getRandomsColors(apps.size)
            )
        )
    }


    private fun filterByTrafficApps(total: Long, list: List<UsageApp>): Pair<List<UsageApp>, List<UsageApp>> {
        val onePercent = total / 100

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

    private fun getRandomsColors(size: Int): MutableList<Int> {
        val colors = mutableListOf<Int>()
        val rnd = Random()

        for (i in 1..size) {
            colors.add(Color.argb(
                    255,
                rnd.nextInt(256),
                rnd.nextInt(256),
                rnd.nextInt(256)
                )
            )
        }

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

    enum class UsageFilters {
        ALPHABETICAL,
        MAX_USAGE,
        MIN_USAGE
    }
}