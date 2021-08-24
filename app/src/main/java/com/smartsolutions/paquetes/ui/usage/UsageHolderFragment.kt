package com.smartsolutions.paquetes.ui.usage

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.smartsolutions.paquetes.databinding.FragmentUsagePlaceHolderBinding
import com.smartsolutions.paquetes.helpers.UIHelper
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.TrafficType
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import kotlin.math.roundToInt

private const val FRAGMENT_TYPE = "fragment_type"
@AndroidEntryPoint
class UsageHolderFragment : Fragment() {

    private var type: Int? = null
    private lateinit var binding: FragmentUsagePlaceHolderBinding

    //TODO: Puedes usar Lazy aquí
    /*private val uiHelper by lazy {
        UIHelper(requireContext())
    }
    Cuando intentes usar la variable por primera vez,
    esta se inicializa automáticamente.
    */
    private lateinit var uiHelper: UIHelper
    private var adapter: UsageRecyclerAdapter? = null

    //TODO: Puedes compartir el mismo viewModel para todos los fragmentos
    /*Ej: private val viewModel by viewModels<UsageViewModel>({
        /*Esto le indica a la función viewModels que use el
        * ViewModelStoreOwner del fragmento padre y se recicla el mismo
        * viewModel para todos sus fragmentos hijos. Esto te permite propagar
        * eventos a traves del viewModel por todos los fragmentos hijos.
        */
        requireParentFragment()
    })*/
    private val viewModel by viewModels<UsageViewModel>()

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

        binding.swipeRefresh.isRefreshing = true

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshData()
        }

        configurePieChart()

        type?.let {
            viewModel.getUsage(TrafficType.values()[it]).observe(viewLifecycleOwner, { result ->

                val total = DataUnitBytes(result.first).getValue()

                setAdapter(result.second)

                binding.pieChart.centerText = "${Math.round(total.value * 100) / 100.0} ${total.dataUnit}"

                viewModel.processAndFillPieCharData(result.first, result.second, binding.pieChart)

                binding.swipeRefresh.isRefreshing = false
            })
        }

    }



    private fun setAdapter(apps: List<UsageApp>){
        if (adapter == null){
            adapter = UsageRecyclerAdapter(this, apps, viewModel.iconManager)
            binding.recyclerView.adapter = adapter
        }else {
            adapter!!.updateApps(apps)
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
                if (label == OTHERS_LABEL) {
                    adapter?.filter(viewModel.others)
                } else {
                    viewModel.apps.firstOrNull { it.app.name == label }?.let {
                        adapter?.filter(listOf(it))
                    }
                }
            }

            override fun onNothingSelected() {
                adapter?.filter(emptyList())
            }
        })
    }



    companion object {
        const val OTHERS_LABEL = "Otras"
        @JvmStatic
        fun newInstance(type: TrafficType) =
            UsageHolderFragment().apply {
                arguments = Bundle().apply {
                    putInt(FRAGMENT_TYPE, type.ordinal)
                }
            }
    }
}

