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
import com.smartsolutions.paquetes.databinding.BubbleControlsBinding

class BubbleControlsFragment : Fragment() {

    private var _binding: BubbleControlsBinding? = null
    private val binding: BubbleControlsBinding
        get() = _binding!!

    private val viewModel by activityViewModels<DashboardViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = BubbleControlsBinding.inflate(
            inflater,
            container,
            false
        )

        setTransitionNames()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (sharedElementEnterTransition as Transition?)
            ?.addListener(object : Transition.TransitionListener {

                override fun onTransitionStart(transition: Transition?) {

                }

                override fun onTransitionEnd(transition: Transition?) {
                    transition?.removeListener(this)
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
            activity?.onBackPressed()
        }
    }

    private fun setTransitionNames() {

        binding.apply {
            ViewCompat.setTransitionName(bubbleControls, DashboardControlActivity.CARD_VIEW)
            ViewCompat.setTransitionName(header, DashboardControlActivity.HEADER)
            ViewCompat.setTransitionName(bubble, DashboardControlActivity.SWITCH)
            ViewCompat.setTransitionName(bubbleSummary, DashboardControlActivity.SUMMARY)
        }
    }

    override fun onDetach() {
        super.onDetach()
        _binding = null
    }

    companion object {

        fun getLaunchIntent(context: Context): Intent {
            return Intent(context, DashboardControlActivity::class.java)
                .putExtra(
                    DashboardControlActivity.EXTRA_CONTROLS_CLASS_NAME,
                    BubbleControlsFragment::class.java.name
                )
        }
    }
}