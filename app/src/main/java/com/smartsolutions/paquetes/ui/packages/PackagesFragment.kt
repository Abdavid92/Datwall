package com.smartsolutions.paquetes.ui.packages

import android.app.AlertDialog
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentPackagesBinding
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.IDataPackage
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.ui.permissions.SinglePermissionFragment
import com.smartsolutions.paquetes.ui.settings.SimsConfigurationFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PackagesFragment : Fragment(), PackagesViewModel.PurchaseResult {

    companion object {
        fun newInstance() = PackagesFragment()
    }

    private val viewModel by viewModels<PackagesViewModel> ()

    private lateinit var binding: FragmentPackagesBinding
    private var adapter: PackagesRecyclerAdapter? = null

    private var sim: Sim? = null
    

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPackagesBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonConfigureSim.setOnClickListener {
            val frag = SimsConfigurationFragment.newInstance()
            //TODO Lanzar fragmento de configurar las Sims
        }

        viewModel.getSimDefaultVoice().observe(viewLifecycleOwner) {
            sim = it
            if (it == null){
                binding.apply {
                    linSimNotDefault.visibility = View.VISIBLE
                    linSimNotConfigured.visibility = View.GONE
                    recycler.visibility = View.GONE
                }
            }else {
                if (it.packages.isNotEmpty()) {
                    binding.apply {
                        linSimNotDefault.visibility = View.GONE
                        linSimNotConfigured.visibility = View.GONE
                        recycler.visibility = View.VISIBLE
                        setAdapter(viewModel.prepareListPackages(it))
                    }
                }else {
                    binding.apply {
                        linSimNotDefault.visibility = View.GONE
                        linSimNotConfigured.visibility = View.VISIBLE
                        recycler.visibility = View.GONE
                    }
                }
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
        sim?.let {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Comprar ${dataPackage.name}")
                .setMessage(dataPackage.description)
                .setPositiveButton(getString(R.string.purchase)){_, _ ->
                    viewModel.purchasePackage(it, dataPackage, this)
                }
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show()
        }
    }

    override fun onSuccess() {
        Toast.makeText(requireContext(), getString(R.string.purchasing_package), Toast.LENGTH_SHORT).show()
    }

    override fun onFailed() {
        Toast.makeText(requireContext(), getString(R.string.purchasing_package_failed), Toast.LENGTH_SHORT).show()
    }

    override fun onMissingPermission() {
        Toast.makeText(requireContext(), getString(R.string.purchasing_package_permissions), Toast.LENGTH_SHORT).show()
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
           val frag = SinglePermissionFragment.newInstance(IPermissionsManager.CALL_CODE)
           frag.show(childFragmentManager, "PermissionFragment")
        }
    }


}