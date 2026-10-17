package com.smartsolutions.paquetes.ui.permissions

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.ui.AbstractActivity
import com.smartsolutions.paquetes.ui.followStartDestination
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Explicación del uso de datos y lista de permisos. En el primer uso muestra solo los permisos
 * que se piden al inicio y deja continuar sin conceder ninguno; desde Ajustes ("Permisos y
 * privacidad") muestra todos y la recomendación de batería.
 * */
@AndroidEntryPoint
class PermissionsActivity : AbstractActivity(R.layout.activity_permissions) {

    private val viewModel by viewModels<PermissionsViewModel>()

    private val adapter = PermissionsAdapter { viewModel.onPermissionClicked(it) }

    private val isFirstUse by lazy { intent.action != ACTION_PERMISSIONS_SETTINGS }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(findViewById(R.id.toolbar))

        if (!isFirstUse) {
            setTitle(R.string.permissions_privacy)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        findViewById<RecyclerView>(R.id.permissions).adapter = adapter

        findViewById<View>(R.id.battery_recommendation).apply {
            visibility = if (isFirstUse) View.GONE else View.VISIBLE

            findViewById<View>(R.id.btn_battery).setOnClickListener {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }

        if (isFirstUse) {
            findViewById<View>(R.id.startup_buttons).visibility = View.VISIBLE
            findViewById<View>(R.id.btn_not_now).setOnClickListener { viewModel.onFinishFirstUse() }
            findViewById<View>(R.id.btn_continue).setOnClickListener { viewModel.onFinishFirstUse() }
            followStartDestination()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { adapter.items = it.items } }
                launch { viewModel.events.collect(::onEvent) }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        viewModel.onShown(isFirstUse, rationaleKeys())
    }

    /**
     * Claves para las que el sistema todavía deja volver a preguntar. Se calculan aquí porque
     * necesitan la actividad, y se entregan a la lógica como un dato.
     * */
    private fun rationaleKeys() = viewModel.permissionKeys.filter {
        ActivityCompat.shouldShowRequestPermissionRationale(this, it)
    }.toSet()

    private fun onEvent(event: PermissionsEvent) {
        when (event) {
            is PermissionsEvent.RequestFromSystem -> PermissionLauncher.request(event.permission, this)
            PermissionsEvent.OpenAppSettings -> openAppSettings()
            PermissionsEvent.SimSetupPending -> {
                openStartScreen()
                finish()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.onShown(isFirstUse, rationaleKeys())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        viewModel.onSystemResult(
            requestCode,
            grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED },
            rationaleKeys()
        )
    }

    companion object {

        /**
         * Acción con la que Ajustes abre la sección "Permisos y privacidad"
         * (res/xml/header_preferences.xml).
         * */
        const val ACTION_PERMISSIONS_SETTINGS = "com.smartsolutions.paquetes.action.PERMISSIONS_SETTINGS"
    }
}
