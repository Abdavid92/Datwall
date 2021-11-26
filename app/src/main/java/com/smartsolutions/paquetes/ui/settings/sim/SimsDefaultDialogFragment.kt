package com.smartsolutions.paquetes.ui.settings.sim

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentSimsDefaultDialogBinding
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.serverApis.models.Result
import dagger.hilt.android.AndroidEntryPoint
import kotlin.IllegalArgumentException

private const val SIM_TYPE = "sim_type"

@AndroidEntryPoint
class SimsDefaultDialogFragment(
    private val onDefault: (sim: Sim) -> Unit,
    private val sim: Sim
) : BottomSheetDialogFragment() {

    private val viewModel: SimsDefaultDialogViewModel by viewModels()
    private lateinit var simType: SimDelegate.SimType

    private var _binding: FragmentSimsDefaultDialogBinding? = null

    private val binding
        get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        simType = SimDelegate.SimType.valueOf(
            arguments?.getString(SIM_TYPE) ?: throw IllegalArgumentException()
        )

        if (viewModel.onDefault == null)
            viewModel.onDefault = onDefault

        if (viewModel.sim == null)
            viewModel.sim = sim
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSimsDefaultDialogBinding.inflate(layoutInflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getDefaultSim(simType).observe(viewLifecycleOwner) { result ->

            //Si el resultado es Success significa que se pudo obtener la Sim
            //Sino o no hay Sim o no se pudo obtener la predeterminada
            if (result.isSuccess) {
                val sim = (result as Result.Success).value

                //Si la Sim es la predeterminada ejecuto la acción
                //Sino draw la vista segun el tipo
                if (sim.id == viewModel.sim?.id) {
                    dismiss()
                    viewModel.onDefault?.invoke(sim)
                } else {
                    binding.apply {
                        when (simType) {
                            SimDelegate.SimType.VOICE -> {
                                title.text = getString(R.string.no_sim_default_voice, sim.name())
                                imageType.setImageResource(R.drawable.ic_call_24)
                            }
                            SimDelegate.SimType.DATA -> {
                                title.text = getString(R.string.no_sim_default_data, sim.name())
                                imageType.setImageResource(R.drawable.ic_data_24)
                            }
                        }

                        sim.icon?.let {
                            imageSim.setImageBitmap(it)
                        }

                        imageAction.setImageResource(R.drawable.ic_arrow_forward_24)

                        description.text = getString(R.string.no_sim_default_summary)

                        buttonCancel.visibility = View.GONE
                        buttonDone.text = getString(R.string.btn_ok)
                        buttonDone.setOnClickListener {
                            dismiss()
                        }
                    }
                }
            } else {
                //Si es de tipo NoSuchElementException significa que no hay ninguna Sim
                // Sino es que no se pudo obtener
                if ((result as Result.Failure).throwable is NoSuchElementException) {
                    dismiss()
                } else {
                    binding.apply {
                        when (simType) {
                            SimDelegate.SimType.VOICE -> {
                                title.text =
                                    getString(R.string.is_default_sim_voice, viewModel.sim?.name())
                                imageType.setImageResource(R.drawable.ic_call_24)
                            }
                            SimDelegate.SimType.DATA -> {
                                title.text =
                                    getString(R.string.is_default_sim_data, viewModel.sim?.name())
                                imageType.setImageResource(R.drawable.ic_data_24)
                            }
                        }

                        viewModel.sim?.icon?.let {
                            imageSim.setImageBitmap(it)
                        }

                        imageAction.setImageResource(R.drawable.ic_question_mark_24)

                        description.text = getString(R.string.is_default_sim_summary)

                        buttonCancel.setOnClickListener {
                            dismiss()
                        }

                        buttonDone.setOnClickListener {
                            viewModel.onDefault?.invoke(viewModel.sim ?: throw IllegalArgumentException())
                        }
                    }
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


    companion object {
        fun newInstance(sim: Sim, simType: SimDelegate.SimType, onDefault: (sim: Sim) -> Unit) =
            SimsDefaultDialogFragment(onDefault, sim).apply {
                arguments?.putString(SIM_TYPE, simType.name)
            }
    }
}