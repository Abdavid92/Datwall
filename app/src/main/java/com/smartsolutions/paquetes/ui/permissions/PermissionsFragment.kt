package com.smartsolutions.paquetes.ui.permissions

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
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
@RequiresApi(Build.VERSION_CODES.M)
class PermissionsFragment private constructor(): Fragment() {

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

        val steppers = view.findViewById<VerticalStepperView>(R.id.steppers)

        activity?.let {
            steppers.stepperAdapter = StepperAdapter(permissions, it)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        Toast.makeText(context, "Permission", Toast.LENGTH_SHORT).show()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param requestCodes
         *
         * @return A new instance of fragment PermissionsFragment.
         */
        @JvmStatic
        fun newInstance(requestCodes: IntArray) =
            PermissionsFragment().apply {
                arguments = Bundle().apply {
                    putIntArray(REQUEST_CODES, requestCodes)
                }
            }
    }
}