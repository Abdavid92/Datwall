package com.smartsolutions.paquetes.ui.permissions

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.models.Permission
import dagger.hilt.android.AndroidEntryPoint
import moe.feng.common.stepperview.VerticalStepperView

/**
 * Actividad de los permisos.
 * */
@AndroidEntryPoint
class PermissionsActivity : AppCompatActivity(R.layout.activity_permissions) {

    private val viewModel by viewModels<PermissionsViewModel>()

    private lateinit var steppers: VerticalStepperView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(findViewById(R.id.toolbar))

        steppers = findViewById(R.id.steppers)

        steppers.stepperAdapter = StepperAdapter(viewModel.permissions, this)
    }

    fun nextStep() {
        steppers.setErrorText(steppers.currentStep, null)
        if (steppers.canNext())
            steppers.nextStep()
        else {
            viewModel.finish(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        viewModel.permissions.firstOrNull {
            it.requestCode == requestCode
        }?.let {
            processPermissionResult(it.checkPermission(it, this), it)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        viewModel.permissions.firstOrNull {
            it.requestCode == requestCode
        }?.let {
            var granted = true

            grantResults.forEach { result ->
                if (result == PackageManager.PERMISSION_DENIED)
                    granted = false
            }

            processPermissionResult(granted, it)
        }
    }

    private fun processPermissionResult(granted: Boolean, permission: Permission) {
        if (granted) {
            nextStep()
        } else {
            if (permission.category == Permission.Category.Required) {
                steppers.setErrorText(steppers.currentStep, getString(R.string.required_permission_denied))
            } else {
                steppers.setErrorText(steppers.currentStep, getString(R.string.optional_permission_denied))
            }
        }
    }
}