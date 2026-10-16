package com.smartsolutions.paquetes.ui.usage

import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.helpers.DataFormatter
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.smartsolutions.paquetes.databinding.FragmentUsagePlaceHolderBinding
import com.smartsolutions.paquetes.helpers.UIHelper
import com.smartsolutions.paquetes.repositories.models.TrafficType
import dagger.hilt.android.AndroidEntryPoint

private const val FRAGMENT_TYPE = "fragment_type"
@AndroidEntryPoint
class UsageHolderFragment : Fragment() {

    private lateinit var type: TrafficType

    private var _binding: FragmentUsagePlaceHolderBinding? = null
    private val binding: FragmentUsagePlaceHolderBinding
        get() = _binding!!

    private val uiHelper by lazy {
        UIHelper(requireContext())
    }

    private var adapter: UsageRecyclerAdapter? = null

    private val viewModel by viewModels<UsageViewModel>({ requireParentFragment() })

    private var breakdown: UsageBreakdown? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = TrafficType.entries[arguments?.getInt(FRAGMENT_TYPE) ?: throw IllegalArgumentException("The type is NULL")]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsagePlaceHolderBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefresh.apply {
            val uiHelper = UIHelper(requireActivity())

            uiHelper.applySwipeRefreshTheme(this)

            isRefreshing = true
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.onRefresh()
        }

        configurePieChart()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state
                    .map { it.loading to it.breakdowns[type] }
                    .distinctUntilChanged()
                    .collect { (loading, breakdown) -> render(loading, breakdown) }
            }
        }
    }

    private fun render(loading: Boolean, breakdown: UsageBreakdown?) {
        binding.swipeRefresh.isRefreshing = loading

        if (loading || breakdown == null)
            return

        this.breakdown = breakdown

        val total = DataFormatter.bytes(breakdown.total)

        setAdapter(breakdown.apps)

        binding.pieChart.centerText = "${total.value} ${total.dataUnit}"

        fillPieChart(breakdown.slices)
    }

    private fun fillPieChart(slices: List<UsageSlice>) {
        val entries = slices.map { PieEntry(it.bytes.toFloat(), it.label ?: getString(R.string.usage_others)) }

        val pieData = PieData(PieDataSet(entries, "").apply {
            valueTextSize = 11f
            colors = slices.map { it.colour }
        })

        pieData.setDrawValues(false)

        binding.pieChart.apply {
            data = pieData
            animateY(1000, Easing.EaseInOutCubic)
            visibility = View.VISIBLE
        }.postInvalidate()
    }

    private fun setAdapter(apps: List<UsageApp>){
        if (adapter == null){
            adapter = UsageRecyclerAdapter(this, apps, viewModel.iconManager)
            binding.recyclerView.adapter = adapter
        }else {
            adapter?.updateApps(apps)
        }
    }



    private fun configurePieChart() {
        binding.pieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(android.R.color.transparent)
            legend.isEnabled = false
            legend.textColor = uiHelper.getTextColorByTheme()
            setEntryLabelTextSize(11f)
            setDrawEntryLabels(false)
            setEntryLabelColor(Color.BLACK)
            setCenterTextColor(uiHelper.getTextColorByTheme())
            setCenterTextSize(23f)
        }

        binding.pieChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {

            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val label = (e as PieEntry).label
                val breakdown = breakdown ?: return
                if (label == getString(R.string.usage_others)) {
                    adapter?.filter(breakdown.others)
                } else {
                    breakdown.main.firstOrNull { it.app.name == label }?.let {
                        adapter?.filter(listOf(it))
                    }
                }
            }

            override fun onNothingSelected() {
                adapter?.filter(emptyList())
            }
        })
    }

    override fun onDestroyView() {
        _binding = null
        adapter = null
        breakdown = null
        super.onDestroyView()
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

