package com.smartsolutions.paquetes.ui.dashboard

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.smartsolutions.paquetes.services.BubbleFloatingService
import com.smartsolutions.paquetes.ui.permissions.onPermissionResult
import com.smartsolutions.paquetes.ui.permissions.showPermissionSheet
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.coroutines.launch
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentDashboard2Binding
import com.smartsolutions.paquetes.ui.AbstractFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardFragment2 : AbstractFragment() {

    private val viewModel by viewModels<DashboardViewModel>()

    /**
     * Burbuja de muestra que acompaña a los controles de tamaño y transparencia.
     * */
    private var bubbleView: View? = null

    private var rendering = false

    private var _binding: FragmentDashboard2Binding? = null
    private val binding get() = _binding!!

    override fun getTitle(): CharSequence {
        return requireContext().getString(R.string.title_dashboard)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onPermissionResult { requestCode, granted ->
            if (granted)
                viewModel.onPermissionGranted(requestCode)
            else
                viewModel.onPermissionDenied()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboard2Binding.inflate(
            inflater,
            container,
            false
        )

        setFabSettings()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUssdButtonsSettings()
        setFirewallSettings()
        setBubbleSettings()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.events.collect(::onEvent) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshApps()
    }

    private fun setFirewallSettings() {
        binding.firewall.setOnCheckedChangeListener { _, isChecked ->
            if (!rendering)
                viewModel.onFirewallToggled(isChecked)
        }

        binding.dynamicMode.setOnCheckedChangeListener { _, isChecked ->
            if (!rendering)
                viewModel.onDynamicFirewallToggled(isChecked)
        }
    }

    private fun setBubbleSettings() {
        binding.bubble.setOnCheckedChangeListener { _, isChecked ->
            if (!rendering)
                viewModel.onBubbleToggled(isChecked)
        }

        binding.allWay.setOnCheckedChangeListener { _, isChecked ->
            if (!rendering)
                viewModel.onBubbleAlwaysShowChanged(isChecked)
        }

        // La burbuja de muestra que se ve sobre el control mientras se arrastra.
        bubbleView = LayoutInflater.from(requireContext())
            .inflate(R.layout.bubble_floating_layout, null, false).apply {
                findViewById<LinearLayout>(R.id.lin_background_bubble).visibility = View.VISIBLE
            }

        binding.bubbleTransparency.indicator.contentView = bubbleView
        binding.bubbleSize.indicator.contentView = bubbleView

        binding.bubbleTransparency.onSeekChangeListener = object : OnSeekChangeListener {
            override fun onSeeking(seekParams: SeekParams) {
                if (seekParams.fromUser) {
                    bubbleView?.findViewById<LinearLayout>(R.id.lin_background_bubble)
                        ?.alpha = seekParams.progress.toFloat() / 10f
                }
            }

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {}

            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {
                viewModel.onBubbleTransparencyChanged(seekBar.progress.toFloat() / 10f)
            }
        }

        binding.bubbleSize.onSeekChangeListener = object : OnSeekChangeListener {
            override fun onSeeking(seekParams: SeekParams) {
                showSampleSize(seekParams.progress)
            }

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {}

            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {
                viewModel.onBubbleSizeChanged(seekBar.progress)
            }
        }
    }

    private fun showSampleSize(size: Int) {
        bubbleView?.let {
            BubbleFloatingService.setSizeBubble(
                requireContext(),
                it,
                BubbleFloatingService.BubbleSize.entries[size]
            )
        }
    }

    /**
     * Dibuja el estado sin que los cambios de los controles se tomen como acciones del usuario.
     * */
    private fun render(state: DashboardUiState) {
        rendering = true
        try {
            binding.firewall.isChecked = state.firewallOn
            binding.dynamicMode.isChecked = state.dynamicFirewall
            binding.staticMode.isChecked = !state.dynamicFirewall

            binding.allowedAppsValue.text = state.allowedApps.toString()
            binding.blockedAppsValue.text = state.blockedApps.toString()
            binding.allAppsValue.text = state.totalApps.toString()

            binding.bubble.isChecked = state.bubbleOn
            binding.allWay.isChecked = state.bubbleAlwaysShow
            binding.onlyConsume.isChecked = !state.bubbleAlwaysShow

            binding.bubbleTransparency.setProgress(state.bubbleTransparency * 10)
            bubbleView?.findViewById<LinearLayout>(R.id.lin_background_bubble)
                ?.alpha = state.bubbleTransparency

            binding.bubbleSize.setProgress(state.bubbleSize.toFloat())
            showSampleSize(state.bubbleSize)
        } finally {
            rendering = false
        }
    }

    private fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.RequestPermission -> showPermissionSheet(event.requestCode)

            DashboardEvent.FirewallNeedsBlockedApp -> {
                render(viewModel.state.value)
                Toast.makeText(
                    requireContext(),
                    R.string.error_starting_firewall_for_all_access,
                    Toast.LENGTH_SHORT
                ).show()
            }

            DashboardEvent.StateOutOfSync -> render(viewModel.state.value)
        }
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

                                override fun onAnimationEnd(animation: Animator) {

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

                                override fun onAnimationEnd(animation: Animator) {
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
            viewModel.onUssdShortcut(DashboardViewModel.USSD_CREDIT)
        }

        binding.queryBonus.setOnClickListener {
            viewModel.onUssdShortcut(DashboardViewModel.USSD_BONUS)
        }

        binding.queryMb.setOnClickListener {
            viewModel.onUssdShortcut(DashboardViewModel.USSD_MB)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        bubbleView = null
    }
}