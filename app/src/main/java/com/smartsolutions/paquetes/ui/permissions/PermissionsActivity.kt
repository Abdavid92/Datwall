package com.smartsolutions.paquetes.ui.permissions

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.models.Permission
import com.smartsolutions.paquetes.ui.AbstractActivity
import com.smartsolutions.paquetes.ui.addOpenActivityListener
import com.smartsolutions.paquetes.ui.next
import dagger.hilt.android.AndroidEntryPoint
import moe.feng.common.stepperview.VerticalStepperView

/**
 * Actividad de los permisos.
 * */
@AndroidEntryPoint
class PermissionsActivity : AbstractActivity(R.layout.activity_permissions) {

    private val viewModel by viewModels<PermissionsViewModel>()

    private lateinit var steppers: VerticalStepperView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(findViewById(R.id.toolbar))

        steppers = findViewById(R.id.steppers)

        steppers.isAnimationEnabled = true

        steppers.stepperAdapter = StepperAdapter(viewModel.permissions, this)

        viewModel.addOpenActivityListener(this) { activity, application ->
            application.removeOpenActivityListener(this)
            startActivity(Intent(this, activity))
            finish()
        }
    }

    fun nextStep() {

        steppers.setErrorText(steppers.currentStep, null)

        if (steppers.canNext())
            steppers.nextStep()
        else {
            viewModel.next()
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