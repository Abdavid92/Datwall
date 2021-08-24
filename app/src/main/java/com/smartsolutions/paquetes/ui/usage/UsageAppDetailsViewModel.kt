package com.smartsolutions.paquetes.ui.usage

import android.app.Application
import androidx.datastore.dataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.helpers.NetworkUsageUtils
import com.smartsolutions.paquetes.helpers.Period
import com.smartsolutions.paquetes.managers.NetworkUsageManager
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.uiDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UsageAppDetailsViewModel @Inject constructor(
    application: Application,
    private val networkUsageUtils: NetworkUsageUtils,
    private val networkUsageManager: NetworkUsageManager
): AndroidViewModel(application) {

    private val liveData = MutableLiveData<Pair<List<Traffic>, NetworkUsageUtils.TimeUnit>>()
    private var uid = 0



    fun getUsageByTime(uid: Int): LiveData<Pair<List<Traffic>, NetworkUsageUtils.TimeUnit>> {
        this.uid = uid
        obtainTraffic()
        return liveData
    }

    private fun obtainTraffic() {
        viewModelScope.launch(Dispatchers.IO) {
            getApplication<Application>().uiDataStore.data.firstOrNull()?.get(PreferencesKeys.USAGE_PERIOD)?.let { period ->
                val interval = networkUsageUtils.getTimePeriod(period)

                val timeUnit = when (period) {
                    NetworkUsageUtils.PERIOD_TODAY,
                    NetworkUsageUtils.PERIOD_YESTERDAY -> NetworkUsageUtils.TimeUnit.HOUR
                    NetworkUsageUtils.PERIOD_WEEK,
                    NetworkUsageUtils.PERIOD_MONTH,
                    NetworkUsageUtils.PERIOD_PACKAGE -> NetworkUsageUtils.TimeUnit.DAY
                    NetworkUsageUtils.PERIOD_YEAR -> NetworkUsageUtils.TimeUnit.MONTH
                    else -> NetworkUsageUtils.TimeUnit.HOUR
                }

                val traffics = networkUsageManager.getAppUsageByLapsusTime(
                    uid,
                    interval.first,
                    interval.second,
                    timeUnit
                )

                liveData.postValue(Pair(traffics, timeUnit))
                return@launch
            }
            liveData.postValue(Pair(emptyList(), NetworkUsageUtils.TimeUnit.HOUR))
        }
    }





}