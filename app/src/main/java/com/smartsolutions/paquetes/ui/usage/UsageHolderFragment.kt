package com.smartsolutions.paquetes.ui.usage

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentUsagePlaceHolderBinding
import com.smartsolutions.paquetes.helpers.UIHelper

private const val FRAGMENT_TYPE = "fragment_type"
class UsageHolderFragment : Fragment() {

    private var type: String? = null
    private lateinit var binding: FragmentUsagePlaceHolderBinding
    private lateinit var uiHelper: UIHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            type = it.getString(FRAGMENT_TYPE)
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

        showPieChart(listOf(
            Pair("Caca", 100L),
            Pair("Pipi", 350L),
            Pair("Viento", 127L),
            Pair("Diarrea", 563L),
            Pair("Colico", 675L)
        ))

    }


    private fun showPieChart(data: List<Pair<String, Long>>) {
        val entries = mutableListOf<PieEntry>()

        data.forEach {
            entries.add(PieEntry(it.second.toFloat(), it.first))
        }

        val pieData = PieData(PieDataSet(entries, "Consumo").apply {
            valueTextSize = 11f
            colors = mutableListOf(
                Color.RED,
                Color.YELLOW,
                Color.BLUE,
                Color.GREEN,
                Color.CYAN
            )
        })

        binding.pieChart.data = pieData
        binding.pieChart.description.text = "en MB"
        binding.pieChart.description.textColor = uiHelper.getTextColorByTheme()
        binding.pieChart.isDrawHoleEnabled = false
        binding.pieChart.legend.isEnabled = false
        binding.pieChart.setEntryLabelTextSize(11f)
        binding.pieChart.setEntryLabelColor(Color.BLACK)
        binding.pieChart.animateY(1400, Easing.EaseInOutCubic);
        binding.pieChart.invalidate()
    }

    companion object {
        @JvmStatic
        fun newInstance(type: String) =
            UsageHolderFragment().apply {
                arguments = Bundle().apply {
                    putString(FRAGMENT_TYPE, type)
                }
            }
    }
}