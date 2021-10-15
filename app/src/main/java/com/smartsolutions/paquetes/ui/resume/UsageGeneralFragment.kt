package com.smartsolutions.paquetes.ui.resume

import android.graphics.Color
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentUsageGeneralBinding
import com.smartsolutions.paquetes.helpers.DateCalendarUtils
import com.smartsolutions.paquetes.helpers.UIHelper
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.UsageGeneral
import dagger.hilt.android.AndroidEntryPoint
import java.lang.IllegalArgumentException
import java.text.SimpleDateFormat
import java.util.*


private const val DATA_TYPE = "data_type"
private const val SIM_ID = "sim_id"
@AndroidEntryPoint
class UsageGeneralFragment : BottomSheetDialogFragment() {

    private lateinit var uiHelper: UIHelper

    private var _binding: FragmentUsageGeneralBinding? = null

    private val binding get() = _binding!!

    private lateinit var dataType: String
    private lateinit var simId: String

    private val viewModel by viewModels<UsageGeneralViewModel> ()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            dataType = it.getString(DATA_TYPE) ?: DataBytes.DataType.International.name
            simId = it.getString(SIM_ID) ?: throw IllegalArgumentException()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsageGeneralBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        uiHelper = UIHelper(requireContext())

        binding.chipGroup.setOnCheckedChangeListener { _, checkedId ->
            dataType = when (checkedId){
                R.id.chip_international -> DataBytes.DataType.International.name
                R.id.chip_international_lte -> DataBytes.DataType.InternationalLte.name
                R.id.chip_national -> DataBytes.DataType.National.name
                R.id.chip_bag_daily -> DataBytes.DataType.DailyBag.name
                R.id.chip_promo_bonus -> DataBytes.DataType.PromoBonus.name
                else -> DataBytes.DataType.International.name
            }

            viewModel.setDataType(simId, DataBytes.DataType.valueOf(dataType))
        }

        binding.spinnerUsageOptions.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.setPeriod(position, simId, DataBytes.DataType.valueOf(dataType))
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        configureLineChart()

        viewModel.getUsageGeneral().observe(viewLifecycleOwner){
            if (it.first.isNotEmpty()) {
                setLineChart(it.first, it.second)
                binding.apply {
                    progressBar.visibility = View.GONE
                    textNoData.visibility = View.GONE
                    lineChart.visibility = View.VISIBLE
                }
            }else {
                binding.apply {
                    progressBar.visibility = View.GONE
                    textNoData.visibility = View.VISIBLE
                    lineChart.visibility = View.INVISIBLE
                }
            }
        }

    }

    private fun configureLineChart() {
        binding.lineChart.apply{
            description.isEnabled = false
            axisRight.isEnabled = false
            legend.textColor = uiHelper.getTextColorByTheme()

            xAxis.apply {
                setDrawGridLines(false)
                position = XAxis.XAxisPosition.BOTTOM
                textColor = uiHelper.getTextColorByTheme()
            }
            axisLeft.apply {
                setDrawGridLines(true)
                setDrawZeroLine(false)
                setDrawGridLinesBehindData(false)
                textColor = uiHelper.getTextColorByTheme()
                axisMinimum = 0f
            }
            setPinchZoom(true)
            setDrawMarkers(true)
        }
    }


    private fun setLineChart(usages: List<UsageGeneral>, myTimeUnit: DateCalendarUtils.MyTimeUnit) {
        val entries = mutableListOf<Entry>()

        usages.forEach {
            entries.add(
                Entry(
                    it.date.toFloat(),
                    it.bytes.toFloat()
                )
            )
        }

        binding.lineChart.apply {
            data = LineData(LineDataSet(entries, "Consumo").apply {
                setDrawFilled(false)
                val colours = arrayOf(uiHelper.getColorTheme(R.attr.colorAccent) ?: Color.BLUE).toMutableList()
                colors = colours
                valueTextSize = 0f
                circleColors = colours
                isHighlightPerTapEnabled = true
                circleRadius = 3f
                setDrawCircleHole(true)
                lineWidth = 3f
            })
            animateY(700)
            axisLeft.valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    val bytes = DataUnitBytes(value.toLong()).getValue()
                    return "${Math.round(bytes.value * 100) / 100.0} ${bytes.dataUnit}"
                }
            }

            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    return when (myTimeUnit) {
                        DateCalendarUtils.MyTimeUnit.HOUR, DateCalendarUtils.MyTimeUnit.MINUTE -> {
                            SimpleDateFormat("hh aa", Locale.US).format(Date(value.toLong()))
                        }
                        DateCalendarUtils.MyTimeUnit.DAY -> {
                            "Día " + SimpleDateFormat("dd", Locale.getDefault()).format(Date(value.toLong()))
                        }
                        DateCalendarUtils.MyTimeUnit.MONTH -> {
                            SimpleDateFormat("MMM", Locale.getDefault()).format(Date(value.toLong()))
                        }
                    }
                }
            }

            marker = object : MarkerView(requireContext(), R.layout.higlith_chart) {

                private val text: TextView
                init {
                    text = findViewById(R.id.text_value)
                }

                override fun refreshContent(e: Entry?, highlight: Highlight?) {
                    val index = entries.indexOf(e)
                    if (index in 0..usages.size) {
                        val bytes = DataUnitBytes(usages[index].bytes).getValue()
                        text.text = "${bytes.value} ${bytes.dataUnit.name}"
                    }
                    super.refreshContent(e, highlight)
                }

                private var mOffset: MPPointF? = null

                override fun getOffset(): MPPointF? {
                    if (mOffset == null) {
                        // center the marker horizontally and vertically
                        mOffset = MPPointF(
                            (-(getWidth() / 2)).toFloat(),
                            (-getHeight()).toFloat()
                        )
                    }
                    return mOffset
                }

            }

        }.invalidate()
    }


    companion object {

        fun newInstance(simId: String, dataType: String? = null): UsageGeneralFragment =
            UsageGeneralFragment().apply {
                arguments = Bundle().apply {
                    putString(DATA_TYPE, dataType)
                    putString(SIM_ID, simId)
                }
            }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}