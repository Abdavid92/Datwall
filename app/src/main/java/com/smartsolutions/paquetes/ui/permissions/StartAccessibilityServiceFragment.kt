package com.smartsolutions.paquetes.ui.permissions

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.smartsolutions.paquetes.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * A simple [Fragment] subclass.
 * Use the [StartAccessibilityServiceFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
class StartAccessibilityServiceFragment : BottomSheetDialogFragment() {

    private var listener: SinglePermissionFragment.SinglePermissionCallback? = null

    private val viewModel by viewModels<StartAccessibilityServiceViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_start_accessibility_service,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (this.listener != null)
            viewModel.listener = this.listener
        else
            this.listener = viewModel.listener

        view.findViewById<Button>(R.id.btn_open_settings)
            .setOnClickListener {
                viewModel.openAccessibilityServicesActivity()
            }

        view.findViewById<Button>(R.id.btn_jump)
            .setOnClickListener {
                listener?.onDenied()
                closeFragment()
            }
    }

    override fun onResume() {
        super.onResume()

        if (viewModel.checkAccessibilityService(this))
            closeFragment()
    }

    fun closeFragment() {
        parentFragmentManager.beginTransaction()
            .detach(this)
            .commit()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment StartAccessibilityServiceFragment.
         */
        @JvmStatic
        fun newInstance(listener: SinglePermissionFragment.SinglePermissionCallback? = null) =
            StartAccessibilityServiceFragment().apply {
                this.listener = listener
            }
    }
}