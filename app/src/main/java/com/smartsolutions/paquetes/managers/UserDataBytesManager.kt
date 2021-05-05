package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.DataPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class UserDataBytesManager @Inject constructor(
    private val userDataBytesRepository: IUserDataBytesRepository
): IUserDataBytesManager, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    override fun addDataBytes(dataPackage: DataPackage, simIndex: Int) {
        launch {
            val data = userDataBytesRepository.getBySimIndex(simIndex)
        }
    }
}