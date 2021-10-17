package com.smartsolutions.paquetes.ui.packages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.databinding.FragmentPackagesBinding
import com.smartsolutions.paquetes.helpers.setTabLayoutMediatorSims
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.ui.AbstractFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PackagesFragment : AbstractFragment() {

    companion object {
        fun newInstance() = PackagesFragment()
    }

    private val viewModel by viewModels<PackagesViewModel> ()
    private var adapter: PackagesPagerAdapter? = null

    private var _binding: FragmentPackagesBinding? = null
    private val binding: FragmentPackagesBinding
        get() = _binding!!
    

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPackagesBinding.inflate(layoutInflater, container, false)
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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}