package com.smartsolutions.paquetes.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.databinding.FragmentPackagesConfigurationBinding
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PackagesConfigurationFragment : AbstractSettingsFragment() {

    private val viewModel by viewModels<PackagesConfigurationViewModel>()

    private var _binding: FragmentPackagesConfigurationBinding? = null
    private val binding get() = _binding!!

    private lateinit var installedSims: List<Sim>

    private var simID: String? = null

    /**
     * Indica si es obligatorio configurar los paquetes. De ser así el fragmento no
     * notificará su finalización hasta que se configuren los paquetes correctamente.
     * Esta variable se guarda en el fragmento y no en el viewModel porque su valor se recoje de
     * la propiedad [getArguments] que sobrevive al ciclo de vida del fragmento.
     * */
    private var configurationRequired: Boolean =
        DEFAULT_CONFIGURATION_REQUIRED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configurationRequired =
            arguments?.getBoolean(EXTRA_CONFIGURATION_REQUIRED) ?: configurationRequired
        simID = arguments?.getString(EXTRA_DEFAULT_SIM_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPackagesConfigurationBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.geInstalledSims().observe(viewLifecycleOwner) {
            installedSims = it
            binding.cardSimSelection.visibility = if (it.size > 1) {
                View.VISIBLE
            } else {
                View.GONE
            }
            setAdapterSpinner(it)

            showPackagesAvailable(installedSims[binding.sims.selectedItemPosition].network)
        }

        binding.apply {

            sims.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    kotlin.runCatching {
                        showPackagesAvailable(installedSims[position].network)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            btnStartConfiguration.setOnClickListener {
                when (radioGroupMode.checkedRadioButtonId) {
                    R.id.automatic_mode -> {
                        viewModel.invokeOnDefaultSim(
                            requireContext(),
                            installedSims[sims.selectedItemPosition],
                            SimDelegate.SimType.VOICE,
                            parentFragmentManager
                        ) {
                            viewModel.configureAutomaticPackages(
                                this@PackagesConfigurationFragment,
                                parentFragmentManager
                            )
                        }
                    }
                    R.id.manual_mode -> {
                        showDialogManualMode()
                    }
                    else -> {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.must_have_select),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            btnContinue.setOnClickListener {
                complete()
            }
        }
    }


    private fun setAdapterSpinner(list: List<Sim>) {
        val simsNames = mutableListOf<String>()

        list.forEach {
            simsNames.add(it.name())
        }

        binding.apply {
            val position = if (simID != null) {
                list.firstOrNull { it.id == simID }?.let {
                    return@let list.indexOf(it)
                } ?: sims.selectedItemPosition
            } else {
                sims.selectedItemPosition
            }
            simID = null
            sims.adapter =
                ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, simsNames)
            if (position in simsNames.indices) {
                sims.setSelection(position)
            }
        }
    }

    private fun showDialogManualMode() {
        AlertDialog.Builder(requireContext())
            .setTitle(
                getString(
                    R.string.select_packages_available,
                    installedSims[binding.sims.selectedItemPosition].name()
                )
            )
            .setItems(
                R.array.sim_networks
            ) { _, pos ->
                viewModel.configureManualPackages(
                    when (pos) {
                        0 -> Networks.NETWORK_3G_4G
                        1 -> Networks.NETWORK_3G
                        2 -> Networks.NETWORK_4G
                        else -> Networks.NETWORK_NONE
                    },
                    installedSims[binding.sims.selectedItemPosition]
                )
            }.show()
    }

    private fun showPackagesAvailable(@Networks network: String) {
        binding.apply {
            cardManualOptions.visibility = if (network != Networks.NETWORK_NONE) {
                View.VISIBLE
            } else {
                View.GONE
            }

            packagesAvailable.text = getString(
                when (network) {
                    Networks.NETWORK_3G_4G -> {
                        R.string.all_packages_plan_available
                    }
                    Networks.NETWORK_4G -> {
                        R.string.packages_avaible
                    }
                    Networks.NETWORK_3G -> {
                        R.string.plan_avalaible
                    }
                    else -> {
                        R.string.pkgs_not_configured
                    }
                }
            )
        }
    }


    override fun complete() {
        if (configurationRequired) {
            kotlin.runCatching {
                val simDefault =
                    installedSims.first { viewModel.isDefaultSim(it, SimDelegate.SimType.VOICE)!! }
                if (simDefault.network != Networks.NETWORK_NONE) {
                    super.complete()
                    return@runCatching
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Es necesario configurar la ${simDefault.name()} porque es la Predeterminada de Llamadas",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@runCatching
                }
            }.onFailure {
                throw IllegalStateException("No existe Sim Default")
            }
        } else {
            super.complete()
        }
    }

    companion object {

        private const val DEFAULT_CONFIGURATION_REQUIRED = true
        const val EXTRA_CONFIGURATION_REQUIRED =
            "com.smartsolutions.paquetes.extra.CONFIGURATION_REQUIRED"
        const val EXTRA_DEFAULT_SIM_ID = "default_sim_id"

        fun newInstance(
            configurationRequired: Boolean = DEFAULT_CONFIGURATION_REQUIRED
        ) = PackagesConfigurationFragment().apply {
            arguments?.apply {
                putBoolean(EXTRA_CONFIGURATION_REQUIRED, configurationRequired)
            }
        }
    }
}