package com.smartsolutions.paquetes.ui.packages

import com.smartsolutions.paquetes.data.PackagesRules
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentPackagesHolderBinding
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.IDataPackage
import com.smartsolutions.paquetes.ui.FragmentContainerActivity
import com.smartsolutions.paquetes.ui.permissions.showPermissionSheet
import com.smartsolutions.paquetes.ui.settings.PackagesConfigurationFragment
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class PackagesHolderFragment : Fragment() {

    private var _binding: FragmentPackagesHolderBinding? = null
    private val binding: FragmentPackagesHolderBinding
        get() = _binding!!


    private val viewModel by viewModels<PackagesViewModel>()

    private var adapter: PackagesRecyclerAdapter? = null

    private val purchaseSmsConsent = PurchaseSmsConsent(this) { smsBody ->
        viewModel.onSmsShared(smsBody)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPackagesHolderBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonConfigureSim.setOnClickListener {
            startActivity(
                Intent(
                    requireContext(),
                    FragmentContainerActivity::class.java
                ).apply {
                    putExtra(
                        FragmentContainerActivity.EXTRA_FRAGMENT,
                        PackagesConfigurationFragment::class.java.name
                    )
                    putExtra(
                        PackagesConfigurationFragment.EXTRA_DEFAULT_SIM_ID,
                        arguments?.getString(PackagesViewModel.ARG_SIM_ID)
                    )
                }
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.events.collect(::onEvent) }
            }
        }
    }

    private fun render(state: PackagesUiState) {
        if (state.loading)
            return

        binding.apply {
            if (state.availability == PackagesRules.Availability.AVAILABLE) {
                linSimNotConfigured.visibility = View.GONE
                recycler.visibility = View.VISIBLE
                setAdapter(
                    listOf<IDataPackage>(PackagesViewModel.HeaderPackagesItem(name = getString(R.string.plan_title))) +
                        state.plans
                )
            } else {
                // Una línea configurada sin planes es una línea empresarial.
                val configured = state.availability == PackagesRules.Availability.NO_PLANS
                textSimNotConfigured.setText(
                    if (configured) R.string.no_plans_available else R.string.no_sim_configured
                )
                buttonConfigureSim.visibility = if (configured) View.GONE else View.VISIBLE
                linSimNotConfigured.visibility = View.VISIBLE
                recycler.visibility = View.GONE
            }
        }
    }

    private fun onEvent(event: PackagesEvent) {
        when (event) {
            PackagesEvent.PurchaseStarted -> {
                Toast.makeText(requireContext(), getString(R.string.purchasing_package), Toast.LENGTH_SHORT).show()
                purchaseSmsConsent.start()
            }
            PackagesEvent.PurchaseFailed ->
                Toast.makeText(requireContext(), getString(R.string.purchasing_package_failed), Toast.LENGTH_SHORT).show()
            is PackagesEvent.RequestPermission -> {
                Toast.makeText(requireContext(), getString(R.string.purchasing_package_permissions), Toast.LENGTH_SHORT).show()
                showPermissionSheet(event.requestCode)
            }
        }
    }

    private fun setAdapter(list: List<IDataPackage>){
        if (adapter == null){
            adapter = PackagesRecyclerAdapter(this, list)
            binding.recycler.adapter = adapter
        }else {
            adapter?.dataPackages = list
            adapter?.notifyDataSetChanged()
        }
    }

    fun purchasePackage(iDataPackage: IDataPackage){
        val dataPackage = iDataPackage as DataPackage
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.buy_plan_title, dataPackage.name))
            .setMessage(dataPackage.description)
            .setPositiveButton(getString(R.string.purchase)) { _, _ ->
                viewModel.onPurchase(dataPackage.id)
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {

        @JvmStatic
        fun newInstance(simID: String) =
            PackagesHolderFragment().apply {
                arguments = Bundle().apply {
                    putString(PackagesViewModel.ARG_SIM_ID, simID)
                }
            }
    }
}