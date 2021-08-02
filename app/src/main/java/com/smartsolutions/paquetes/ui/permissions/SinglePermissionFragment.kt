package com.smartsolutions.paquetes.ui.permissions

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.viewModels
import com.google.android.material.textview.MaterialTextView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.models.Permission
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val ARG_REQUEST_CODE = "request_code"

/**
 *
 * A fragment that shows a list of items as a modal bottom sheet.
 *
 * You can show this modal bottom sheet from your activity like this:
 * <pre>
 *    SinglePermissionFragment.newInstance(30).show(supportFragmentManager, "dialog")
 * </pre>
 */
@AndroidEntryPoint
class SinglePermissionFragment private constructor(
    private val callback: SinglePermissionCallback?
): BottomSheetDialogFragment() {

    private val viewModel by viewModels<SinglePermissionViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestCode = arguments?.getInt(ARG_REQUEST_CODE)

        viewModel.initPermission(requestCode)

        if (viewModel.callback == null)
            viewModel.callback = callback
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_single_permission, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<MaterialTextView>(R.id.permission_name)
            .text = viewModel.permission.name

        view.findViewById<MaterialTextView>(R.id.description)
            .text = viewModel.permission.description

        view.findViewById<AppCompatButton>(R.id.btn_jump).apply {
            if (viewModel.permission.category == Permission.Category.Required)
                this.visibility = View.GONE
            else {
                setOnClickListener {
                    dismiss()
                    notifyResult(false)
                }
            }
        }

        view.findViewById<AppCompatButton>(R.id.btn_grant)
            .setOnClickListener {
                viewModel.permission
                    .requestPermissionFragment(viewModel.permission, this)
            }
    }

    private fun notifyResult(granted: Boolean) {
        viewModel.granted = granted
        if (granted)
            viewModel.callback?.onGranted()
        else
            viewModel.callback?.onDenied()

        this.dismiss()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == viewModel.permission.requestCode) {
            var granted = true

            grantResults.forEach {
                if (it == PackageManager.PERMISSION_DENIED)
                    granted = false
            }

            notifyResult(granted)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == viewModel.permission.requestCode) {
            context?.let {
                notifyResult(viewModel.permission.checkPermission(viewModel.permission, it))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!viewModel.granted)
            viewModel.callback?.onDenied()
    }

    companion object {

        fun newInstance(
            requestCode: Int,
            callback: SinglePermissionCallback? = null
        ): SinglePermissionFragment =
            SinglePermissionFragment(callback).apply {
                arguments = Bundle().apply {
                    putInt(ARG_REQUEST_CODE, requestCode)
                }
            }
    }

    interface SinglePermissionCallback {
        fun onGranted()
        fun onDenied()
    }
}