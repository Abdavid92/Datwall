package com.smartsolutions.paquetes.ui.applications

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.databinding.FragmentApplicationsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ApplicationsFragment : Fragment() {

    private lateinit var binding: FragmentApplicationsBinding

    private val viewModel by viewModels<ApplicationsViewModel>()

    private var filter = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentApplicationsBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sectionsAdapter = SectionsPagerAdapter(requireContext(), childFragmentManager)
        binding.viewPager.adapter = sectionsAdapter

        binding.tabs.setupWithViewPager(binding.viewPager)

        binding.btnFilter.setOnClickListener {
            if (filter >= 2)
                filter = 0
            else
                filter += 1
            viewModel.setFilter(AppsFilter.values()[filter])
        }
    }
}