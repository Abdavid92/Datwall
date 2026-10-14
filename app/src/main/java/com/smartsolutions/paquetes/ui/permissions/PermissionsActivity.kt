package com.smartsolutions.paquetes.ui.permissions

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.PermissionAction
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.ui.AbstractActivity
import com.smartsolutions.paquetes.ui.addOpenActivityListener
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

    private val adapter = PermissionsAdapter { onPermissionAction(it) }

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
            findViewById<View>(R.id.btn_not_now).setOnClickListener { viewModel.finishFirstUse() }
            findViewById<View>(R.id.btn_continue).setOnClickListener { viewModel.finishFirstUse() }

            viewModel.addOpenActivityListener(this) { activity, _, application ->
                application.removeOpenActivityListener(this)
                startActivity(Intent(this, activity))
                finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        lifecycleScope.launch {
            adapter.items = viewModel.permissions(isFirstUse).map {
                PermissionItem(it, viewModel.permissionsManager.actionFor(it, this@PermissionsActivity))
            }
        }
    }

    private fun onPermissionAction(item: PermissionItem) {
        when (item.action) {
            PermissionAction.Nothing -> Unit
            PermissionAction.OpenAppSettings -> openAppSettings()
            PermissionAction.Request -> lifecycleScope.launch {
                viewModel.permissionsManager.markRequested(item.permission.requestCode)
                item.permission.requestPermissionActivity(item.permission, this@PermissionsActivity)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        refresh()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val granted = grantResults.isNotEmpty() &&
                grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }

        if (!isFirstUse && granted && requestCode == IPermissionsManager.CALL_CODE) {
            lifecycleScope.launch {
                if (restartIfSimSetupPending(viewModel.configurationManager))
                    finish()
                else
                    refresh()
            }
        } else {
            refresh()
        }
    }

    companion object {
        /**
         * Acción con la que Ajustes abre la sección "Permisos y privacidad"
         * (res/xml/header_preferences.xml).
         * */
        const val ACTION_PERMISSIONS_SETTINGS = "com.smartsolutions.paquetes.action.PERMISSIONS_SETTINGS"
    }
}
