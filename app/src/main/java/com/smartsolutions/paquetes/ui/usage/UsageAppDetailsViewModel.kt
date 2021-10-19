package com.smartsolutions.paquetes.ui.usage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.helpers.DateCalendarUtils
import com.smartsolutions.paquetes.managers.NetworkUsageManager
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.settingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class UsageAppDetailsViewModel @Inject constructor(
    application: Application,
    private val dateCalendarUtils: DateCalendarUtils,
    private val networkUsageManager: NetworkUsageManager
): AndroidViewModel(application) {

    private val liveData = MutableLiveData<Pair<List<Traffic>, DateCalendarUtils.MyTimeUnit>>()
    private var uid = 0



    fun getUsageByTime(uid: Int): LiveData<Pair<List<Traffic>, DateCalendarUtils.MyTimeUnit>> {
        this.uid = uid
        obtainTraffic()
        return liveData
    }

    private fun obtainTraffic() {
        viewModelScope.launch {
            withContext(Dispatchers.IO){
                getApplication<Application>().settingsDataStore.data.firstOrNull()?.get(PreferencesKeys.USAGE_PERIOD)
            }?.let { period ->
                val interval = dateCalendarUtils.getTimePeriod(period)

                val timeUnit = when (period) {
                    DateCalendarUtils.PERIOD_TODAY,
                    DateCalendarUtils.PERIOD_YESTERDAY -> DateCalendarUtils.MyTimeUnit.HOUR
                    DateCalendarUtils.PERIOD_WEEK,
                    DateCalendarUtils.PERIOD_MONTH,
                    DateCalendarUtils.PERIOD_PACKAGE -> DateCalendarUtils.MyTimeUnit.DAY
                    DateCalendarUtils.PERIOD_YEAR -> DateCalendarUtils.MyTimeUnit.MONTH
                    else -> DateCalendarUtils.MyTimeUnit.HOUR
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
            liveData.postValue(Pair(emptyList(), DateCalendarUtils.MyTimeUnit.HOUR))
        }
    }





}