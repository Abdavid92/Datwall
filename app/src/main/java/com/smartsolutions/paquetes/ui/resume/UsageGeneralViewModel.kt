package com.smartsolutions.paquetes.ui.resume

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.helpers.NetworkUsageUtils
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
    private val networkUsageUtils: NetworkUsageUtils,
    private val userDataBytesRepository: IUserDataBytesRepository
) : ViewModel() {

    private var period = PeriodUsageGeneral.TODAY

    private var liveData = MutableLiveData<Pair<List<UsageGeneral>, NetworkUsageUtils.MyTimeUnit>>()


    fun setPeriod(index: Int, simId: String, dataType: DataBytes.DataType) {
        period = PeriodUsageGeneral.values()[index]
        getUsage(dataType, simId)
    }

    fun getUsageGeneral(
        dataType: DataBytes.DataType,
        simId: String
    ): LiveData<Pair<List<UsageGeneral>, NetworkUsageUtils.MyTimeUnit>> {
        getUsage(dataType, simId)
        return liveData
    }


    private fun getUsage(dataType: DataBytes.DataType, simId: String) {
        viewModelScope.launch {
            val period = getPeriod(simId, dataType)
            val timeUnit = when (this@UsageGeneralViewModel.period) {
                PeriodUsageGeneral.TODAY, PeriodUsageGeneral.YESTERDAY -> NetworkUsageUtils.MyTimeUnit.HOUR
                PeriodUsageGeneral.WEEK, PeriodUsageGeneral.MONTH -> NetworkUsageUtils.MyTimeUnit.DAY
                else -> NetworkUsageUtils.MyTimeUnit.DAY
            }

            liveData.postValue(
                filterCompact(
                    withContext(Dispatchers.IO) {
                        usageGeneralRepository.inRangeTime(
                            period.first,
                            period.second
                        )
                    }.filter { it.simId == simId },
                    timeUnit
                ) to timeUnit
            )
        }
    }

    private suspend fun getPeriod(simId: String, dataType: DataBytes.DataType): Pair<Long, Long> {
        return when (period) {
            PeriodUsageGeneral.TODAY -> networkUsageUtils.getTimePeriod(NetworkUsageUtils.PERIOD_TODAY)
            PeriodUsageGeneral.YESTERDAY -> networkUsageUtils.getTimePeriod(NetworkUsageUtils.PERIOD_YESTERDAY)
            PeriodUsageGeneral.WEEK -> networkUsageUtils.getTimePeriod(NetworkUsageUtils.PERIOD_WEEK)
            PeriodUsageGeneral.MONTH -> networkUsageUtils.getTimePeriod(NetworkUsageUtils.PERIOD_MONTH)
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
        timeUnit: NetworkUsageUtils.MyTimeUnit
    ): List<UsageGeneral> {
        TODO()
    }


    enum class PeriodUsageGeneral {
        TODAY,
        YESTERDAY,
        WEEK,
        MONTH,
        PACKAGE
    }

}