package com.smartsolutions.paquetes.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.smartsolutions.paquetes.ui.ApplicationFragment
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.annotations.ApplicationStatus
import com.smartsolutions.paquetes.databinding.FragmentDashboardBinding
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.serverApis.models.AndroidApp
import com.smartsolutions.paquetes.ui.MainActivity
import com.smartsolutions.paquetes.ui.permissions.SinglePermissionFragment
import com.smartsolutions.paquetes.ui.settings.UpdateFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private val dashboardViewModel by viewModels<DashboardViewModel>()

    private lateinit var binding: FragmentDashboardBinding

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashboardBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}