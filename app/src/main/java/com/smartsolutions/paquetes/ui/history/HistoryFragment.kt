package com.smartsolutions.paquetes.ui.history

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.google.android.material.tabs.TabLayoutMediator
import com.smartsolutions.paquetes.databinding.FragmentHistoryBinding
import com.smartsolutions.paquetes.ui.setTabLayoutMediatorSims
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.ui.AbstractFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistoryFragment : AbstractFragment() {

    companion object {
        fun newInstance() = HistoryFragment()
    }

    private val viewModel by viewModels<HistoryViewModel>()

    private var _binding: FragmentHistoryBinding? = null
    private val binding: FragmentHistoryBinding
        get() = _binding!!

    private var adapter: HistoryPagerAdapter? = null
    private var tabsMediator: TabLayoutMediator? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.map { it.sims }.distinctUntilChanged().collect(::renderSims)
            }
        }
    }

    private fun renderSims(sims: List<Sim>) {
        // El estado empieza sin líneas mientras se leen: no hay nada que dibujar todavía.
        if (sims.isEmpty())
            return

        // Las pestañas viejas siguen observando el adaptador: se sueltan antes de crear las nuevas.
        tabsMediator?.detach()

        setAdapter(sims)
        tabsMediator = setTabLayoutMediatorSims(requireContext(), binding.tabs, binding.pager, sims)

        binding.tabs.visibility = if (sims.size <= 1) View.GONE else View.VISIBLE
    }


    private fun setAdapter(sims: List<Sim>){
        if (adapter == null){
            adapter = HistoryPagerAdapter(this, sims)
            binding.pager.adapter = adapter
        }else {
            adapter?.sims = sims
            adapter?.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        tabsMediator?.detach()
        tabsMediator = null
        _binding = null
        super.onDestroyView()
    }
}