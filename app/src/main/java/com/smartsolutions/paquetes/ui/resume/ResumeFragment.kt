package com.smartsolutions.paquetes.ui.resume

import android.graphics.drawable.Animatable
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.viewModels
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.android.material.tabs.TabLayoutMediator
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentResumeBinding
import com.smartsolutions.paquetes.databinding.PopupMenuTabBinding
import com.smartsolutions.paquetes.databinding.TabItemBinding
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.managers.PermissionsManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.models.Permission
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.ui.BottomSheetDialogBasic
import com.smartsolutions.paquetes.ui.permissions.SinglePermissionFragment
import com.smartsolutions.paquetes.ui.permissions.StartAccessibilityServiceFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ResumeFragment : Fragment(), ResumeViewModel.SynchronizationResult {

    private val viewModel by viewModels<ResumeViewModel>()

    private lateinit var binding: FragmentResumeBinding
    private var adapterFragment: FragmentPageAdapter? = null
    private lateinit var installedSims: List<Sim>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentResumeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getInstalledSims().observe(viewLifecycleOwner) {
            installedSims = it

            setAdapter(it)
            setTabLayoutMediator()

            //TODO comentado temporal hasta que termine de probar
            /*if (it.size == 1){
                binding.tabs.visibility = View.GONE
            }else {
                binding.tabs.visibility = View.VISIBLE
            }*/
        }

        binding.floatingActionButton.setOnClickListener {
            if (installedSims[binding.pager.currentItem].defaultVoice) {
                animateFAB()
                viewModel.synchronizeUserDataBytes(this)
            } else {
                val fragment =
                    BottomSheetDialogBasic.newInstance(BottomSheetDialogBasic.DialogType.SYNCHRONIZATION_FAILED_NOT_DEFAULT_SIM)
                fragment.show(this.childFragmentManager, "BasicDialog")
            }
        }
    }


    private fun setAdapter(sims: List<Sim>) {
        if (adapterFragment == null) {
            adapterFragment = FragmentPageAdapter(this, sims)
            binding.pager.adapter = adapterFragment
        } else {
            adapterFragment!!.sims = sims
            adapterFragment!!.notifyDataSetChanged()
        }
    }

    private fun setTabLayoutMediator() {
        try {
            TabLayoutMediator(binding.tabs, binding.pager) { tab, pos ->
                val tabBind =
                    TabItemBinding.inflate(LayoutInflater.from(requireContext()), null, false)
                val sim = installedSims[pos]

                sim.icon?.let {
                    tabBind.icon.setImageBitmap(it)
                }

                tabBind.title.text = "Sim ${sim.slotIndex + 1}"
                tabBind.subtitle.text = if (sim.defaultVoice && sim.defaultData) {
                    "Voz y Datos"
                } else if (sim.defaultData) {
                    "Datos"
                } else if (sim.defaultVoice) {
                    "Voz"
                } else {
                    tabBind.subtitle.visibility = View.GONE
                    ""
                }

                tab.setCustomView(tabBind.root)

                tab.view.setOnLongClickListener {
                    showTabMenu(sim, it)
                    true
                }
            }.also {
                if (!it.isAttached) {
                    it.attach()
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun showTabMenu(sim: Sim, view: View) {
        val popupMenu = PopupWindow(requireContext())

        val menuBind = PopupMenuTabBinding.inflate(
            LayoutInflater.from(requireContext()),
            null,
            false
        )

        menuBind.radioButtonDataDefault.apply {
            isChecked = sim.defaultData
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                isEnabled = false
            }
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked)
                    viewModel.setDefaultSim(SimDelegate.SimType.DATA, sim)
            }
        }
        menuBind.radioButtonVoiceDefault.apply {
            isChecked = sim.defaultVoice
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                isEnabled = false
            }
            setOnCheckedChangeListener { _, isCheked ->
                if (isCheked)
                    viewModel.setDefaultSim(SimDelegate.SimType.VOICE, sim)
            }
        }

        popupMenu.contentView = menuBind.root
        popupMenu.isOutsideTouchable = true

        popupMenu.showAsDropDown(view)
    }

    private fun animateFAB(){
        binding.floatingActionButton.shrink()
    }


    override fun onSuccess() {
        binding.floatingActionButton.extend()
        Toast.makeText(requireContext(), "Sincronizado", Toast.LENGTH_SHORT).show()
    }

    override fun onCallPermissionsDenied() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val fragment = SinglePermissionFragment.newInstance(IPermissionsManager.CALL_CODE)
            fragment.show(this.childFragmentManager, "PermissionsFragment")
        }
    }

    override fun onUSSDFail(message: String) {
        val fragment = BottomSheetDialogBasic.newInstance(
            BottomSheetDialogBasic.DialogType.SYNCHRONIZATION_FAILED,
            message = message
        )
        fragment.show(this.childFragmentManager, "BasicDialog")
    }

    override fun onFailed(throwable: Throwable?) {
        val fragment =
            BottomSheetDialogBasic.newInstance(BottomSheetDialogBasic.DialogType.SYNCHRONIZATION_FAILED)
        fragment.show(this.childFragmentManager, "BasicDialog")
    }

    override fun onAccessibilityServiceDisabled() {
        val fragment = StartAccessibilityServiceFragment.newInstance()
        fragment.show(this.childFragmentManager, "AccessibilityFragment")
    }


    companion object {
        fun newInstance() = ResumeFragment()
    }
}