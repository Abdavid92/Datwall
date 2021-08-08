package com.smartsolutions.paquetes.ui.firewall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.databinding.FragmentFirewallBinding
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.repositories.models.IApp
import com.smartsolutions.paquetes.ui.ApplicationFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FirewallFragment : ApplicationFragment() {

    private val viewModel by viewModels<FirewallViewModel>()

    private lateinit var binding: FragmentFirewallBinding

    @Inject
    lateinit var iconManager: IIconManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFirewallBinding.inflate(
            layoutInflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        viewModel.getApps().observe(viewLifecycleOwner, {
            val adapter = AppsListAdapter(requireContext(), it, iconManager)
            adapter.onAccessChange = ::onAccessChange
            binding.appsList.adapter = adapter
        })
    }

    private fun onAccessChange(app: IApp) {
        viewModel.updateApp(app)
    }

    override fun onPause() {
        super.onPause()
        viewModel.confirmUpdates()
    }
}