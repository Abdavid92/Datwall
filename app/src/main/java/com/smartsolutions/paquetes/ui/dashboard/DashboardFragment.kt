package com.smartsolutions.paquetes.ui.dashboard

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
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

        setFabSettings()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setFirewallSettings()
        setBubbleSettings()
        setUssdButtonsSettings()
    }

    private fun setFabSettings() {
        binding.queryCredit.shrink()
        binding.queryBonus.shrink()
        binding.queryMb.shrink()

        binding.fabQuery.setOnClickListener {
            val fab = it as ExtendedFloatingActionButton

            val interpolator = AnimationUtils.loadInterpolator(
                requireContext(),
                android.R.interpolator.decelerate_quint
            )
            
            if (fab.isExtended) {
                fab.shrink(object : ExtendedFloatingActionButton.OnChangedCallback() {

                    override fun onShrunken(extendedFab: ExtendedFloatingActionButton?) {
                        binding.queryCredit.visibility = View.VISIBLE
                        binding.queryBonus.visibility = View.VISIBLE
                        binding.queryMb.visibility = View.VISIBLE

                        val ratio = 280

                        val coordinates = Point(ratio / 2, ratio / 2)

                        binding.queryCredit.animate()
                            .rotation(360f)
                            .translationX(-ratio.toFloat())
                            .interpolator = interpolator

                        binding.queryBonus.animate()
                            .rotation(360f)
                            .translationY(-coordinates.y.toFloat())
                            .translationX(-coordinates.x.toFloat())
                            .setStartDelay(100)
                            .interpolator = interpolator

                        binding.queryMb.animate()
                            .rotation(360f)
                            .translationY(-ratio.toFloat())
                            .setInterpolator(interpolator)
                            .setStartDelay(200)
                            .setListener(object : AnimatorListenerAdapter() {

                                override fun onAnimationEnd(animation: Animator?) {

                                    binding.queryCredit.extend()
                                    binding.queryBonus.extend()
                                    binding.queryMb.extend()
                                }
                            })
                    }
                })
            } else {
                binding.queryCredit.shrink()
                binding.queryBonus.shrink()
                binding.queryMb.shrink(object : ExtendedFloatingActionButton.OnChangedCallback() {

                    override fun onShrunken(extendedFab: ExtendedFloatingActionButton?) {

                        binding.queryCredit.animate()
                            .rotation(360f)
                            .translationX(0f)
                            .interpolator = interpolator

                        binding.queryBonus.animate()
                            .rotation(360f)
                            .translationY(0f)
                            .translationX(0f)
                            .setStartDelay(100)
                            .interpolator = interpolator

                        binding.queryMb.animate()
                            .rotation(360f)
                            .translationY(0f)
                            .setStartDelay(200)
                            .setInterpolator(interpolator)
                            .setListener(object : AnimatorListenerAdapter() {

                                override fun onAnimationEnd(animation: Animator?) {
                                    binding.queryCredit.visibility = View.INVISIBLE
                                    binding.queryBonus.visibility = View.INVISIBLE
                                    binding.queryMb.visibility = View.INVISIBLE

                                    fab.extend()
                                }
                            })
                    }
                })
            }
        }
    }

    private fun setFirewallSettings() {
        viewModel.setFirewallSwitchListener(binding.firewall, childFragmentManager)

        binding.firewallControl.setOnClickListener {
            val activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(
                requireActivity(),
                Pair(binding.firewallControl, IControls.CARD_VIEW),
                Pair(binding.firewallHeader, IControls.HEADER),
                Pair(binding.firewall, IControls.SWITCH),
                Pair(binding.firewallSummary, IControls.SUMMARY)
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
                Pair(binding.bubble, IControls.SWITCH),
                Pair(binding.bubbleSummary, IControls.SUMMARY)
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