package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.data.DataPackagesContract
import com.smartsolutions.paquetes.managers.contracts.IUserDataBytesManager
import com.smartsolutions.paquetes.managers.models.DataBytes
import com.smartsolutions.paquetes.micubacel.models.DataType
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import org.apache.commons.lang.time.DateUtils
import javax.inject.Inject
import kotlin.math.abs

class UserDataBytesManager @Inject constructor(
    private val userDataBytesRepository: IUserDataBytesRepository,
    private val simManager: SimManager
) : IUserDataBytesManager {

    override suspend fun addDataBytes(dataPackage: DataPackage, simId: String) {
        if (dataPackage.id == DataPackagesContract.DailyBag.id) {
            userDataBytesRepository.get(simId, UserDataBytes.DataType.DailyBag)
                .apply {
                    bytesLte += dataPackage.bytesLte
                    initialBytes = bytesLte
                    startTime = 0
                    expiredTime = 0

                    userDataBytesRepository.update(this)
                }
        } else {
            userDataBytesRepository.update(userDataBytesRepository.bySimId(simId).apply {
                TODO("Este método tiene un error")
                val bonus = first { it.type == UserDataBytes.DataType.Bonus }
                bonus.bytesLte += dataPackage.bonusBytes
                bonus.initialBytes = bonus.bytesLte
                bonus.startTime = 0
                bonus.expiredTime = 0

                val international = first { it.type == UserDataBytes.DataType.International }
                international.bytes += dataPackage.bytes
                international.bytesLte += dataPackage.bytesLte
                international.initialBytes = international.bytes + international.bytesLte
                international.startTime = 0
                international.expiredTime = 0

                val national = first { it.type == UserDataBytes.DataType.National}
                national.bytes = dataPackage.nationalBytes
                national.initialBytes = dataPackage.nationalBytes
                national.startTime = 0
                national.expiredTime = 0
            })
        }
    }

    override suspend fun addPromoBonus(simId: String) {
        userDataBytesRepository.get(simId, UserDataBytes.DataType.PromoBonus).apply {
            bytes += DataBytes.GB.toLong()
            initialBytes = bytes
            startTime = System.currentTimeMillis()
            expiredTime = System.currentTimeMillis() + DateUtils.MILLIS_PER_DAY * 30

            userDataBytesRepository.update(this)
        }
    }

    override suspend fun registerTraffic(rxBytes: Long, txBytes: Long, isLte: Boolean) {

        val sim = simManager.getDefaultDataSim()

        if (isLte) {
            registerLteTraffic(fixTrafficByTime(rxBytes + txBytes), sim.id)
        } else {
            registerTraffic(fixTrafficByTime(rxBytes + txBytes), sim.id)
        }
    }

    override suspend fun synchronizeUserDataBytes(data: List<DataType>, simId: String) {
        //Obtengo todos los userDataBytes de la linea
        val userDataBytes = userDataBytesRepository.bySimId(simId)

        //Iteración por la lista de DataType que me dieron
        data.forEach { dataType ->
            //Busco el UserDataBytes correspondiente al DataType en el que estoy
            userDataBytes.first { it.type == dataType.type }.apply {
                //Analizo el tipo del UserDataBytes
                when (type) {
                    /*Estos tipos de UserDataBytes solo tienen asignada la variable bytesLte
                    * porque funcionan solo en las 4G. Por eso se le asigna el valor del
                    * DataType a dicha variable.*/
                    UserDataBytes.DataType.Bonus,
                    UserDataBytes.DataType.DailyBag -> {
                        bytesLte = dataType.value
                        dataType.dateExpired?.let {
                            expiredTime = it
                        }
                    }
                    /*Estos tipos de UserDataBytes tienen asignada la variable bytes
                    * porque funcionan en todas las redes. Es esta variable la que se aigna.*/
                    UserDataBytes.DataType.PromoBonus,
                    UserDataBytes.DataType.International,
                    UserDataBytes.DataType.National -> {
                        bytes = dataType.value
                        dataType.dateExpired?.let {
                            expiredTime = it
                            /*Si estamos tratando con los datos nacionales, se le asigna la fecha de compra
                            * porque no hay otra forma de resolverla.*/
                            if (type == UserDataBytes.DataType.National)
                                startTime = it - DateUtils.MILLIS_PER_DAY * 30
                        }
                    }
                }
            }
        }
    }

    private suspend fun registerLteTraffic(bytes: Long, simId: String) {
        var consumed = bytes

        suspend fun processUserDataBytes(userDataBytes: UserDataBytes) {
            if (userDataBytes.exists() && !userDataBytes.isExpired()) {
                var rest = userDataBytes.bytesLte - consumed

                if (rest < 0) {
                    userDataBytes.bytesLte = 0
                    consumed = abs(rest)
                } else {
                    userDataBytes.bytesLte = rest
                    consumed = 0
                }

                if (consumed > 0) {
                    rest = userDataBytes.bytes - consumed

                    if (rest < 0) {
                        userDataBytes.bytes = 0
                        consumed = abs(rest)
                    } else {
                        userDataBytes.bytes = rest
                        consumed = 0
                    }
                }

                initTimes(userDataBytes)
                userDataBytesRepository.update(userDataBytes)
            }
        }

        userDataBytesRepository.bySimId(simId)
            .filter { it.type != UserDataBytes.DataType.National }
            .sortedBy { it.priority }
            .forEach {
                if (consumed > 0)
                    processUserDataBytes(it)
            }
    }


    private suspend fun registerTraffic(bytes: Long, simId: String) {
        var consumed = bytes

        suspend fun processUserDataBytes(userDataBytes: UserDataBytes) {
            if (userDataBytes.exists() && !userDataBytes.isExpired()) {
                val rest = userDataBytes.bytes - consumed

                if (rest < 0) {
                    userDataBytes.bytes = 0
                    consumed = abs(rest)
                } else {
                    userDataBytes.bytes = rest
                    consumed = 0
                }

                initTimes(userDataBytes)
                userDataBytesRepository.update(userDataBytes)
            }
        }

        userDataBytesRepository.bySimId(simId)
            .filter {
                it.type == UserDataBytes.DataType.PromoBonus ||
                        it.type == UserDataBytes.DataType.International }
            .sortedBy { it.priority }
            .forEach {
                if (consumed > 0)
                    processUserDataBytes(it)
            }
    }

    private fun initTimes(userDataBytes: UserDataBytes) {
        if (userDataBytes.startTime == 0L) {
            return
        }

        userDataBytes.startTime = System.currentTimeMillis()

        if (userDataBytes.type == UserDataBytes.DataType.DailyBag) {
            userDataBytes.expiredTime = System.currentTimeMillis() + DateUtils.MILLIS_PER_DAY
        }else {
            userDataBytes.expiredTime = System.currentTimeMillis() + DateUtils.MILLIS_PER_DAY * 30
        }
    }

    /**
     * Corrige la cantidad de trafico consumido basado en la hora desde la 1 am hasta las 6 am para usar solo la mitad
     *
     **/
    private fun fixTrafficByTime (bytes: Long) : Long {
        return if (NetworkUtils.isInDiscountHour(System.currentTimeMillis(), System.currentTimeMillis())){
            bytes / 2
        }else {
            bytes
        }
    }
}