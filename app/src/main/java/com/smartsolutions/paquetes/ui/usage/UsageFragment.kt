package com.smartsolutions.paquetes.ui.usage

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayoutMediator
import com.smartsolutions.paquetes.databinding.FragmentUsageBinding

class UsageFragment : Fragment() {

    companion object {
        fun newInstance() = UsageFragment()
    }

    private val viewModel by viewModels<UsageViewModel> ()
    private lateinit var binding: FragmentUsageBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUsageBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = FragmentPageAdapter(requireContext(), this)
        binding.pager.adapter = adapter

        TabLayoutMediator(binding.tabs, binding.pager){ tab, position ->
            tab.text = adapter.fragmentsList[position].second
        }.attach()

    }

}