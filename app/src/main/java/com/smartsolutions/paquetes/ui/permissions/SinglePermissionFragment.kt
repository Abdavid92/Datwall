package com.smartsolutions.paquetes.ui.permissions

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import com.google.android.material.textview.MaterialTextView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.PacketManager
import com.smartsolutions.paquetes.managers.PermissionsManager
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

    @Inject
    lateinit var permissionsManager: PermissionsManager

    private lateinit var permission: Permission

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestCode = arguments?.getInt(ARG_REQUEST_CODE)

        permission = permissionsManager.findPermission(requestCode ?: 0)
            ?: throw IllegalArgumentException()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_single_permission, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<MaterialTextView>(R.id.permission_name)
            .text = permission.name

        view.findViewById<MaterialTextView>(R.id.description)
            .text = permission.description

        view.findViewById<AppCompatButton>(R.id.btn_cancel).apply {
            if (permission.category == Permission.Category.Required)
                this.visibility = View.GONE
            else {
                setOnClickListener {
                    dismiss()
                    notifyResult(false)
                }
            }
        }

        view.findViewById<AppCompatButton>(R.id.btn_grant)
            .setOnClickListener { permission.requestPermissionFragment(permission, this) }
    }

    private fun notifyResult(granted: Boolean) {
        if (granted)
            callback?.onGranted()
        else
            callback?.onDenied()

        this.dismiss()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == permission.requestCode) {
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

        if (requestCode == permission.requestCode) {
            context?.let {
                notifyResult(permission.checkPermission(permission, it))
            }
        }
    }

    companion object {

        fun newInstance(requestCode: Int, callback: SinglePermissionCallback? = null): SinglePermissionFragment =
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