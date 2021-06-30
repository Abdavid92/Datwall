package com.smartsolutions.paquetes.ui.permissions

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.PermissionsManager
import com.smartsolutions.paquetes.managers.models.Permission
import dagger.hilt.android.AndroidEntryPoint
import moe.feng.common.stepperview.VerticalStepperItemView
import moe.feng.common.stepperview.VerticalStepperView
import javax.inject.Inject

private const val REQUEST_CODES = "request_codes"

/**
 * A simple [Fragment] subclass.
 * Use the [PermissionsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
class PermissionsFragment private constructor(
    private val callback: PermissionFragmentCallback?
): Fragment() {

    private lateinit var steppers: VerticalStepperView

    private var permissions = emptyList<Permission>()

    @Inject
    lateinit var permissionsManager: PermissionsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            bundle.getIntArray(REQUEST_CODES)?.let {
                permissions = permissionsManager.findPermissions(it)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_permissions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        steppers = view.findViewById(R.id.steppers)

        steppers.stepperAdapter = StepperAdapter(permissions, this)

    }

    fun nextStep() {
        steppers.setErrorText(steppers.currentStep, null)
        if (steppers.canNext())
            steppers.nextStep()
        else
            notifyFinished()
    }

    private fun notifyFinished() {
        callback?.onFinished()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        this.permissions.firstOrNull { it.requestCode == requestCode }?.let { permission ->
            context?.let { context ->
                processPermissionResult(permission.checkPermission(permission, context), permission)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        this.permissions.firstOrNull { it.requestCode == requestCode }?.let { permission ->
            var granted = true

            grantResults.forEach {
                if (it == PackageManager.PERMISSION_DENIED)
                    granted = false
            }

            processPermissionResult(granted, permission)
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

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param requestCodes
         * @param callback
         *
         * @return A new instance of fragment PermissionsFragment.
         */
        @JvmStatic
        fun newInstance(requestCodes: IntArray, callback: PermissionFragmentCallback? = null) =
            PermissionsFragment(callback).apply {
                arguments = Bundle().apply {
                    putIntArray(REQUEST_CODES, requestCodes)
                }
            }
    }

    interface PermissionFragmentCallback {
        fun onFinished()
    }
}