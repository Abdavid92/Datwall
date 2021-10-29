package com.smartsolutions.paquetes.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.databinding.FragmentPackagesConfigurationBinding
import com.smartsolutions.paquetes.exceptions.USSDRequestException
import com.smartsolutions.paquetes.serverApis.models.Result
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PackagesConfigurationFragment @Inject constructor(

) : AbstractSettingsFragment() {

    private val viewModel by viewModels<PackagesConfigurationViewModel>()

    private var _binding: FragmentPackagesConfigurationBinding? = null
    private val binding: FragmentPackagesConfigurationBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPackagesConfigurationBinding
            .inflate(inflater, container, false).apply {
                automatic = true
                network = Networks.NETWORK_NONE
                executePendingBindings()
            }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStartConfiguration.setOnClickListener {
            if (binding.automatic == true) {
                viewModel.configureDataPackages(
                    this,
                    childFragmentManager
                )
            } else {
                val network = when (binding.simNetwork.selectedItemPosition) {
                    1 -> Networks.NETWORK_3G_4G
                    2 -> Networks.NETWORK_3G
                    else -> Networks.NETWORK_NONE
                }

                if (network != Networks.NETWORK_NONE) {
                    viewModel.setManualConfiguration(network)
                } else {
                    Toast.makeText(
                        requireContext(),
                        R.string.invalid_network_option,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.btnContinue.setOnClickListener {

            binding.btnContinue.isClickable = false

            complete()
        }

        viewModel.configurationResult.observe(viewLifecycleOwner) {

            if (it.isSuccess) {

                val network = it.getOrThrow().network

                if (network != Networks.NETWORK_NONE)
                    binding.network = network

                val none = "No se pudo encontrar ningún plan ni paquete para esta linea. " +
                        "Si cree que esto fue un error intentelo de nuevo."

                binding.resultMsg.text = when (network) {
                    Networks.NETWORK_3G_4G -> {
                        binding.simNetwork.setSelection(1, true)
                        "Todos los planes y paquetes disponibles para esta linea"
                    }
                    Networks.NETWORK_3G -> {
                        binding.simNetwork.setSelection(2, true)
                        "Solo los planes combinados disponibles para esta linea"
                    }
                    Networks.NETWORK_NONE -> {
                        none
                    }
                    else -> {
                        none
                    }
                }
            } else {
                val message = ((it as Result.Failure).throwable as USSDRequestException).message

                binding.resultMsg.text = message
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance() = PackagesConfigurationFragment()
    }
}