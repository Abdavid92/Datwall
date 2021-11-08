package com.smartsolutions.paquetes.ui.dashboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.transition.Transition
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.smartsolutions.paquetes.databinding.FirewallControlsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FirewallControlsFragment : Fragment() {

    private var _binding: FirewallControlsBinding? = null
    private val binding get() = _binding!!

    private val viewModel by activityViewModels<DashboardViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FirewallControlsBinding.inflate(
            inflater,
            container,
            false
        )

        setTransitionNames()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setFirewallSwitchListener(binding.firewall, childFragmentManager)

        viewModel.setFirewallDynamicModeListener(
            binding.dynamicMode,
            binding.staticMode
        )

        viewModel.appsData.observe(viewLifecycleOwner) {
            binding.allowedAppsValue.text = it[0].toString()
            binding.blockedAppsValue.text = it[1].toString()
            binding.allAppsValue.text = it[2].toString()
        }

        binding.root.setOnClickListener {
            activity?.onBackPressed()
        }
        binding.firewallControls.setOnClickListener {
            //Empty
        }
    }

    private fun setTransitionNames() {
        binding.apply {
            ViewCompat.setTransitionName(firewallControls, DashboardControlActivity.CARD_VIEW)
            ViewCompat.setTransitionName(firewallHeader, DashboardControlActivity.HEADER)
            ViewCompat.setTransitionName(firewall, DashboardControlActivity.SWITCH)
            ViewCompat.setTransitionName(firewallSummary, DashboardControlActivity.SUMMARY)
        }
    }

    override fun onDetach() {
        super.onDetach()

        binding.apply {
            allowedApps.visibility = View.INVISIBLE
            blockedApps.visibility = View.INVISIBLE
            allApps.visibility = View.INVISIBLE
            allowedAppsValue.visibility = View.INVISIBLE
            blockedAppsValue.visibility = View.INVISIBLE
            allowedAppsValue.visibility = View.INVISIBLE
        }
        _binding = null
    }

    companion object {

        fun getLaunchIntent(context: Context): Intent {
            return Intent(context, DashboardControlActivity::class.java)
                .putExtra(
                    DashboardControlActivity.EXTRA_CONTROLS_CLASS_NAME,
                    FirewallControlsFragment::class.java.name
                )
        }
    }
}