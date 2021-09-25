package com.smartsolutions.paquetes.ui.packages

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentPackagesBinding

class PackagesFragment : Fragment() {

    companion object {
        fun newInstance() = PackagesFragment()
    }

    private val viewModel by viewModels<PackagesViewModel> ()

    private lateinit var binding: FragmentPackagesBinding
    

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPackagesBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getPackagesAndSims().observe(viewLifecycleOwner){

        }

    }



}