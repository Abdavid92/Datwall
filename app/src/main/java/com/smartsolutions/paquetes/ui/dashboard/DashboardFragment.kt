package com.smartsolutions.paquetes.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentDashboardBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private val viewModel by viewModels<DashboardViewModel>()

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

        setFirewallSettings()
        setBubbleSettings()
        setMoreConsumeSettings()
        setUssdButtonsSettings()
    }

    private fun setFirewallSettings() {
        viewModel.setFirewallSwitchListener(binding.firewall, childFragmentManager)
        viewModel.setFirewallDynamicModeListener(binding.dynamicMode, binding.staticMode)

        viewModel.appsData.observe(viewLifecycleOwner) {
            binding.allowedApps.text = getString(R.string.allowed_apps, it[0])
            binding.blockedApps.text = getString(R.string.blocked_apps, it[1])
            binding.allApps.text = getString(R.string.all_apps, it[2])
        }
    }

    private fun setBubbleSettings() {
        viewModel.setBubbleSwitchListener(binding.bubble, childFragmentManager)
    }

    private fun setMoreConsumeSettings() {
        viewModel.appMoreConsume.observe(viewLifecycleOwner) {

            if (it != null) {

                viewModel.setAppIcon(it, binding.iconMoreConsume)
                binding.titleAppMoreConsume.text = it.name
                binding.valueAppMoreConsume.text = it.traffic?.totalBytes.toString()
                binding.valueAppMoreConsume.visibility = View.VISIBLE
            } else {

                binding.titleAppMoreConsume.text = getString(R.string.failed_find_app_more_consume)
            }
        }
    }

    private fun setUssdButtonsSettings() {
        binding.queryCredit.setOnClickListener {
            viewModel.launchUssdCode("*222#", childFragmentManager)
        }

        binding.queryBonus.setOnClickListener {
            viewModel.launchUssdCode("*222*266#", childFragmentManager)
        }
    }
}