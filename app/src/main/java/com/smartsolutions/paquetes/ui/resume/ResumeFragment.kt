package com.smartsolutions.paquetes.ui.resume

import android.animation.Animator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.google.android.material.tabs.TabLayoutMediator
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentResumeBinding
import com.smartsolutions.paquetes.ui.setTabLayoutMediatorSims
import com.smartsolutions.paquetes.managers.models.UserDataOrder
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.ui.AbstractFragment
import com.smartsolutions.paquetes.ui.BottomSheetDialogBasic
import com.smartsolutions.paquetes.ui.ErrorTexts
import com.smartsolutions.paquetes.ui.permissions.onAccessibilityResult
import com.smartsolutions.paquetes.ui.permissions.onPermissionResult
import com.smartsolutions.paquetes.ui.permissions.showAccessibilitySheet
import com.smartsolutions.paquetes.ui.permissions.showPermissionSheet
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ResumeFragment : AbstractFragment() {

    private val viewModel by viewModels<ResumeViewModel>()

    private var _binding: FragmentResumeBinding? = null
    private val binding: FragmentResumeBinding
        get() = _binding!!

    private var adapterFragment: ResumePagerAdapter? = null
    private var installedSims: List<Sim> = emptyList()
    private var tabsMediator: TabLayoutMediator? = null

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Con el permiso concedido se continúa la sincronización que pidió el usuario.
        onPermissionResult { _, granted ->
            if (granted)
                viewModel.onPermissionGranted()
        }

        onAccessibilityResult { granted ->
            if (granted)
                viewModel.onPermissionGranted()
            else
                Toast.makeText(
                    requireContext(),
                    R.string.accessibility_sync_cancelled,
                    Toast.LENGTH_SHORT
                ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResumeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.map { it.sims }.distinctUntilChanged().collect(::renderSims)
                }
                launch {
                    viewModel.state.map { it.synchronizing }.distinctUntilChanged().collect(::animateFAB)
                }
                launch { viewModel.events.collect(::onEvent) }
            }
        }

        configureAnimationFAB()

        binding.floatingActionButton.setOnClickListener {
            synchronize()
        }

        binding.floatingActionButton.setOnLongClickListener {
            kotlin.runCatching {
                Toast.makeText(
                    requireContext(),
                    getString(
                        R.string.last_synchronization,
                        SimpleDateFormat("dd/MM/yyyy hh:mm:ss aa", Locale.getDefault()).format(
                            Date(installedSims[binding.pager.currentItem].lastSynchronization)
                        )
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }

            return@setOnLongClickListener true
        }

        binding.buttonFilter.setOnClickListener {
            showFilterOptions()
        }

        binding.buttonAdd.setOnClickListener {
            installedSims.getOrNull(binding.pager.currentItem)?.let {
                EditAddUserDataBytesFragment.newInstance(it.id, false, null)
                    .show(childFragmentManager, null)
            }
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
                viewModel.onFilterChanged(UserDataOrder.entries[pos])
            }.show()
    }

    private fun showChartUsageGeneral() {
        installedSims.getOrNull(binding.pager.currentItem)?.let {
            UsageGeneralFragment.newInstance(it.id).show(childFragmentManager, null)
        }
    }

    private fun configureAnimationFAB() {
        binding.floatingActionButton.apply {

            addOnExtendAnimationListener(object :
                Animator.AnimatorListener {

                override fun onAnimationStart(anim: Animator) {
                    binding.floatingActionButton.apply {
                        animation?.cancel()
                        animation?.reset()
                    }
                }

                override fun onAnimationEnd(anim: Animator) {}
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}

            })


            addOnShrinkAnimationListener(object :
                Animator.AnimatorListener {

                override fun onAnimationStart(animation: Animator) {}

                override fun onAnimationEnd(anim: Animator) {
                    binding.floatingActionButton.apply {
                        animation = rotateAnimation
                        animation?.start()
                    }
                }

                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}

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
        runCatching {

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
    }

    private fun renderSims(sims: List<Sim>) {
        // El estado empieza sin líneas mientras se leen: no hay nada que dibujar todavía.
        if (sims.isEmpty())
            return

        installedSims = sims

        // Las pestañas viejas siguen observando el adaptador: se sueltan antes de crear las nuevas.
        tabsMediator?.detach()

        setAdapter(sims)

        tabsMediator = setTabLayoutMediatorSims(requireContext(), binding.tabs, binding.pager, sims)

        binding.tabs.visibility = if (sims.size <= 1) View.GONE else View.VISIBLE
    }

    private fun synchronize() {
        installedSims.getOrNull(binding.pager.currentItem)?.let {
            viewModel.onSyncClicked(it.id)
        }
    }

    private fun onEvent(event: ResumeEvent) {
        when (event) {
            is ResumeEvent.RequestPermission -> showPermissionSheet(event.requestCode)

            ResumeEvent.RequestAccessibility -> showAccessibilitySheet()

            is ResumeEvent.SyncFailed -> BottomSheetDialogBasic.newInstance(
                BottomSheetDialogBasic.DialogType.SYNCHRONIZATION_FAILED,
                message = event.ussdErrorCode?.let { getString(ErrorTexts.ussd(it)) }
            ).show(childFragmentManager, "BasicDialog")
        }
    }

    override fun onDestroyView() {
        tabsMediator?.detach()
        tabsMediator = null
        super.onDestroyView()
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