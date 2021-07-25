package com.smartsolutions.paquetes.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.exceptions.USSDRequestException
import com.smartsolutions.paquetes.serverApis.models.Result
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PackagesConfigurationFragment :
    AbstractSettingsFragment(R.layout.fragment_packages_configuration) {

    private val viewModel by viewModels<PackagesConfigurationViewModel>()

    override fun isRequired(): Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val startConfiguration = view.findViewById<Button>(R.id.btn_start_configuration)

            startConfiguration.setOnClickListener {
                viewModel.configureDataPackages(
                    listener,
                    childFragmentManager
                )
            }

        viewModel.configurationResult.observe(viewLifecycleOwner) {

            val resultMsg = view.findViewById<TextView>(R.id.result_msg)

            if (it.isSuccess) {

                resultMsg.text = when (it.getOrThrow().network) {
                    Networks.NETWORK_3G -> {
                        startConfiguration.isEnabled = false
                        "Paquetes para la red 3G disponibles para esta linea"
                    }
                    Networks.NETWORK_3G_4G -> {
                        startConfiguration.isEnabled = false
                        "Todos los paquetes disponibles para esta linea"
                    }
                    Networks.NETWORK_4G -> {
                        startConfiguration.isEnabled = false
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

                resultMsg.text = message
            }
        }
    }

    companion object {
        fun newInstance() = PackagesConfigurationFragment()
    }
}