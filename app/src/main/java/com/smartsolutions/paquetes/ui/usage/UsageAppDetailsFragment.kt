package com.smartsolutions.paquetes.ui.usage

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.smartsolutions.paquetes.databinding.FragmentUsageAppDetailsBinding
import com.smartsolutions.paquetes.helpers.DateCalendarUtils
import com.smartsolutions.paquetes.helpers.UIHelper
import com.smartsolutions.paquetes.managers.contracts.IIconManager2
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.models.App
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


const val ARG_APP = "app"

@AndroidEntryPoint
class UsageAppDetailsFragment : BottomSheetDialogFragment() {

    @Inject
    lateinit var iconManager: IIconManager2

    private lateinit var uiHelper: UIHelper

    private var _binding: FragmentUsageAppDetailsBinding? = null
    private val binding: FragmentUsageAppDetailsBinding
        get() = _binding!!

    private val viewModel by viewModels<UsageAppDetailsViewModel>()

    private var adapter: UsageAppDetailsRecyclerAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsageAppDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val app = arguments?.getParcelable<App>(ARG_APP)

        if (app == null) {
            this.dismiss()
            return
        }

        uiHelper = UIHelper(requireContext())

        binding.include.circleColour.visibility = View.GONE
        iconManager.getIcon(app.packageName, app.version) {
            binding.include.appIcon.setImageBitmap(it)
        }

        binding.include.textAppName.text = app.name
        val value = app.traffic!!.totalBytes.getValue()
        binding.include.textUsageValue.text = "${value.value} ${value.dataUnit}"
        configureLineChart()

        viewModel.getUsageByTime(app.uid).observe(viewLifecycleOwner, { result ->
            setLineChart(result.first, result.second)
            setAdapter(result)
            binding.progressBar.visibility = View.GONE
            binding.lineChart.visibility = View.VISIBLE
        })
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

    private fun setLineChart(traffics: List<Traffic>, myTimeUnit: DateCalendarUtils.MyTimeUnit) {
        val entries = mutableListOf<Entry>()

        traffics.forEach {
            entries.add(
                Entry(
                    it.startTime.toFloat(),
                    it.totalBytes.bytes.toFloat()
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

                @SuppressLint("StaticFieldLeak")
                private var text: TextView = findViewById(R.id.text_value)

                override fun refreshContent(e: Entry?, highlight: Highlight?) {
                    val index = entries.indexOf(e)
                    if (index in 0..traffics.size) {
                        val bytes = traffics[index].totalBytes.getValue()
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


    private fun setAdapter(traffics: Pair<List<Traffic>, DateCalendarUtils.MyTimeUnit>){
        if (adapter == null){
            adapter = UsageAppDetailsRecyclerAdapter(traffics)
            binding.recyclerUsage.adapter = adapter
        }else {
            adapter!!.traffics = traffics
            adapter?.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance(app: App): UsageAppDetailsFragment =
            UsageAppDetailsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_APP, app)
                }
            }

    }
}