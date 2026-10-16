package com.smartsolutions.paquetes.ui.resume

import com.smartsolutions.paquetes.R
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.smartsolutions.paquetes.databinding.FragmentResumeHolderBinding
import com.smartsolutions.paquetes.helpers.DataFormatter
import com.smartsolutions.paquetes.managers.UserDataSummary
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

    private val viewModel by viewModels<ResumeViewModel>({ requireParentFragment() })
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

        viewModel.onLineShown(simID)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Sin distinctUntilChanged: los datos se comparan solo por tipo y línea, así que un
                // cambio de megas pasaría por "igual". El estado trae su propia revisión.
                viewModel.state
                    .map { it.lines[simID] }
                    .collect { line -> line?.let(::render) }
            }
        }
    }

    private fun render(line: ResumeLineState) {
        binding.apply {
            if (line.data.isEmpty()) {
                linNoData.visibility = View.VISIBLE
                recycler.visibility = View.GONE
                cardResumeProgress.visibility = View.GONE
            } else {
                linNoData.visibility = View.GONE
                recycler.visibility = View.VISIBLE
                cardResumeProgress.visibility = View.VISIBLE
                setTotals(line.summary)
                setAdapter(line.data)
            }

            line.dailyAverage?.getValue()?.let {
                averageDay.text = "${it.value}"
                averageDayUnit.text = "${it.dataUnit}"
            }
            line.dailyRemainder?.getValue()?.let {
                averageRest.text = "${it.value}"
                averageRestUnit.text = "${it.dataUnit}"
            }
        }
    }

    private fun setTotals(summary: UserDataSummary) {
        val rest = DataFormatter.bytes(summary.restBytes)
        val used = DataFormatter.bytes(summary.usedBytes)
        binding.apply {
            progressBar.isIndeterminate = false
            progressBar.max = 100
            progressBar.progress = summary.percent
            textPercentTotal.text = getString(R.string.percent_value, summary.percent)
            textRestTotal.text = getString(R.string.resume_rest_total, rest.value.toString(), rest.dataUnit.name)
            textUsageTotal.text = getString(R.string.resume_used_total, used.value.toString(), used.dataUnit.name)
        }
    }

    fun showEditFragment(dataType: DataBytes.DataType?) {
        EditAddUserDataBytesFragment.newInstance(
            simID,
            dataType != null,
            dataType
        ).show(childFragmentManager, null)
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