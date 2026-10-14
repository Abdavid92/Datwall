package com.smartsolutions.paquetes.ui.usage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayoutMediator
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentUsageBinding
import com.smartsolutions.paquetes.ui.AbstractFragment
import dagger.hilt.android.AndroidEntryPoint
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.ui.permissions.bindPermissionNeeded
import javax.inject.Inject

@AndroidEntryPoint
class UsageFragment : AbstractFragment() {

    companion object {
        fun newInstance() = UsageFragment()
    }

    private val viewModel by viewModels<UsageViewModel>()

    @Inject
    lateinit var permissionsManager: IPermissionsManager

    /**
     * Para recargar el consumo solo cuando el acceso de uso se acaba de conceder.
     * */
    private var usageAccessGranted = true

    private var _binding: FragmentUsageBinding? = null
    private val binding: FragmentUsageBinding
        get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsageBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.spinnerUsageOptions.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.setUsagePeriod(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

        }

        binding.btnFilterUsage.setOnClickListener {
            showFilterOptions()
        }

        val adapter = FragmentPageAdapter(this)
        binding.pager.adapter = adapter

        TabLayoutMediator(binding.tabs, binding.pager){ tab, position ->
            tab.text = adapter.fragmentsList[position].second
        }.attach()

    }

    override fun onResume() {
        super.onResume()

        // El consumo por app sale de las estadísticas del sistema: sin el acceso de uso se
        // explica por qué hace falta y se ofrece concederlo.
        val granted = bindPermissionNeeded(
            binding.usagePermission,
            binding.pager,
            permissionsManager,
            IPermissionsManager.USAGE_ACCESS_CODE
        )

        if (granted && !usageAccessGranted)
            viewModel.setUsagePeriod(binding.spinnerUsageOptions.selectedItemPosition)

        usageAccessGranted = granted
    }

    private fun showFilterOptions(){
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_filter_title)
            .setItems(R.array.filter_usage) { _, pos ->
                viewModel.setUsageFilter(UsageViewModel.UsageFilters.values()[pos])
            }.show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}