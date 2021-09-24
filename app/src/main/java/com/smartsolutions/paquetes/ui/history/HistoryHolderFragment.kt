package com.smartsolutions.paquetes.ui.history

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentHistoryHolderBinding
import com.smartsolutions.paquetes.helpers.UIHelper
import com.smartsolutions.paquetes.repositories.models.IPurchasedPackage
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*


private const val SIM_ID = "sim_id"

@AndroidEntryPoint
class HistoryHolderFragment : Fragment() {

    private lateinit var simId: String
    private lateinit var binding: FragmentHistoryHolderBinding

    private val viewModel by viewModels<HistoryViewModel>()

    private var adapter: HistoryRecyclerAdapter? = null
    private lateinit var uiHelper: UIHelper



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            simId = it.getString(SIM_ID) ?: throw IllegalArgumentException()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryHolderBinding.inflate(layoutInflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uiHelper = UIHelper(requireContext())
        configureBarChart()

        viewModel.getPurchasedPackages(simId).observe(viewLifecycleOwner) {
            binding.progressBar.visibility = View.GONE
            setAdapter(it)
            setValuesBarCHart(it.filterIsInstance<HistoryViewModel.HeaderPurchasedPackage>())

            if(it.isEmpty()){
                binding.apply {
                    linNoData.visibility = View.VISIBLE
                    barChart.visibility = View.INVISIBLE
                }
            }else {
                binding.apply {
                    linNoData.visibility = View.GONE
                    barChart.visibility = View.VISIBLE
                }
            }
        }

    }


    private fun setAdapter(list: List<IPurchasedPackage>) {
        if (adapter == null){
            adapter = HistoryRecyclerAdapter(list)
            binding.recycler.adapter = adapter
        }else {
            adapter?.update(list)
        }
    }


    private fun configureBarChart(){
        binding.barChart.apply {
            renderer = RoundedBarChartRender(this, animator, viewPortHandler)
            description.isEnabled = false
            axisRight.isEnabled = false
            setDrawValueAboveBar(false)

            xAxis.apply {
                setDrawGridLines(false)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                isEnabled = true
                textColor = uiHelper.getTextColorByTheme()
            }

            axisLeft.apply {
                setDrawGridLines(true)
                granularity = 1f
                valueFormatter = object: ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return value.toInt().toString()
                    }
                }
                textColor = uiHelper.getTextColorByTheme()
                axisMinimum = 0f
            }
        }
    }

    private fun setValuesBarCHart(list: List<IPurchasedPackage>){

        val entries = mutableListOf<BarEntry>()
        val dates = mutableListOf<Pair<Int, String>>()

        var index = 1
        list.forEach {
            val data = it as HistoryViewModel.HeaderPurchasedPackage
            entries.add(BarEntry(
                index.toFloat(),
                data.cuantity.toFloat()
            ))
            dates.add(Pair(index, SimpleDateFormat("MMM", Locale.getDefault()).format(Date(data.date))))
            index++
        }

        binding.barChart.xAxis.valueFormatter = object: ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return dates.firstOrNull { it.first == value.toInt() }?.second ?: value.toString()
            }
        }

        val dataSet = BarDataSet(entries, getString(R.string.month_purchased)).apply {
            valueFormatter = object: ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return value.toInt().toString()
                    }
                }
            valueTextSize = 16f
            valueTextColor = Color.WHITE
            colors = arrayOf(ContextCompat.getColor(requireContext(), R.color.color_primary)).toMutableList()
        }

        binding.barChart.apply {
            data = BarData(dataSet)
            data.barWidth = 0.65f
            setOnChartValueSelectedListener(object: OnChartValueSelectedListener{

                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    e?.let {
                        val item =list[entries.indexOf(it)] as HistoryViewModel.HeaderPurchasedPackage
                        adapter?.filter(item)
                    }
                }

                override fun onNothingSelected() {
                    adapter?.filter(null)
                }

            })
            animateY(700)
        }.invalidate()

    }



    companion object {

        @JvmStatic
        fun newInstance(sim: Sim) =
            HistoryHolderFragment().apply {
                arguments = Bundle().apply {
                    putString(SIM_ID, sim.id)
                }
            }
    }
}