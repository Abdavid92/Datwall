package com.smartsolutions.paquetes.ui.resume

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayoutMediator
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentResumeBinding
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ResumeFragment : Fragment(), ResumeViewModel.SynchronizationResult {

    private val viewModel by viewModels<ResumeViewModel> ()

    private lateinit var binding: FragmentResumeBinding
    private var adapterFragment: FragmentPageAdapter? = null
    private lateinit var installedSim: List<Sim>


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
            installedSim = it

            setAdapter(it)

            try {
                TabLayoutMediator(binding.tabs, binding.pager) { tab, pos ->
                    it[pos].icon?.let {
                        tab.icon = it.toDrawable(resources)
                    }
                    tab.text = "Sim ${it[pos].slotIndex}"
                }.also {
                    if (!it.isAttached) {
                        it.attach()
                    } else {
                        it.detach()
                        it.attach()
                    }
                }
            }catch (e: Exception){}

            if (it.size == 1){
                binding.tabs.visibility = View.GONE
            }else {
                binding.tabs.visibility = View.VISIBLE
            }
        }

        binding.floatingActionButton.setOnClickListener {
            showSynchronizationDialog()
            viewModel.synchronizeUserDataBytes(
                this
            )
        }
    }


    private fun setAdapter(sims: List<Sim>) {
        if (adapterFragment == null){
            adapterFragment = FragmentPageAdapter(this, sims)
            binding.pager.adapter = adapterFragment
        }else {
            adapterFragment!!.sims = sims
            adapterFragment!!.notifyDataSetChanged()
        }
    }



    private fun showSynchronizationDialog(){

    }


    override fun onSuccess() {
        Toast.makeText(requireContext(), "Sincronizado", Toast.LENGTH_SHORT).show()
    }

    override fun onCallPermissionsDenied() {

    }

    override fun onUSSDFail(message: String) {

    }

    override fun onFailed(throwable: Throwable?) {

    }

    override fun onAccessibilityServiceDisabled() {

    }


    companion object {
        fun newInstance() = ResumeFragment()
    }
}