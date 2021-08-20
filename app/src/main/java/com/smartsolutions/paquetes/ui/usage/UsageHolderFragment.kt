package com.smartsolutions.paquetes.ui.usage

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentUsagePlaceHolderBinding
import com.smartsolutions.paquetes.helpers.UIHelper
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.models.TrafficType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

private const val FRAGMENT_TYPE = "fragment_type"
@AndroidEntryPoint
class UsageHolderFragment : Fragment() {

    private var type: Int? = null
    private lateinit var binding: FragmentUsagePlaceHolderBinding
    private lateinit var uiHelper: UIHelper

    private val viewModel by viewModels<UsageViewModel> ()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            type = it.getInt(FRAGMENT_TYPE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUsagePlaceHolderBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uiHelper = UIHelper(requireContext())

        type?.let {
            viewModel.getUsage(TrafficType.values()[it]).observe(viewLifecycleOwner, {
                val total =  DataUnitBytes(it.first).getValue()
                binding.textTotalValue.text = "${total.value.roundToInt()} ${total.dataUnit}"

                runBlocking {
                    showPieChart(viewModel.processTrafficChart(it.second))
                }
            })
        }

    }


    private fun showPieChart(data: List<Pair<String, Long>>) {
        val entries = mutableListOf<PieEntry>()

        data.forEach {
            entries.add(PieEntry(it.second.toFloat(), it.first))
        }

        val pieData = PieData(PieDataSet(entries, "").apply {
            valueTextSize = 11f
            colors = resources.getIntArray(R.array.colors_chart).toMutableList()
        })

        //pieData.setValueFormatter(PercentFormatter())

        pieData.setDrawValues(false)

        binding.pieChart.data = pieData
        binding.pieChart.description.text = "en MB"
        binding.pieChart.description.textColor = uiHelper.getTextColorByTheme()
        binding.pieChart.isDrawHoleEnabled = false
        binding.pieChart.legend.textColor = uiHelper.getTextColorByTheme()
        binding.pieChart.setDrawRoundedSlices(true)
        binding.pieChart.setUsePercentValues(false)
        binding.pieChart.setEntryLabelTextSize(11f)
        binding.pieChart.setDrawEntryLabels(false)
        binding.pieChart.setEntryLabelColor(Color.BLACK)
        binding.pieChart.animateY(1000, Easing.EaseInOutCubic)
        binding.pieChart.postInvalidate()
    }

    companion object {
        @JvmStatic
        fun newInstance(type: TrafficType) =
            UsageHolderFragment().apply {
                arguments = Bundle().apply {
                    putInt(FRAGMENT_TYPE, type.ordinal)
                }
            }
    }
}