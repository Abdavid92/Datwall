package com.smartsolutions.paquetes.ui.dashboard

import android.content.Context
import android.content.Intent
import android.transition.Transition
import android.view.View
import androidx.annotation.Keep
import androidx.core.view.ViewCompat
import com.smartsolutions.paquetes.databinding.BubbleControlsBinding
import com.smartsolutions.paquetes.ui.dashboard.IControls.Companion.CARD_VIEW
import com.smartsolutions.paquetes.ui.dashboard.IControls.Companion.HEADER
import com.smartsolutions.paquetes.ui.dashboard.IControls.Companion.SUMMARY
import com.smartsolutions.paquetes.ui.dashboard.IControls.Companion.SWITCH

@Keep
class BubbleControls(
    private val activity: DashboardControlActivity
) : IControls {

    private val binding = BubbleControlsBinding
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
                viewModel.setBubbleSwitchListener(
                    binding.bubble,
                    activity.supportFragmentManager
                )
                viewModel.setBubbleAllWayListener(
                    binding.allWay,
                    binding.onlyConsume
                )
                viewModel.setTransparencyListener(
                    binding.bubbleTransparency,
                    binding.bubbleExample
                )
                viewModel.setSizeListener(
                    binding.bubbleSize,
                    binding.bubbleExample
                )
            }

            override fun onTransitionCancel(transition: Transition?) {
                transition?.removeListener(this)
            }

            override fun onTransitionPause(transition: Transition?) {
            }

            override fun onTransitionResume(transition: Transition?) {
            }

        })
        binding.bubbleControls.setOnClickListener {
            //Empty
        }
        binding.root.setOnClickListener {
            activity.onBackPressed()
        }
    }

    override fun onBackPressed() {

    }

    private fun setTransitionNames() {
        ViewCompat.setTransitionName(binding.bubbleControls, CARD_VIEW)
        ViewCompat.setTransitionName(binding.header, HEADER)
        ViewCompat.setTransitionName(binding.bubble, SWITCH)
        ViewCompat.setTransitionName(binding.bubbleSummary, SUMMARY)
    }

    companion object {
        fun getLaunchIntent(context: Context): Intent {
            return Intent(context, DashboardControlActivity::class.java)
                .putExtra(
                    DashboardControlActivity.EXTRA_CONTROLS_CLASS_NAME,
                    BubbleControls::class.java.name
                )
        }
    }
}