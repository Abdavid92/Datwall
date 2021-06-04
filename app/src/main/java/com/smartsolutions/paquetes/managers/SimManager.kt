package com.smartsolutions.paquetes.managers

import android.os.Build
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.repositories.models.Sim
import javax.inject.Inject

class SimManager @Inject constructor(
    private val simDelegate: SimDelegate
) {

    fun getActivesSims(): List<Sim> {
        //Código de ejemplo como idea
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {

        }
        return listOf(
            Sim("id embeido", 0)
        )
    }
}