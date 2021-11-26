package com.smartsolutions.paquetes.ui.resume

import android.animation.Animator
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentResumeBinding
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.helpers.setTabLayoutMediatorSims
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.ui.AbstractFragment
import com.smartsolutions.paquetes.ui.BottomSheetDialogBasic
import com.smartsolutions.paquetes.ui.permissions.SinglePermissionFragment
import com.smartsolutions.paquetes.ui.permissions.StartAccessibilityServiceFragment
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ResumeFragment : AbstractFragment(), ResumeViewModel.SynchronizationResult {

    private val viewModel by viewModels<ResumeViewModel>()

    private var _binding: FragmentResumeBinding? = null
    private val binding: FragmentResumeBinding
        get() = _binding!!

    private var adapterFragment: ResumePagerAdapter? = null
    private lateinit var installedSims: List<Sim>

    private val rotateAnimation = RotateAnimation(
        360f,
        0f,
        Animation.RELATIVE_TO_SELF,
        0.5f,
        Animation.RELATIVE_TO_SELF,
        0.5f
    ).apply {
        fillAfter = true
        duration = 1000L
        repeatMode = Animation.RESTART
        repeatCount = 30
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (canWork()) {
            _binding = FragmentResumeBinding.inflate(inflater, container, false)
            return binding.root
        }

        return inflatePurchasedFunctionLayout(inflater, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!canWork())
            return

        viewModel.getInstalledSims().observe(viewLifecycleOwner) {
            installedSims = it

            setAdapter(it)

            setTabLayoutMediatorSims(
                requireContext(),
                binding.tabs,
                binding.pager,
                it,
                childFragmentManager
            )

            if (it.size <= 1) {
                binding.tabs.visibility = View.GONE
            } else {
                binding.tabs.visibility = View.VISIBLE
            }
        }

        configureAnimationFAB()

        binding.floatingActionButton.setOnClickListener {
            viewModel.invokeOnDefaultSim(
                requireContext(),
                installedSims[binding.pager.currentItem],
                SimDelegate.SimType.VOICE,
                parentFragmentManager
            ){
                animateFAB(true)
                viewModel.synchronizeUserDataBytes(this)
            }
        }

        binding.floatingActionButton.setOnLongClickListener {
            kotlin.runCatching {
                Toast.makeText(
                    requireContext(),
                    "Actualizado:\n${
                        SimpleDateFormat("dd/MM/yyyy hh:mm:ss aa", Locale.getDefault()).format(
                            Date(installedSims[binding.pager.currentItem].lastSynchronization)
                        )
                    }",
                    Toast.LENGTH_SHORT
                ).show()
            }

            return@setOnLongClickListener true
        }

        binding.buttonFilter.setOnClickListener {
            showFilterOptions()
        }

        binding.buttonAdd.setOnClickListener {
            EditAddUserDataBytesFragment.newInstance(
                installedSims[binding.pager.currentItem].id,
                false,
                null
            ).show(childFragmentManager, null)
        }

        binding.buttonChart.setOnClickListener {
            showChartUsageGeneral()
        }
    }

    override fun onPause() {
        super.onPause()
        adapterFragment = null
    }

    private fun showFilterOptions() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_filter_title)
            .setItems(R.array.filter_resume) { _, pos ->
                viewModel.setFilter(ResumeViewModel.FilterUserDataBytes.values()[pos])
            }.show()
    }

    private fun showChartUsageGeneral() {
        UsageGeneralFragment.newInstance(
            installedSims[binding.tabs.selectedTabPosition].id
        ).show(childFragmentManager, null)
    }

    private fun configureAnimationFAB() {
        binding.floatingActionButton.apply {

            addOnExtendAnimationListener(object :
                Animator.AnimatorListener {

                override fun onAnimationStart(anim: Animator?) {
                    binding.floatingActionButton.apply {
                        animation?.cancel()
                        animation?.reset()
                    }
                }

                override fun onAnimationEnd(anim: Animator?) {}
                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationRepeat(animation: Animator?) {}

            })


            addOnShrinkAnimationListener(object :
                Animator.AnimatorListener {

                override fun onAnimationStart(animation: Animator?) {}

                override fun onAnimationEnd(anim: Animator?) {
                    binding.floatingActionButton.apply {
                        animation = rotateAnimation
                        animation?.start()
                    }
                }

                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationRepeat(animation: Animator?) {}

            })
        }
    }

    private fun setAdapter(sims: List<Sim>) {
        if (adapterFragment == null) {
            adapterFragment = ResumePagerAdapter(this, sims)
            binding.pager.adapter = adapterFragment
        } else {
            adapterFragment?.sims = sims
            adapterFragment?.notifyDataSetChanged()
        }
    }


    private fun animateFAB(animate: Boolean) {
        if (animate) {
            binding.floatingActionButton.apply {
                shrink()
            }
        } else {
            binding.floatingActionButton.apply {
                extend()
            }
        }
    }

    override fun onSuccess() {
        animateFAB(false)
    }

    override fun onCallPermissionsDenied() {
        animateFAB(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val fragment = SinglePermissionFragment.newInstance(IPermissionsManager.CALL_CODE)
            fragment.show(this.childFragmentManager, "PermissionsFragment")
        }
    }

    override fun onUSSDFail(message: String) {
        animateFAB(false)
        val fragment = BottomSheetDialogBasic.newInstance(
            BottomSheetDialogBasic.DialogType.SYNCHRONIZATION_FAILED,
            message = message
        )
        fragment.show(this.childFragmentManager, "BasicDialog")
    }

    override fun onFailed(throwable: Throwable?) {
        animateFAB(false)
        val fragment =
            BottomSheetDialogBasic.newInstance(BottomSheetDialogBasic.DialogType.SYNCHRONIZATION_FAILED)
        fragment.show(this.childFragmentManager, "BasicDialog")
    }

    override fun onAccessibilityServiceDisabled() {
        animateFAB(false)
        val fragment = StartAccessibilityServiceFragment.newInstance()
        fragment.show(this.childFragmentManager, "AccessibilityFragment")
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        adapterFragment = null
    }

    companion object {
        fun newInstance() = ResumeFragment()
    }
}