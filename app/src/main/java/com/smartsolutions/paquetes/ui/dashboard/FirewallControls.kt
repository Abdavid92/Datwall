package com.smartsolutions.paquetes.ui.dashboard

import android.content.Context
import android.content.Intent
import android.transition.Transition
import android.view.View
import androidx.annotation.Keep
import androidx.core.view.ViewCompat
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FirewallControlsBinding
import com.smartsolutions.paquetes.ui.dashboard.IControls.Companion.SWITCH
import com.smartsolutions.paquetes.ui.dashboard.IControls.Companion.CARD_VIEW
import com.smartsolutions.paquetes.ui.dashboard.IControls.Companion.HEADER
import com.smartsolutions.paquetes.ui.dashboard.IControls.Companion.SUMMARY

@Keep
class FirewallControls(
    private val activity: DashboardControlActivity
) : IControls {

    private val binding = FirewallControlsBinding
        .inflate(activity.layoutInflater)

    private val viewModel = activity.viewModel

    override fun getRoot(): View {
        return binding.root
    }

    override fun init() {
        setTransitionNames()

        activity.window.sharedElementEnterTransition
            ?.addListener(object : Transition.TransitionListener {
                override fun onTransitionStart(transition: Transition?) {

                }

                override fun onTransitionEnd(transition: Transition?) {
                    transition?.removeListener(this)

                    viewModel.setFirewallSwitchListener(binding.firewall, activity.supportFragmentManager)
                    viewModel.setFirewallDynamicModeListener(
                        binding.dynamicMode,
                        binding.staticMode,
                        activity.supportFragmentManager
                    )

                    viewModel.appsData.observe(activity) {
                        binding.allowedAppsValue.text = it[0].toString()
                        binding.blockedAppsValue.text = it[1].toString()
                        binding.allAppsValue.text = it[2].toString()
                    }
                }

                override fun onTransitionCancel(transition: Transition?) {
                    transition?.removeListener(this)
                }

                override fun onTransitionPause(transition: Transition?) {
                }

                override fun onTransitionResume(transition: Transition?) {
                }

            })
        binding.root.setOnClickListener {
            activity.onBackPressed()
        }
        binding.firewallControls.setOnClickListener {
            //Empty
        }
    }

    override fun onBackPressed() {
        binding.allowedApps.visibility = View.INVISIBLE
        binding.blockedApps.visibility = View.INVISIBLE
        binding.allApps.visibility = View.INVISIBLE
        binding.allowedAppsValue.visibility = View.INVISIBLE
        binding.blockedAppsValue.visibility = View.INVISIBLE
        binding.allowedAppsValue.visibility = View.INVISIBLE
    }

    private fun setTransitionNames() {
        ViewCompat.setTransitionName(binding.firewallControls, CARD_VIEW)
        ViewCompat.setTransitionName(binding.firewallHeader, HEADER)
        ViewCompat.setTransitionName(binding.firewall, SWITCH)
        ViewCompat.setTransitionName(binding.firewallSummary, SUMMARY)
    }

    companion object {
        fun getLaunchIntent(context: Context): Intent {
            return Intent(context, DashboardControlActivity::class.java)
                .putExtra(
                    DashboardControlActivity.EXTRA_CONTROLS_CLASS_NAME,
                    FirewallControls::class.java.name
                )
        }
    }
}