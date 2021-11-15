package com.smartsolutions.paquetes.ui.resume

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.databinding.FragmentResumeHolderBinding
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import dagger.hilt.android.AndroidEntryPoint


private const val SIM_ID = "sim_id"

@AndroidEntryPoint
class ResumeHolderFragment : Fragment() {

    private lateinit var simID: String

    private var _binding: FragmentResumeHolderBinding? = null
    private val binding: FragmentResumeHolderBinding
        get() = _binding!!

    private val viewModel by viewModels<ResumeViewModel>()
    private var adapter: UserDataBytesRecyclerAdapter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            simID = it.getString(SIM_ID, null) ?: throw NullPointerException()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResumeHolderBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getUserDataBytes(simID).observe(viewLifecycleOwner) {
            if (it.first.isEmpty()) {
                binding.linNoData.visibility = View.VISIBLE
                binding.recycler.visibility = View.GONE
                binding.cardResumeProgress.visibility = View.GONE
            } else {
                binding.linNoData.visibility = View.GONE
                binding.recycler.visibility = View.VISIBLE
                binding.cardResumeProgress.visibility = View.VISIBLE
                setTotals(it.second)
                setAdapter(it.first)
            }
        }

        viewModel.getAverages().observe(viewLifecycleOwner){
            binding.apply {
                val usage = it.first.getValue()
                val rest = it.second.getValue()
                averageDay.text = "${usage.value} ${usage.dataUnit}"
                averageRest.text = "${rest.value} ${rest.dataUnit}"
            }
        }

    }

    private fun setTotals(data: Triple<Int, DataUnitBytes.DataValue, DataUnitBytes.DataValue>){
        binding.apply {
            progressBar.isIndeterminate = false
            progressBar.max = 100
            progressBar.progress = data.first
            textPercentTotal.text = "${data.first}%"
            textRestTotal.text = "Rest: ${data.second.value} ${data.second.dataUnit}"
            textUsageTotal.text = "Gast: ${data.third.value} ${data.third.dataUnit}"
        }
    }

    fun showChartUsageGeneral(dataType: DataBytes.DataType) {
        UsageGeneralFragment.newInstance(
            simID,
            dataType.name
        ).show(childFragmentManager, null)
    }

    override fun onPause() {
        super.onPause()
        adapter = null
    }

    private fun setAdapter(userData: List<UserDataBytes>) {
        if (adapter == null) {
            adapter = UserDataBytesRecyclerAdapter(this, userData)
            binding.recycler.adapter = adapter
        } else {
            adapter?.update(userData)
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    companion object {

        @JvmStatic
        fun newInstance(simID: String) =
            ResumeHolderFragment().apply {
                arguments = Bundle().apply {
                    putString(SIM_ID, simID)
                }
            }
    }
}