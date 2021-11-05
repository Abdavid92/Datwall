package com.smartsolutions.paquetes.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.databinding.FragmentPackagesConfigurationBinding
import com.smartsolutions.paquetes.ui.settings.sim.DefaultSimsDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PackagesConfigurationFragment @Inject constructor(

) : AbstractSettingsFragment() {

    private val viewModel by viewModels<PackagesConfigurationViewModel>()

    private var _binding: FragmentPackagesConfigurationBinding? = null
    private val binding get() = _binding!!

    /**
     * Indica si es obligatorio configurar los paquetes. De ser así el fragmento no
     * notificará su finalización hasta que se configuren los paquetes correctamente.
     * Esta variable se guarda en el fragmento y no en el viewModel porque su valor se recoje de
     * la propiedad [getArguments] que sobrevive al ciclo de vida del fragmento.
     * */
    private var configurationRequired: Boolean = DEFAULT_CONFIGURATION_REQUIRED

    private var defaultSimId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configurationRequired = arguments?.getBoolean(EXTRA_CONFIGURATION_REQUIRED) ?: configurationRequired
        defaultSimId = arguments?.getString(EXTRA_DEFAULT_SIM_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPackagesConfigurationBinding.inflate(
            inflater,
            container,
            false
        ).apply {
            /*Esta propiedad es el eje central del modo de configuración de los paquetes.*/
            automatic = true
            network = viewModel.lastNetworkResult
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prepareSimSelection()
        preparePackagesConfigurations()
        prepareManualPackageConfigurations()

        binding.btnContinue.setOnClickListener {

            it.isClickable = false

            complete()
        }
    }

    override fun complete() {

        if (configurationRequired) {

            if (viewModel.lastNetworkResult != Networks.NETWORK_NONE)
                super.complete()
            else
                Toast.makeText(
                    requireContext(),
                    R.string.package_configuration_required,
                    Toast.LENGTH_SHORT)
                    .show()
        } else {
            super.complete()
        }
    }

    private fun prepareManualPackageConfigurations() {

        binding.spinnerPackages.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                if (binding.automatic == false) {

                    val network = when (position) {
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
                            Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        binding.radioGroupMode.setOnCheckedChangeListener { group, checkedId ->

            if (checkedId == R.id.manual_mode) {
                binding.nestedScroll.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
            }
        }
    }

    private fun preparePackagesConfigurations() {

        viewModel.configurationResult.observe(viewLifecycleOwner) {

            val message = if (it.isSuccess) {

                val network = it.getOrThrow().network

                if (network != Networks.NETWORK_NONE)
                    binding.network = network

                val none = "No se pudo encontrar ningún plan ni paquete para esta linea. " +
                        "Si cree que esto fue un error inténtelo de nuevo."

                val spinnerPackagesIndex: Int

                val resultText = when (network) {
                    Networks.NETWORK_3G -> {
                        spinnerPackagesIndex = 2
                        "Solo los planes combinados disponibles para esta linea"
                    }
                    Networks.NETWORK_4G -> {
                        spinnerPackagesIndex = 3
                        "Solo los paquetes de la 4G disponibles para esta linea"
                    }
                    Networks.NETWORK_3G_4G -> {
                        spinnerPackagesIndex = 1
                        "Todos los planes y paquetes disponibles para esta linea"
                    }
                    else -> {
                        spinnerPackagesIndex = 0
                        none
                    }
                }

                if (binding.automatic == true) {
                    binding.cardManualOptions.visibility = View.VISIBLE
                    binding.spinnerPackages.setSelection(spinnerPackagesIndex, true)
                }

                resultText
            } else {
                it.getThrowableOrNull()?.message
            }

            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun prepareSimSelection() {

        /*Si hay varias sims preparo el spinner de selección de sims*/
        if (viewModel.isSeveralSimsInstalled()) {

            viewModel.getSims(this, childFragmentManager).observe(viewLifecycleOwner) { simsList ->

                binding.apply {
                    sims.adapter = SimsAdapter(simsList)

                    sims.onItemSelectedListener = null

                    var simIndex: Int? = null

                    simsList.firstOrNull { sim -> sim.id == defaultSimId }?.let { sim ->
                        simIndex = simsList.indexOf(sim)
                    }

                    sims.setSelection(simIndex ?: simsList.indexOf(simsList.find { sim -> sim.defaultVoice }))

                    sims.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {

                            if (!simsList[position].defaultVoice) {
                                DefaultSimsDialogFragment
                                    .newInstance(DefaultSimsDialogFragment.FailDefault.DEFAULT_VOICE)
                                    .show(childFragmentManager, null)
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {

                        }
                    }

                    btnStartConfiguration.setOnClickListener {

                        val selection = sims.selectedItemPosition

                        if (!simsList[selection].defaultVoice) {

                            DefaultSimsDialogFragment
                                .newInstance(DefaultSimsDialogFragment.FailDefault.DEFAULT_VOICE)
                                .show(childFragmentManager, null)
                        } else {

                            viewModel.configureDataPackages(
                                this@PackagesConfigurationFragment,
                                childFragmentManager
                            )
                        }
                    }
                }
            }
        } else {

            binding.apply {

                //Sino oculto la tarjeta de selección de sims
                cardSimSelection.visibility = View.GONE

                btnStartConfiguration.setOnClickListener {

                    viewModel.configureDataPackages(
                        this@PackagesConfigurationFragment,
                        childFragmentManager
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {

        const val EXTRA_CONFIGURATION_REQUIRED = "com.smartsolutions.paquetes.extra.CONFIGURATION_REQUIRED"

        const val EXTRA_DEFAULT_SIM_ID = "com.smartsolutions.paquetes.extra.DEFAUL_SIM_ID"

        private const val DEFAULT_CONFIGURATION_REQUIRED = true

        /**
         * Crea una nueva instancia de [PackagesConfigurationFragment].
         *
         * @param configurationRequired - Indica si es obligatorio la configuración
         * de los paquetes. De ser así el fragmento no notificará la finalización hasta que
         * no se configuren correctamente los paquetes.
         * */
        fun newInstance(
            configurationRequired: Boolean = DEFAULT_CONFIGURATION_REQUIRED
        ): PackagesConfigurationFragment {

            val args = Bundle().apply {
                putBoolean(EXTRA_CONFIGURATION_REQUIRED, configurationRequired)
            }

            return PackagesConfigurationFragment().apply {
                arguments = args
            }
        }
    }
}