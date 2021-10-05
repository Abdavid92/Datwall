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
import com.google.android.material.tabs.TabLayoutMediator
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentPackagesBinding
import com.smartsolutions.paquetes.helpers.setTabLayoutMediatorSims
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.IDataPackage
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.ui.permissions.SinglePermissionFragment
import com.smartsolutions.paquetes.ui.settings.SimsConfigurationFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PackagesFragment : Fragment() {

    companion object {
        fun newInstance() = PackagesFragment()
    }

    private val viewModel by viewModels<PackagesViewModel> ()
    private var adapter: PackagesPagerAdapter? = null

    private lateinit var binding: FragmentPackagesBinding
    

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPackagesBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getInstalledSims().observe(viewLifecycleOwner) {
            setAdapter(it)
            setTabLayoutMediatorSims(requireContext(), binding.tabs, binding.pager, it, childFragmentManager)

            if (it.size <= 1){
                binding.tabs.visibility = View.GONE
            }else {
                binding.tabs.visibility = View.VISIBLE
            }
        }

    }

    private fun setAdapter(sims: List<Sim>){
        if (adapter == null){
            adapter = PackagesPagerAdapter(sims, this)
            binding.pager.adapter = adapter
        } else {
            adapter?.sims = sims
            adapter?.notifyDataSetChanged()
        }
    }


}