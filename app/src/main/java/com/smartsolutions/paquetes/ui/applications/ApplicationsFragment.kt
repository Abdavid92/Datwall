package com.smartsolutions.paquetes.ui.applications

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentApplicationsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ApplicationsFragment : Fragment() {

    private lateinit var binding: FragmentApplicationsBinding

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

        val sectionsAdapter = SectionsPagerAdapter(this)
        binding.viewPager.adapter = sectionsAdapter

        binding.tabs.setupWithViewPager(binding.viewPager)
    }

    companion object {
        fun newInstance() = ApplicationsFragment()
    }
}