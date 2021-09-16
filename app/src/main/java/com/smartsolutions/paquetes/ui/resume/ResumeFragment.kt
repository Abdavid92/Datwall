package com.smartsolutions.paquetes.ui.resume

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayoutMediator
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentResumeBinding
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ResumeFragment : Fragment() {

    private val viewModel by viewModels<ResumeViewModel> ()

    private lateinit var binding: FragmentResumeBinding
    private var adapterFragment: FragmentPageAdapter? = null


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

            setAdapter(it)

            TabLayoutMediator(binding.tabs, binding.pager) { tab, pos ->
                it[pos].icon?.let {
                    tab.icon = it.toDrawable(resources)
                }
                tab.text = "Sim ${it[pos].slotIndex}"
            }.attach()

            if (it.size == 1){
                binding.tabs.visibility = View.GONE
            }else {
                binding.tabs.visibility = View.VISIBLE
            }
        }

        binding.floatingActionButton.setOnClickListener {
            if (binding.floatingActionButton.isExtended){
                binding.floatingActionButton.shrink()
            }else {
                binding.floatingActionButton.extend()
            }
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

    companion object {
        fun newInstance() = ResumeFragment()
    }
}