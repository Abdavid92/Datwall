package com.smartsolutions.paquetes.ui.settings.sim

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentDefaultSimsDialogBinding
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.AndroidEntryPoint

private const val FAILED_DEFAULT = "failed_default"

@AndroidEntryPoint
class DefaultSimsDialogFragment : BottomSheetDialogFragment() {

    private var failed: FailDefault? = null

    private var _binding: FragmentDefaultSimsDialogBinding? = null
    private val binding: FragmentDefaultSimsDialogBinding
        get() = _binding!!

    private val viewModel by viewModels<DefaultSimsViewModel> ()
    private var adapterVoice: DefaultSimRecyclerAdapter? = null
    private var adapterData: DefaultSimRecyclerAdapter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            bundle.getString(FAILED_DEFAULT)?.let {
                failed = FailDefault.valueOf(it)
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDefaultSimsDialogBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        failed?.let {
            binding.apply {
                linFailedDefault.visibility = View.VISIBLE
                textDescription.text = if (it == FailDefault.DEFAULT_VOICE){
                    getString(R.string.no_sim_default_voice)
                }else {
                    getString(R.string.no_sim_default_data)
                }
            }
        }

        binding.buttonSettings.visibility = View.VISIBLE

        binding.buttonSettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.find_settings_dual_sim),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.cant_not_open),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        viewModel.getInstalledSims().observe(viewLifecycleOwner){
            setAdapter(it.first, it.second)
        }
    }



    private fun setAdapter(sims: List<Sim>, isBrokenDualSim: Boolean){
        if (adapterVoice == null || adapterData == null){
            adapterData = DefaultSimRecyclerAdapter(this, sims, isBrokenDualSim, false)
            binding.recyclerData.adapter = adapterData

            adapterVoice = DefaultSimRecyclerAdapter(this, sims, isBrokenDualSim, true)
            binding.recyclerVoice.adapter = adapterVoice
        }else {
            adapterData?.sims = sims
            adapterData?.notifyDataSetChanged()
            adapterVoice?.sims = sims
            adapterVoice?.notifyDataSetChanged()
        }
    }


    fun setDefaultSim(sim: Sim, isDefaultVoice: Boolean){
        val simType = if (isDefaultVoice){
            SimDelegate.SimType.VOICE
        }else {
            SimDelegate.SimType.DATA
        }
        viewModel.setDefaultSim(sim, simType)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {

        fun newInstance(failDefault: FailDefault? = null): DefaultSimsDialogFragment =
            DefaultSimsDialogFragment().apply {
                arguments = Bundle().apply {
                    failDefault?.let {
                        putString(FAILED_DEFAULT, it.name)
                    }
                }
            }

    }

    enum class FailDefault {
        DEFAULT_DATA,
        DEFAULT_VOICE
    }
}