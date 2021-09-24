package com.smartsolutions.paquetes.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.databinding.FragmentDashboardBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private val viewModel by viewModels<DashboardViewModel>()

    lateinit var binding: FragmentDashboardBinding
        private set

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
        setUssdButtonsSettings()
    }

    private fun setFirewallSettings() {
        viewModel.setFirewallSwitchListener(binding.firewall, childFragmentManager)

        binding.firewallControl.setOnClickListener {
            val activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(
                requireActivity(),
                Pair(binding.firewallControl, IControls.CARD_VIEW),
                Pair(binding.firewallHeader, IControls.HEADER),
                Pair(binding.firewall, IControls.SWITCH)
            )

            startActivity(
                FirewallControls.getLaunchIntent(requireContext()),
                activityOptions.toBundle()
            )
        }
    }

    private fun setBubbleSettings() {
        viewModel.setBubbleSwitchListener(binding.bubble, childFragmentManager)

        binding.bubbleControl.setOnClickListener {
            val activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(
                requireActivity(),
                Pair(binding.bubbleControl, IControls.CARD_VIEW),
                Pair(binding.bubbleHeader, IControls.HEADER),
                Pair(binding.bubble, IControls.SWITCH)
            )
            startActivity(
                BubbleControls.getLaunchIntent(requireContext()),
                activityOptions.toBundle()
            )
        }
    }

    private fun setUssdButtonsSettings() {
        binding.queryCredit.setOnClickListener {
            viewModel.launchUssdCode("*222#", childFragmentManager)
        }

        binding.queryBonus.setOnClickListener {
            viewModel.launchUssdCode("*222*266#", childFragmentManager)
        }

        binding.queryMb.setOnClickListener {
            viewModel.launchUssdCode("*222*328#", childFragmentManager)
        }
    }
}