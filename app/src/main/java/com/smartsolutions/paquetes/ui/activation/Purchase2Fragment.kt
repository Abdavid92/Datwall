package com.smartsolutions.paquetes.ui.activation

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentPurchase2Binding
import com.smartsolutions.paquetes.ui.settings.AbstractSettingsFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Purchase2Fragment : Fragment() {

    private val viewModel: Purchase2ViewModel by viewModels ()
    private var _binding: FragmentPurchase2Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPurchase2Binding.inflate(layoutInflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


    companion object {
        fun newInstance() = Purchase2Fragment()
    }

}