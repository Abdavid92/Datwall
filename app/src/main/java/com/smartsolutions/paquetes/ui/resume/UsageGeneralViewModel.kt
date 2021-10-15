package com.smartsolutions.paquetes.ui.resume

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.helpers.DateCalendarUtils
import com.smartsolutions.paquetes.repositories.contracts.IUsageGeneralRepository
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.UsageGeneral
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class UsageGeneralViewModel @Inject constructor(
    private val usageGeneralRepository: IUsageGeneralRepository,
    private val dateCalendarUtils: DateCalendarUtils,
    private val userDataBytesRepository: IUserDataBytesRepository
) : ViewModel() {

    private var period = PeriodUsageGeneral.TODAY

    private var liveData = MutableLiveData<Pair<List<UsageGeneral>, DateCalendarUtils.MyTimeUnit>>()


    fun setPeriod(index: Int, simId: String, dataType: DataBytes.DataType) {
        period = PeriodUsageGeneral.values()[index]
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
            val period = getPeriod(simId, dataType)
            val timeUnit = when (this@UsageGeneralViewModel.period) {
                PeriodUsageGeneral.TODAY, PeriodUsageGeneral.YESTERDAY -> DateCalendarUtils.MyTimeUnit.MINUTE
                PeriodUsageGeneral.WEEK, PeriodUsageGeneral.MONTH -> DateCalendarUtils.MyTimeUnit.DAY
                else -> DateCalendarUtils.MyTimeUnit.DAY
            }

            liveData.postValue(
                filterCompact(
                    withContext(Dispatchers.IO) {
                        usageGeneralRepository.inRangeTime(
                            period.first,
                            period.second
                        )
                    }.filter { it.simId == simId && it.type == dataType },
                    timeUnit
                ) to timeUnit
            )
        }
    }

    private suspend fun getPeriod(simId: String, dataType: DataBytes.DataType): Pair<Long, Long> {
        return when (period) {
            PeriodUsageGeneral.TODAY -> dateCalendarUtils.getTimePeriod(DateCalendarUtils.PERIOD_TODAY)
            PeriodUsageGeneral.YESTERDAY -> dateCalendarUtils.getTimePeriod(DateCalendarUtils.PERIOD_YESTERDAY)
            PeriodUsageGeneral.WEEK -> dateCalendarUtils.getTimePeriod(DateCalendarUtils.PERIOD_WEEK)
            PeriodUsageGeneral.MONTH -> dateCalendarUtils.getTimePeriod(DateCalendarUtils.PERIOD_MONTH)
            PeriodUsageGeneral.PACKAGE -> {
                val dataBytes = userDataBytesRepository.get(simId, dataType)
                if (dataBytes.exists()) {
                    dataBytes.startTime to System.currentTimeMillis()
                } else {
                    0L to 0L
                }
            }
        }
    }

    private fun filterCompact(
        list: List<UsageGeneral>,
        timeUnit: DateCalendarUtils.MyTimeUnit
    ): List<UsageGeneral> {
        val compacted = mutableListOf<UsageGeneral>()

        var sameUsage: UsageGeneral? = null

        list.forEach { usage ->
            if (sameUsage == null){
                sameUsage = usage
            }else {
                val isSame = if (timeUnit == DateCalendarUtils.MyTimeUnit.MINUTE){
                    DateCalendarUtils.isSameMinute(usage.date, sameUsage!!.date)
                }else {
                    DateCalendarUtils.isSameDay(usage.date, sameUsage!!.date)
                }

                if (isSame){
                    sameUsage!! += usage
                }else {
                    compacted.add(sameUsage!!)
                    sameUsage = usage
                }
            }
        }

        return compacted
    }


    enum class PeriodUsageGeneral {
        TODAY,
        YESTERDAY,
        WEEK,
        MONTH,
        PACKAGE
    }

}