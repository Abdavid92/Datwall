package com.smartsolutions.paquetes.ui.resume

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.helpers.DateCalendarUtils
import com.smartsolutions.paquetes.managers.UsageChartGrouping
import com.smartsolutions.paquetes.managers.UsageChartGrouping.GeneralPeriod
import com.smartsolutions.paquetes.managers.contracts.IUserDataManager
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.UsageGeneral
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UsageGeneralViewModel @Inject constructor(
    private val userDataManager: IUserDataManager,
    private val dateCalendarUtils: DateCalendarUtils
) : ViewModel() {

    private var period = GeneralPeriod.TODAY

    private var liveData = MutableLiveData<Pair<List<UsageGeneral>, DateCalendarUtils.MyTimeUnit>>()


    fun setPeriod(index: Int, simId: String, dataType: DataBytes.DataType) {
        period = GeneralPeriod.entries[index]
        getUsage(dataType, simId)
    }

    fun setDataType(simId: String, dataType: DataBytes.DataType) {
        getUsage(dataType, simId)
    }

    fun getUsageGeneral(): LiveData<Pair<List<UsageGeneral>, DateCalendarUtils.MyTimeUnit>> {
        return liveData
    }


    private fun getUsage(dataType: DataBytes.DataType, simId: String) {
        viewModelScope.launch {
            val interval = getInterval(simId, dataType)
            val timeUnit = UsageChartGrouping.generalTimeUnit(period)

            val usage = userDataManager.usage(simId, dataType, interval.first, interval.second)

            liveData.postValue(UsageChartGrouping.compact(usage, timeUnit) to timeUnit)
        }
    }

    private suspend fun getInterval(simId: String, dataType: DataBytes.DataType): Pair<Long, Long> {
        return when (period) {
            GeneralPeriod.TODAY -> dateCalendarUtils.getTimePeriod(DateCalendarUtils.PERIOD_TODAY)
            GeneralPeriod.YESTERDAY -> dateCalendarUtils.getTimePeriod(DateCalendarUtils.PERIOD_YESTERDAY)
            GeneralPeriod.WEEK -> dateCalendarUtils.getTimePeriod(DateCalendarUtils.PERIOD_WEEK)
            GeneralPeriod.MONTH -> dateCalendarUtils.getTimePeriod(DateCalendarUtils.PERIOD_MONTH)
            GeneralPeriod.PACKAGE -> {
                val dataBytes = userDataManager.get(simId, dataType)
                if (dataBytes.exists()) {
                    dataBytes.startTime to System.currentTimeMillis()
                } else {
                    0L to 0L
                }
            }
        }
    }

}
