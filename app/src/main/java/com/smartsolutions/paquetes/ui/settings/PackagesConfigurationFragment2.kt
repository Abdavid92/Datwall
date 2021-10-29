package com.smartsolutions.paquetes.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.databinding.FragmentPackagesConfiguration2Binding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PackagesConfigurationFragment2 @Inject constructor(

) : AbstractSettingsFragment() {

    private val viewModel by viewModels<PackagesConfigurationViewModel2>()

    private var _binding: FragmentPackagesConfiguration2Binding? = null
    private val binding: FragmentPackagesConfiguration2Binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPackagesConfiguration2Binding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance() = PackagesConfigurationFragment2()
    }
}