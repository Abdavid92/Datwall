package com.smartsolutions.paquetes.helpers

import androidx.fragment.app.FragmentManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager2
import com.smartsolutions.paquetes.repositories.models.Sim
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext


class SimsHelper @Inject constructor(
    private val simManager: ISimManager2
) {


    suspend fun invokeOnDefault(
        sim: Sim,
        simType: SimDelegate.SimType,
        fragmentManager: FragmentManager,
        onDefault: () -> Unit
    ) {
        val result = simManager.isSimDefaultSystem(simType, sim)
        when {
            result == null -> {
                //TODO No se pudo saber.Preguntar
            }
            result -> {
                withContext(Dispatchers.Main) {
                    onDefault()
                }
            }
            else -> {
                //TODO No se puede realizar la acción informar
            }
        }
    }


}