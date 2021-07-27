package com.smartsolutions.paquetes.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.databinding.FragmentPackagesConfigurationBinding
import com.smartsolutions.paquetes.exceptions.USSDRequestException
import com.smartsolutions.paquetes.serverApis.models.Result
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PackagesConfigurationFragment :
    AbstractSettingsFragment() {

    private val viewModel by viewModels<PackagesConfigurationViewModel>()

    private lateinit var binding: FragmentPackagesConfigurationBinding

    override fun isRequired(): Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPackagesConfigurationBinding
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
                        listener,
                        childFragmentManager
                    )
                } else {
                    val network = when (binding.simNetwork.selectedItemPosition) {
                        1 -> Networks.NETWORK_3G_4G
                        2 -> Networks.NETWORK_3G
                        3 -> Networks.NETWORK_4G
                        else -> Networks.NETWORK_NONE
                    }

                    if (network != Networks.NETWORK_NONE) {
                        viewModel.setManualConfiguration(network)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            R.string.invalid_network_option,
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }

        binding.manualMode.setOnCheckedChangeListener(this::manualModeChange)

        viewModel.configurationResult.observe(viewLifecycleOwner) {

            if (it.isSuccess) {

                binding.network = it.getOrThrow().network

                binding.resultMsg.text = when (it.getOrThrow().network) {
                    Networks.NETWORK_3G -> {
                        binding.simNetwork.setSelection(2, true)
                        "Paquetes para la red 3G disponibles para esta linea"
                    }
                    Networks.NETWORK_3G_4G -> {
                        binding.simNetwork.setSelection(1, true)
                        "Todos los paquetes disponibles para esta linea"
                    }
                    Networks.NETWORK_4G -> {
                        binding.simNetwork.setSelection(3, true)
                        "Paquetes para la red 4G disponibles para esta linea"
                    }
                    Networks.NETWORK_NONE -> {
                        "No se pudo encontrar ningún paquete para esta linea"
                    }
                    else -> {
                        "No se pudo encontrar ningún paquete para esta linea"
                    }
                }
            } else {
                val message = ((it as Result.Failure).throwable as USSDRequestException).message

                binding.resultMsg.text = message
            }
        }
    }

    /**
     * Cambia la variable network para que el botón de aplicar los cambios se abilite
     * cuando se cambia al modo automático.
     * */
    private fun manualModeChange(buttonView: CompoundButton, isChecked: Boolean) {
        if (!isChecked)
            binding.network = Networks.NETWORK_NONE
    }

    companion object {
        fun newInstance() = PackagesConfigurationFragment()
    }
}