package com.smartsolutions.paquetes.ui

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.smartsolutions.paquetes.DatwallApplication
import com.smartsolutions.paquetes.managers.models.StartDestination
import com.smartsolutions.paquetes.ui.permissions.PermissionsActivity
import com.smartsolutions.paquetes.ui.setup.SetupActivity
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Pantalla de cada destino del arranque.
 * */
fun activityFor(destination: StartDestination): Class<out Activity> = when (destination) {
    StartDestination.FirstUse -> PermissionsActivity::class.java
    is StartDestination.Setup -> SetupActivity::class.java
    StartDestination.Main -> MainActivity::class.java
}

/**
 * Sigue el destino que decide el arranque: cuando corresponde a otra pantalla, la abre y
 * cierra esta.
 * */
fun AppCompatActivity.followStartDestination() {
    val kernel = (application as DatwallApplication).kernel

    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            kernel.startDestination.filterNotNull().collect { destination ->
                val target = activityFor(destination)

                if (target != this@followStartDestination::class.java) {
                    startActivity(Intent(this@followStartDestination, target))
                    finish()
                }
            }
        }
    }
}
