package com.smartsolutions.paquetes.ui.dashboard

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.viewModels
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentDashboard2Binding
import com.smartsolutions.paquetes.ui.AbstractFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardFragment2 : AbstractFragment() {

    private val viewModel by viewModels<DashboardViewModel>()

    private var _binding: FragmentDashboard2Binding? = null
    private val binding get() = _binding!!

    override fun getTitle(): CharSequence {
        return requireContext().getString(R.string.title_dashboard)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        if (canWork()) {
            _binding = FragmentDashboard2Binding.inflate(
                inflater,
                container,
                false
            )

            setFabSettings()

            return binding.root
        }

        return inflatePurchasedFunctionLayout(inflater, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!canWork())
            return

        setUssdButtonsSettings()
        setFirewallSettings()
        setBubbleSettings()
    }

    private fun setFirewallSettings() {

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
    }

    private fun setBubbleSettings() {

        viewModel.setBubbleSwitchListener(
            binding.bubble,
            childFragmentManager
        )

        viewModel.setBubbleAllWayListener(
            binding.allWay,
            binding.onlyConsume
        )

        viewModel.setTransparencyListener(
            binding.bubbleTransparency
        )

        viewModel.setSizeListener(
            binding.bubbleSize
        )
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

                        val ratio = View.MeasureSpec.getSize(280) // 280

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}