package com.smartsolutions.paquetes.helpers

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager2
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject


class SimsHelper @Inject constructor(
    @ApplicationContext
    private val context: Context,
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
                Toast.makeText(context, "Cual es la SIM?????", Toast.LENGTH_SHORT).show()
            }
            result -> {
                withContext(Dispatchers.Main) {
                    onDefault()
                }
            }
            else -> {
                //TODO No se puede realizar la acción informar
                Toast.makeText(context, "La SIM NO es Predeterminada", Toast.LENGTH_SHORT).show()
            }
        }
    }


}